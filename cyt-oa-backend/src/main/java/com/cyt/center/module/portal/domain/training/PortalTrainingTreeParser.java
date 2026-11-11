package com.cyt.center.module.portal.domain.training;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 培训 NAS 目录树解析（迁自 Portal 的 domain/training.parse_tree 及文件名/图片尺寸规则）。
 *
 * 所在层：domain/ = 纯业务规则，零 IO 零 SQL。纯图片目录本层判不了 cards/article（要读文件头尺寸），
 * 先按保守的 article 落下并列进 {@link ParsedTree#imageDirs}，由 application 读头并发探测后回填 kind。
 *
 * 逐字节对齐 Portal：parse_image_size 的 PNG/JPEG/GIF/WEBP 帧头解析按原样移植，不做任何「聪明」的变通。
 */
public final class PortalTrainingTreeParser {

    private static final String ARTICLE_ENTRY = "output.html";
    private static final List<String> NOISE_BASENAMES = List.of("thumbs.db", "desktop.ini");
    private static final List<String> IMAGE_EXTS = List.of("png", "jpg", "jpeg", "gif");

    /** 图卡判定宽高比容差（1% 内视为同规格） */
    private static final double ASPECT_RATIO_TOLERANCE = 0.01;
    /** 图卡至少两张 */
    private static final int CARDS_MIN_IMAGES = 2;
    /** 判型抽样上限 */
    private static final int IMAGE_DIR_SAMPLE_SIZE = 4;

    private static final Pattern SORT_PREFIX = Pattern.compile("^\\s*(\\d+)\\s*[_.、．\\-]\\s*(.*)$");
    private static final Pattern NATURAL_CHUNK = Pattern.compile("(\\d+)");

    // ══════════════ 解析结果 ══════════════

    /** 整桶列举出来的一条对象：键 + 字节数 */
    public record TreeEntry(String key, long size) {
    }

    /** 从 bucket 解析出的一个课时（尚未落库）。source_prefix 是幂等键。 */
    @Data
    public static class ParsedLesson {
        private int sortOrder;
        private String title;
        private String kind;
        private String sourcePrefix;
        private String objectKey;
        private String pdfKey;
        private List<String> pages;
        private Long sizeBytes;
    }

    /** 从 bucket 解析出的一门课程：主题目录 = category，课程目录名 = title。 */
    @Data
    public static class ParsedCourse {
        private String category;
        private String title;
        private String sourcePrefix;
        private int sortOrder;
        private List<ParsedLesson> lessons = new ArrayList<>();
    }

    /** 整桶解析结果：courses 按 source_prefix 排序；imageDirs 是 courses 里纯图片目录课时的同一批对象引用。 */
    @Data
    public static class ParsedTree {
        private List<ParsedCourse> courses = new ArrayList<>();
        private List<String> skipped = new ArrayList<>();
        private List<ParsedLesson> imageDirs = new ArrayList<>();

        /** 出现过的顶层主题目录名（去重、按名排序）。 */
        public List<String> categories() {
            TreeSet<String> set = new TreeSet<>();
            for (ParsedCourse c : courses) {
                set.add(c.category);
            }
            return new ArrayList<>(set);
        }
    }

    private record SortResult(int order, String title) {
    }

    private record DirFile(String key, String rel, long size) {
    }

    /** 课时目录 / 直接文件判型结果；pending = 纯图片目录待读头回填 cards/article */
    private record ClassifyResult(String kind, String objectKey, String pdfKey, List<String> pages,
                                 Long sizeBytes, boolean pending) {
    }

    private record DirGroupKey(String coursePrefix, String lessonDir) {
    }

    private record DirectGroupKey(String category, String secondDir) {
    }

    private PortalTrainingTreeParser() {
    }

    // ══════════════ 文件名规则 ══════════════

    /** 把「7_公司介绍」拆成 (7, "公司介绍")；没有数字前缀返回 (0, 原名)。 */
    public static SortResult splitSortPrefix(String name) {
        String n = name == null ? "" : name;
        Matcher m = SORT_PREFIX.matcher(n);
        if (!m.matches() || m.group(2).trim().isEmpty()) {
            return new SortResult(0, n.trim());
        }
        return new SortResult(Integer.parseInt(m.group(1)), m.group(2).trim());
    }

    /** 去掉文件扩展名（只剥最后一个点之后）；无扩展名原样返回。 */
    public static String stripExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx > 0 ? filename.substring(0, idx) : filename;
    }

    /** 取小写扩展名（不含点）；没有返回空串。 */
    public static String extensionOf(String filename) {
        if (filename == null || filename.indexOf('.') < 0) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /** 自然排序键：01.jpg &lt; 2.jpg &lt; 10.jpg。元素交替为字符串（可为空）与整数。 */
    public static List<Object> naturalKey(String text) {
        List<Object> out = new ArrayList<>();
        Matcher m = NATURAL_CHUNK.matcher(text);
        int last = 0;
        while (m.find()) {
            out.add(text.substring(last, m.start()).toLowerCase());
            out.add(Integer.parseInt(m.group(1)));
            last = m.end();
        }
        out.add(text.substring(last).toLowerCase());
        return out;
    }

    /** 两个文件名的自然序比较。 */
    public static int naturalCompare(String a, String b) {
        List<Object> ka = naturalKey(a);
        List<Object> kb = naturalKey(b);
        int n = Math.min(ka.size(), kb.size());
        for (int i = 0; i < n; i++) {
            int c = compareElement(ka.get(i), kb.get(i));
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(ka.size(), kb.size());
    }

    private static int compareElement(Object x, Object y) {
        if (x instanceof Integer xi && y instanceof Integer yi) {
            return Integer.compare(xi, yi);
        }
        return ((String) x).compareTo((String) y);
    }

    private static boolean isNoise(String basename) {
        return basename.startsWith(".") || NOISE_BASENAMES.contains(basename.toLowerCase());
    }

    // ══════════════ 图片尺寸解析（逐字节对齐 Portal） ══════════════

    /**
     * 从文件头字节解析 (宽, 高)，支持 PNG / JPEG / GIF / WEBP；认不出或字节不够返回 null。
     * 按字节签名识别而不看扩展名。只需要前几十 KB。
     */
    public static int[] parseImageSize(byte[] head) {
        if (head == null || head.length < 10) {
            return null;
        }
        try {
            if (bytesEq(head, 0, "\u0089PNG\r\n\u001a\n")) {
                if (head.length < 24 || !bytesEq(head, 12, "IHDR")) {
                    return null;
                }
                return positive(readUIntBE(head, 16), readUIntBE(head, 20));
            }
            if (bytesEq(head, 0, "GIF87a") || bytesEq(head, 0, "GIF89a")) {
                return positive(readUIntLE(head, 6), readUIntLE(head, 8));
            }
            if (bytesEq(head, 0, "RIFF") && bytesEq(head, 8, "WEBP")) {
                return parseWebpSize(head);
            }
            if ((head[0] & 0xff) == 0xff && (head[1] & 0xff) == 0xd8) {
                return parseJpegSize(head);
            }
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        return null;
    }

    private static int[] parseWebpSize(byte[] head) {
        if (bytesEq(head, 12, "VP8 ")) {
            if (head.length < 30 || !bytesEq(head, 23, "\u009d\u0001\u002a")) {
                return null;
            }
            return positive(readUIntLE(head, 26) & 0x3FFF, readUIntLE(head, 28) & 0x3FFF);
        }
        if (bytesEq(head, 12, "VP8L")) {
            if (head.length < 25 || (head[20] & 0xff) != 0x2F) {
                return null;
            }
            long bits = readUIntLE(head, 21);
            return positive((int) ((bits & 0x3FFF) + 1), (int) (((bits >> 14) & 0x3FFF) + 1));
        }
        if (bytesEq(head, 12, "VP8X")) {
            if (head.length < 30) {
                return null;
            }
            return positive(readUInt24LE(head, 24) + 1, readUInt24LE(head, 27) + 1);
        }
        return null;
    }

    private static int[] parseJpegSize(byte[] head) {
        int pos = 2;
        int n = head.length;
        while (pos + 4 <= n) {
            if ((head[pos] & 0xff) != 0xff) {
                return null;
            }
            int marker = head[pos + 1] & 0xff;
            if (marker == 0xff) {
                pos += 1;
                continue;
            }
            if (isJpegStandalone(marker)) {
                pos += 2;
                continue;
            }
            if (marker == 0xda || marker == 0xd9) {
                return null;
            }
            if (isJpegSof(marker)) {
                if (pos + 9 > n) {
                    return null;
                }
                int h = readUIntBE(head, pos + 5);
                int w = readUIntBE(head, pos + 7);
                return positive(w, h);
            }
            int segLen = readUIntBE(head, pos + 2);
            if (segLen < 2) {
                return null;
            }
            pos += 2 + segLen;
        }
        return null;
    }

    private static boolean isJpegStandalone(int marker) {
        if (marker == 0xd8 || marker == 0x01) {
            return true;
        }
        return marker >= 0xd0 && marker <= 0xd7;
    }

    private static boolean isJpegSof(int marker) {
        if (marker < 0xc0 || marker > 0xcf) {
            return false;
        }
        return marker != 0xc4 && marker != 0xc8 && marker != 0xcc;
    }

    private static int[] positive(int w, int h) {
        return (w > 0 && h > 0) ? new int[]{w, h} : null;
    }

    private static int readUIntBE(byte[] b, int off) {
        return ((b[off] & 0xff) << 24) | ((b[off + 1] & 0xff) << 16) | ((b[off + 2] & 0xff) << 8) | (b[off + 3] & 0xff);
    }

    private static int readUIntLE(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8) | ((b[off + 2] & 0xff) << 16) | ((b[off + 3] & 0xff) << 24);
    }

    private static int readUInt24LE(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8) | ((b[off + 2] & 0xff) << 16);
    }

    private static boolean bytesEq(byte[] b, int off, String ascii) {
        for (int i = 0; i < ascii.length(); i++) {
            if ((b[off + i] & 0xff) != ascii.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    // ══════════════ 图片目录判型 ══════════════

    /** 从图片目录的页列表里挑最多 4 张做判型样本（含首尾，中间均匀取）。 */
    public static List<String> sampleImagePages(List<String> pages) {
        int total = pages.size();
        if (total <= IMAGE_DIR_SAMPLE_SIZE) {
            return new ArrayList<>(pages);
        }
        TreeSet<Integer> picked = new TreeSet<>();
        for (int i = 0; i < IMAGE_DIR_SAMPLE_SIZE; i++) {
            picked.add((int) ((2L * i * (total - 1) + 3) / 6));
        }
        List<String> out = new ArrayList<>();
        for (int idx : picked) {
            out.add(pages.get(idx));
        }
        return out;
    }

    /**
     * 纯图片目录判型：≥2 张且每张尺寸已知、宽高比两两在 1% 容差内 → cards；否则 article。
     * 任何一张探不到尺寸就证明不了，直接按 article（保守解）。
     * 入参是惰性迭代器：遇到未知或不同比即返回，application 借此少读几个文件头。
     */
    public static String decideImageDirKind(java.util.Iterator<int[]> sizes) {
        double lo = 0;
        double hi = 0;
        boolean first = true;
        int count = 0;
        while (sizes.hasNext()) {
            int[] size = sizes.next();
            if (size == null || size[0] <= 0 || size[1] <= 0) {
                return "article";
            }
            double ratio = (double) size[0] / size[1];
            if (first) {
                lo = ratio;
                hi = ratio;
                first = false;
            } else {
                lo = Math.min(lo, ratio);
                hi = Math.max(hi, ratio);
            }
            if (hi - lo > ASPECT_RATIO_TOLERANCE * lo) {
                return "article";
            }
            count++;
        }
        return count >= CARDS_MIN_IMAGES ? "cards" : "article";
    }

    // ══════════════ 目录树解析 ══════════════

    public static ParsedTree parseTree(List<TreeEntry> entries) {
        Map<String, ParsedCourse> courses = new LinkedHashMap<>();
        Map<DirGroupKey, List<DirFile>> dirGroups = new LinkedHashMap<>();
        Map<DirectGroupKey, List<DirFile>> directGroups = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();
        List<ParsedLesson> imageDirs = new ArrayList<>();

        for (TreeEntry e : entries) {
            String key = e.key();
            if (key == null || key.isEmpty() || key.endsWith("/")) {
                continue;
            }
            String[] parts = key.split("/");
            if (isNoise(parts[parts.length - 1])) {
                continue;
            }
            if (parts.length < 2 || anyEmpty(parts)) {
                skipped.add(key);
                continue;
            }
            String category = parts[0];
            if (parts.length == 2) {
                ParsedLesson lesson = fileLesson(key, parts[1], e.size());
                if (lesson == null) {
                    skipped.add(key);
                } else {
                    courseOf(courses, category + "/", category, category).lessons.add(lesson);
                }
                continue;
            }
            String courseDir = parts[1];
            String entry = parts[2];
            if (parts.length == 3) {
                directGroups.computeIfAbsent(new DirectGroupKey(category, courseDir), k -> new ArrayList<>())
                        .add(new DirFile(key, entry, e.size()));
                continue;
            }
            String rel = String.join("/", Arrays.copyOfRange(parts, 3, parts.length));
            dirGroups.computeIfAbsent(new DirGroupKey(category + "/" + courseDir + "/", entry), k -> new ArrayList<>())
                    .add(new DirFile(key, rel, e.size()));
        }

        for (Map.Entry<DirGroupKey, List<DirFile>> en : dirGroups.entrySet()) {
            String coursePrefix = en.getKey().coursePrefix();
            String lessonDir = en.getKey().lessonDir();
            String[] cp = coursePrefix.split("/");
            String category = cp[0];
            String courseDir = cp[1];
            ClassifyResult result = classifyDirLesson(lessonDir, en.getValue(), skipped);
            if (result == null) {
                continue;
            }
            SortResult sr = splitSortPrefix(lessonDir);
            ParsedLesson lesson = new ParsedLesson();
            lesson.setSortOrder(sr.order());
            lesson.setTitle(sr.title().isEmpty() ? lessonDir : sr.title());
            lesson.setKind(result.kind());
            lesson.setSourcePrefix(coursePrefix + lessonDir + "/");
            lesson.setObjectKey(result.objectKey());
            lesson.setPdfKey(result.pdfKey());
            lesson.setPages(result.pages());
            lesson.setSizeBytes(result.sizeBytes());
            courseOf(courses, coursePrefix, category, courseDir).lessons.add(lesson);
            if (result.pending()) {
                imageDirs.add(lesson);
            }
        }

        for (Map.Entry<DirectGroupKey, List<DirFile>> en : directGroups.entrySet()) {
            String category = en.getKey().category();
            String secondDir = en.getKey().secondDir();
            String coursePrefix = category + "/" + secondDir + "/";
            if (courses.containsKey(coursePrefix) || directFilesProveCourse(en.getValue())) {
                for (DirFile f : en.getValue()) {
                    ParsedLesson lesson = fileLesson(f.key(), f.rel(), f.size());
                    if (lesson == null) {
                        skipped.add(f.key());
                    } else {
                        courseOf(courses, coursePrefix, category, secondDir).lessons.add(lesson);
                    }
                }
                continue;
            }
            ClassifyResult result = classifyDirLesson(secondDir, en.getValue(), skipped);
            if (result == null) {
                continue;
            }
            SortResult sr = splitSortPrefix(secondDir);
            ParsedLesson lesson = new ParsedLesson();
            lesson.setSortOrder(sr.order());
            lesson.setTitle(sr.title().isEmpty() ? secondDir : sr.title());
            lesson.setKind(result.kind());
            lesson.setSourcePrefix(coursePrefix);
            lesson.setObjectKey(result.objectKey());
            lesson.setPdfKey(result.pdfKey());
            lesson.setPages(result.pages());
            lesson.setSizeBytes(result.sizeBytes());
            courseOf(courses, category + "/", category, category).lessons.add(lesson);
            if (result.pending()) {
                imageDirs.add(lesson);
            }
        }

        ParsedTree tree = new ParsedTree();
        tree.setSkipped(new ArrayList<>(new TreeSet<>(skipped)));
        tree.setImageDirs(imageDirs);
        List<String> prefixes = new ArrayList<>(courses.keySet());
        Collections.sort(prefixes);
        for (String prefix : prefixes) {
            ParsedCourse course = courses.get(prefix);
            course.lessons.sort((a, b) -> {
                int cmp = Integer.compare(a.getSortOrder(), b.getSortOrder());
                return cmp != 0 ? cmp : naturalCompare(a.getTitle(), b.getTitle());
            });
            tree.courses.add(course);
        }
        return tree;
    }

    private static ParsedCourse courseOf(Map<String, ParsedCourse> courses, String prefix, String category, String title) {
        ParsedCourse course = courses.get(prefix);
        if (course == null) {
            course = new ParsedCourse();
            course.setCategory(category);
            course.setTitle(title);
            course.setSourcePrefix(prefix);
            course.setSortOrder(countChar(prefix, '/') > 1 ? splitSortPrefix(title).order() : 0);
            courses.put(prefix, course);
        }
        return course;
    }

    /** 直接文件课时：只认 mp4 → video、pdf → pdf；其余返回 null。 */
    private static ParsedLesson fileLesson(String key, String filename, long size) {
        String ext = extensionOf(filename);
        String kind;
        if ("mp4".equals(ext)) {
            kind = "video";
        } else if ("pdf".equals(ext)) {
            kind = "pdf";
        } else {
            return null;
        }
        SortResult sr = splitSortPrefix(stripExtension(filename));
        ParsedLesson lesson = new ParsedLesson();
        lesson.setSortOrder(sr.order());
        lesson.setTitle(sr.title().isEmpty() ? filename : sr.title());
        lesson.setKind(kind);
        lesson.setSourcePrefix(key);
        lesson.setObjectKey(key);
        lesson.setSizeBytes(size);
        return lesson;
    }

    /** 判一个课时目录是 html 图文、图卡还是图片序列图文；都不是则整目录进 skipped。 */
    private static ClassifyResult classifyDirLesson(String lessonDir, List<DirFile> files, List<String> skipped) {
        LinkedHashMap<String, DirFile> direct = new LinkedHashMap<>();
        for (DirFile f : files) {
            if (!f.rel().contains("/")) {
                direct.put(f.rel(), f);
            }
        }
        DirFile htmlHit = null;
        for (Map.Entry<String, DirFile> en : direct.entrySet()) {
            if (ARTICLE_ENTRY.equals(en.getKey().toLowerCase())) {
                htmlHit = en.getValue();
                break;
            }
        }
        if (htmlHit != null) {
            List<String> pdfs = new ArrayList<>();
            for (String rel : direct.keySet()) {
                if ("pdf".equals(extensionOf(rel))) {
                    pdfs.add(rel);
                }
            }
            pdfs.sort(PortalTrainingTreeParser::naturalCompare);
            String pdfKey = pdfs.isEmpty() ? null : direct.get(pdfs.get(0)).key();
            for (DirFile f : files) {
                if (!f.key().equals(htmlHit.key()) && !f.key().equals(pdfKey)) {
                    skipped.add(f.key());
                }
            }
            return new ClassifyResult("article", htmlHit.key(), pdfKey, null, htmlHit.size(), false);
        }
        if (!files.isEmpty() && allImages(files)) {
            List<DirFile> ordered = new ArrayList<>(files);
            ordered.sort((a, b) -> naturalCompare(a.rel(), b.rel()));
            List<String> pages = ordered.stream().map(DirFile::key).toList();
            long total = ordered.stream().mapToLong(DirFile::size).sum();
            return new ClassifyResult("article", null, null, pages, total, true);
        }
        for (DirFile f : files) {
            skipped.add(f.key());
        }
        return null;
    }

    private static boolean allImages(List<DirFile> files) {
        for (DirFile f : files) {
            if (!IMAGE_EXTS.contains(extensionOf(f.rel()))) {
                return false;
            }
        }
        return true;
    }

    /** 二级目录里的直接文件能否证明它是课程目录（而非主题下的一节课时）。 */
    private static boolean directFilesProveCourse(List<DirFile> files) {
        List<String> names = files.stream().map(f -> f.rel().toLowerCase()).toList();
        for (String n : names) {
            if ("mp4".equals(extensionOf(n))) {
                return true;
            }
        }
        if (names.contains(ARTICLE_ENTRY)) {
            return false;
        }
        for (String n : names) {
            if ("pdf".equals(extensionOf(n))) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyEmpty(String[] parts) {
        for (String p : parts) {
            if (p.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static int countChar(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }
}
