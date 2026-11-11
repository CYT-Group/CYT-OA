package com.cyt.center.module.portal.service.admin;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.module.portal.controller.admin.events.vo.PortalEventSaveReqVO;
import com.cyt.center.module.portal.controller.admin.products.vo.PortalProductSaveReqVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingSaveReqVO;
import com.cyt.center.module.portal.controller.admin.wiki.vo.PortalWikiSaveReqVO;
import com.cyt.center.module.portal.service.events.PortalEventService;
import com.cyt.center.module.portal.service.products.PortalProductService;
import com.cyt.center.module.portal.service.training.PortalTrainingService;
import com.cyt.center.module.portal.service.wiki.PortalWikiService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Validator;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PortalImportService {
    private final CurrentUser user;
    private final PortalWikiService wiki;
    private final PortalTrainingService training;
    private final PortalEventService events;
    private final PortalProductService products;
    private final ObjectMapper json;
    private final Validator validator;
    private final TransactionTemplate transactions;
    private static final int MAX_ROWS = 2000;
    private static final Map<String, String> HEADERS =
            Map.ofEntries(
                    Map.entry("标题", "title"),
                    Map.entry("名称", "name"),
                    Map.entry("分类", "category"),
                    Map.entry("部门", "department"),
                    Map.entry("内容", "content"),
                    Map.entry("正文", "content"),
                    Map.entry("链接", "docUrl"),
                    Map.entry("标签", "tags"),
                    Map.entry("描述", "description"),
                    Map.entry("简介", "description"),
                    Map.entry("地点", "location"),
                    Map.entry("开始日期", "startDate"),
                    Map.entry("结束日期", "endDate"),
                    Map.entry("类型", "eventType"),
                    Map.entry("品牌", "brand"),
                    Map.entry("上市日期", "launchDate"),
                    Map.entry("级别", "level"),
                    Map.entry("讲师", "instructor"),
                    Map.entry("封面", "coverImage"),
                    Map.entry("文档链接", "docUrl"));

    public Map<String, Object> run(String type, MultipartFile file, boolean preview) {
        user.requirePermission("can_import");
        Class<?> clazz =
                switch (type) {
                    case "wiki" -> PortalWikiSaveReqVO.class;
                    case "training" -> PortalTrainingSaveReqVO.class;
                    case "events" -> PortalEventSaveReqVO.class;
                    case "products" -> PortalProductSaveReqVO.class;
                    default -> throw new BizException("不支持的导入类型");
                };
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024)
            throw new BizException("请选择10MB以内的导入文件");
        List<List<String>> rows = read(file);
        if (rows.size() < 2) throw new BizException("文件须包含标题行和至少一条记录");
        if (rows.size() > MAX_ROWS + 1) throw new BizException("一次最多导入2000条记录");
        List<String> headers =
                rows.getFirst().stream()
                        .map(x -> HEADERS.getOrDefault(x.trim(), camelHeader(x.trim())))
                        .toList();
        if (new HashSet<>(headers).size() != headers.size()) throw new BizException("文件包含重复标题列");
        List<Map<String, Object>> errors = new ArrayList<>();
        int imported = 0, valid = 0, total = 0;
        for (int i = 1; i < rows.size(); i++) {
            var values = rows.get(i);
            if (values.stream().allMatch(String::isBlank)) continue;
            total++;
            try {
                Map<String, Object> body = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++)
                    if (c < values.size() && !values.get(c).isBlank())
                        body.put(headers.get(c), values.get(c).trim());
                if (type.equals("products") && body.containsKey("docUrl"))
                    body.put("linkUrl", body.remove("docUrl"));
                if (type.equals("wiki") && body.get("tags") instanceof String s)
                    body.put(
                            "tags",
                            Arrays.stream(s.split("[,，;；|]"))
                                    .map(String::trim)
                                    .filter(x -> !x.isEmpty())
                                    .toList());
                if (type.equals("training")) {
                    if (!body.containsKey("category") || body.get("category").toString().isBlank())
                        throw new BizException("培训分类不能为空");
                    String level = String.valueOf(body.getOrDefault("level", "elective"));
                    if (!Set.of("必修", "选修", "required", "elective").contains(level))
                        throw new BizException("培训级别须为必修或选修");
                    body.put(
                            "level",
                            Set.of("必修", "required").contains(level) ? "required" : "elective");
                }
                // 身份和主键不接受文件覆盖；导入只创建新内容。
                body.remove("id");
                body.remove("authorUsername");
                body.remove("createdAt");
                Object dto = json.convertValue(body, clazz);
                var problems = validator.validate(dto);
                if (!problems.isEmpty())
                    throw new BizException(problems.iterator().next().getMessage());
                if (!preview) {
                    transactions.executeWithoutResult(
                            status -> {
                                switch (type) {
                                    case "wiki" ->
                                            wiki.create(
                                                    user.requireUserId(),
                                                    (PortalWikiSaveReqVO) dto);
                                    case "training" ->
                                            training.create(
                                                    user.requireUserId(),
                                                    (PortalTrainingSaveReqVO) dto);
                                    case "events" -> events.create((PortalEventSaveReqVO) dto);
                                    case "products" ->
                                            products.create((PortalProductSaveReqVO) dto);
                                    default -> throw new IllegalStateException();
                                }
                            });
                    imported++;
                }
                valid++;
            } catch (Exception e) {
                String message = e instanceof BizException ? e.getMessage() : "字段格式或业务校验未通过";
                errors.add(Map.of("row", i + 1, "message", message));
            }
        }
        return Map.of(
                "preview",
                preview,
                "total",
                total,
                "valid",
                valid,
                "imported",
                imported,
                "failed",
                errors.size(),
                "errors",
                errors);
    }

    private List<List<String>> read(MultipartFile file) {
        String name =
                Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        try {
            byte[] bytes = file.getBytes();
            if (name.endsWith(".csv")) {
                String text =
                        StandardCharsets.UTF_8
                                .newDecoder()
                                .decode(java.nio.ByteBuffer.wrap(bytes))
                                .toString();
                return parseCsv(text);
            }
            if (!name.endsWith(".xlsx") && !name.endsWith(".xls"))
                throw new BizException("支持 UTF-8 CSV、XLSX 和 XLS 文件");
            boolean zip = bytes.length > 3 && bytes[0] == 'P' && bytes[1] == 'K',
                    ole = bytes.length > 7 && (bytes[0] & 255) == 0xD0 && (bytes[1] & 255) == 0xCF;
            if (name.endsWith(".xlsx") ? !zip : !ole) throw new BizException("文件内容与扩展名不符");
            try (Workbook book = WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes))) {
                Sheet sheet = book.getSheetAt(0);
                if (sheet.getLastRowNum() > MAX_ROWS) throw new BizException("一次最多导入2000条记录");
                List<List<String>> rows = new ArrayList<>();
                DataFormatter fmt = new DataFormatter(Locale.ROOT);
                for (Row row : sheet) {
                    List<String> values = new ArrayList<>();
                    if (row.getLastCellNum() > 100) throw new BizException("最多100列");
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        if (cell != null && cell.getCellType() == CellType.FORMULA)
                            throw new BizException("导入文件不能含公式，请粘贴为值后重试");
                        if (cell != null
                                && cell.getCellType() == CellType.NUMERIC
                                && DateUtil.isCellDateFormatted(cell))
                            values.add(cell.getLocalDateTimeCellValue().toLocalDate().toString());
                        else values.add(cell == null ? "" : fmt.formatCellValue(cell));
                    }
                    rows.add(values);
                }
                return rows;
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("无法读取文件，请使用有效的 Excel 或 UTF-8 CSV 文件");
        }
    }

    static List<List<String>> parseCsv(String input) {
        if (input.startsWith("\uFEFF")) input = input.substring(1);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < input.length() && input.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (quoted || cell.isEmpty()) quoted = !quoted;
                else throw new BizException("CSV 引号位置无效");
            } else if (!quoted && (ch == ',' || ch == '\n' || ch == '\r')) {
                row.add(cell.toString());
                cell.setLength(0);
                if (ch != ',') {
                    if (ch == '\r' && i + 1 < input.length() && input.charAt(i + 1) == '\n') i++;
                    rows.add(row);
                    row = new ArrayList<>();
                    if (rows.size() > MAX_ROWS + 1) throw new BizException("一次最多导入2000条记录");
                }
            } else cell.append(ch);
            if (cell.length() > 200000 || row.size() > 100) throw new BizException("单元格或列数超出限制");
        }
        if (quoted) throw new BizException("CSV 引号未闭合");
        if (!cell.isEmpty() || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }

    private static String camelHeader(String key) {
        StringBuilder out = new StringBuilder();
        boolean upper = false;
        for (char c : key.toCharArray()) {
            if (c == '_') {
                upper = true;
                continue;
            }
            out.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return out.toString();
    }
}
