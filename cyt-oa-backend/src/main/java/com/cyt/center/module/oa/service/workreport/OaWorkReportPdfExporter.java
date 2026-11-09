package com.cyt.center.module.oa.service.workreport;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.oa.controller.admin.workreport.vo.OaWorkReportRespVO;
import com.cyt.center.module.oa.dal.dataobject.workreport.OaWorkReportDO;
import com.cyt.center.module.oa.enums.workreport.OaWorkReportStatusEnum;
import com.cyt.center.module.oa.enums.workreport.OaWorkReportTypeEnum;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 工作汇报 PDF 导出。中文字体从配置或常见系统路径按回退链加载，避免硬编码单一环境路径。
 */
@Component
public class OaWorkReportPdfExporter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<String> FONT_CANDIDATES = List.of(
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simsun.ttc",
            "C:/Windows/Fonts/msyh.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc");

    @Value("${cyt.oa.work-report.pdf-font:}")
    private String configuredFont;

    public byte[] export(OaWorkReportRespVO report) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont font = loadFont(doc);
            new Renderer(doc, font).render(report);
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException("工作汇报 PDF 导出失败：" + e.getMessage());
        }
    }

    private PDFont loadFont(PDDocument doc) throws IOException {
        if (configuredFont != null && !configuredFont.isBlank()) {
            File file = new File(configuredFont);
            if (file.isFile()) {
                return loadFontFile(doc, file);
            }
            throw new BizException("配置的中文字体不存在：" + configuredFont);
        }
        for (String candidate : FONT_CANDIDATES) {
            File file = new File(candidate);
            if (file.isFile()) {
                return loadFontFile(doc, file);
            }
        }
        throw new BizException("未找到可用的中文字体，请在配置项 cyt.oa.work-report.pdf-font 指定字体文件路径");
    }

    private PDFont loadFontFile(PDDocument doc, File file) throws IOException {
        if (file.getName().toLowerCase().endsWith(".ttc")) {
            // TTC 集合取第一个字体，与系统默认渲染行为一致
            try (TrueTypeCollection ttc = new TrueTypeCollection(file)) {
                TrueTypeFont[] first = new TrueTypeFont[1];
                ttc.processAllFonts(font -> {
                    if (first[0] == null) {
                        first[0] = font;
                    }
                });
                if (first[0] == null) {
                    throw new IOException("字体集合中没有可用字体：" + file.getName());
                }
                return PDType0Font.load(doc, first[0], true);
            }
        }
        return PDType0Font.load(doc, file);
    }

    /** 单页 A4 渲染器，按剩余高度自动翻页，中文按字符换行。 */
    private static final class Renderer {

        private static final float MARGIN = 56f;
        private static final float BOTTOM = 64f;

        private final PDDocument doc;
        private final PDFont font;
        private PDPageContentStream cs;
        private PDPage page;
        private float y;

        Renderer(PDDocument doc, PDFont font) {
            this.doc = doc;
            this.font = font;
        }

        void render(OaWorkReportRespVO r) throws IOException {
            newPage();
            String title = r.getTitle() == null || r.getTitle().isBlank()
                    ? typeName(r) + " " + r.getPeriodKey() : r.getTitle();
            writeLine(title, 18, 20);
            y -= 6;
            meta("汇报单号", r.getNo());
            meta("汇报类型", typeName(r));
            meta("汇报周期", r.getPeriodKey());
            meta("汇报人", join(r.getUserName(), r.getDeptName()));
            meta("状态", OaWorkReportStatusEnum.valueOf(r.getStatus()).getName());
            meta("创建时间", r.getCreateTime() == null ? "" : DATE_TIME.format(r.getCreateTime()));
            y -= 8;

            section("工作总结");
            if (blank(r.getSummary())) {
                writeLine("（未填写）", 10, 12, 0.63f, 0.63f, 0.63f);
            } else {
                writeWrapped(r.getSummary(), 11, 16);
            }
            section("已完成工作项");
            if (r.getWorkItems() == null || r.getWorkItems().isEmpty()) {
                writeLine("（未填写）", 10, 12, 0.63f, 0.63f, 0.63f);
            } else {
                int i = 1;
                for (OaWorkReportDO.WorkItem item : r.getWorkItems()) {
                    writeWrapped(i++ + ". " + item.getContent()
                            + "（进度 " + (item.getProgress() == null ? 0 : item.getProgress()) + "%）", 11, 16);
                }
            }
            section("工作计划");
            if (blank(r.getPlan()) && (r.getPlanItems() == null || r.getPlanItems().isEmpty())) {
                writeLine("（未填写）", 10, 12, 0.63f, 0.63f, 0.63f);
            } else {
                if (!blank(r.getPlan())) {
                    writeWrapped(r.getPlan(), 11, 16);
                }
                if (r.getPlanItems() != null) {
                    int i = 1;
                    for (OaWorkReportDO.PlanItem item : r.getPlanItems()) {
                        writeWrapped(i++ + ". " + item.getContent(), 11, 16);
                    }
                }
            }
            section("问题与协调事项");
            if (blank(r.getProblem())) {
                writeLine("（未填写）", 10, 12, 0.63f, 0.63f, 0.63f);
            } else {
                writeWrapped(r.getProblem(), 11, 16);
            }
            section("备注");
            if (blank(r.getRemark())) {
                writeLine("（未填写）", 10, 12, 0.63f, 0.63f, 0.63f);
            } else {
                writeWrapped(r.getRemark(), 11, 16);
            }
            cs.close();
        }

        private void newPage() throws IOException {
            if (cs != null) {
                cs.close();
            }
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        private void section(String name) throws IOException {
            ensureSpace(34);
            writeLine(name, 14, 20);
            y -= 6;
        }

        private void meta(String label, String value) throws IOException {
            ensureSpace(18);
            writeLine(label + "：" + (value == null ? "" : value), 10, 14, 0.38f, 0.38f, 0.38f);
        }

        private void writeLine(String text, float size, float leading) throws IOException {
            writeLine(text, size, leading, 0f, 0f, 0f);
        }

        private void writeLine(String text, float size, float leading, float cr, float cg, float cb) throws IOException {
            cs.beginText();
            cs.setFont(font, size);
            cs.setNonStrokingColor(cr, cg, cb);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(text == null ? "" : text);
            cs.endText();
            y -= leading;
        }

        /** 按当前行宽逐字贪心换行，行高不够自动翻页。 */
        private void writeWrapped(String text, float size, float leading) throws IOException {
            float maxWidth = PDRectangle.A4.getWidth() - MARGIN * 2;
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                String candidate = line + text.substring(i, i + 1);
                if (widthOf(candidate, size) > maxWidth && line.length() > 0) {
                    ensureSpace(leading);
                    writeLine(line.toString(), size, leading);
                    line.setLength(0);
                }
                line.append(text.charAt(i));
            }
            if (line.length() > 0) {
                ensureSpace(leading);
                writeLine(line.toString(), size, leading);
            }
        }

        private float widthOf(String text, float size) throws IOException {
            return font.getStringWidth(text) / 1000f * size;
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < BOTTOM) {
                newPage();
            }
        }

        private boolean blank(String s) {
            return s == null || s.isBlank();
        }

        private String typeName(OaWorkReportRespVO r) {
            OaWorkReportTypeEnum type = OaWorkReportTypeEnum.valueOf(r.getType());
            return type == null ? "" : type.getName();
        }

        private String join(String a, String b) {
            if (a == null || a.isBlank()) {
                return b == null ? "" : b;
            }
            return b == null || b.isBlank() ? a : a + " / " + b;
        }
    }

}
