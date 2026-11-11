package com.cyt.center.module.hrm.controller.admin.workspace;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.result.Result;
import com.cyt.center.module.hrm.service.recruit.candidate.HrmRecruitCandidateService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/hrm/workspace")
@RequiredArgsConstructor
public class HrmWorkspaceFileController {
    private final JdbcTemplate jdbc;
    private final HrmRecruitCandidateService candidates;

    @PostMapping("/template-text") @SaCheckPermission("hrm:contract-template:create")
    public Result<?> templateText(@RequestParam MultipartFile file) throws Exception {
        byte[] bytes=check(file); String name=file.getOriginalFilename().toLowerCase(Locale.ROOT); String text;
        if(name.endsWith(".pdf")) { try(var pdf=Loader.loadPDF(bytes)) { text=new PDFTextStripper().getText(pdf); } }
        else if(name.endsWith(".docx")) { try(var doc=new XWPFDocument(new ByteArrayInputStream(bytes)); var extractor=new XWPFWordExtractor(doc)) { text=extractor.getText(); } }
        else if(name.endsWith(".txt")) text=new String(bytes,StandardCharsets.UTF_8).replace("\uFEFF","");
        else throw new BizException("请上传 DOCX、PDF 或 UTF-8 TXT 模板");
        if(text.isBlank() || text.length()>50000) throw new BizException("模板须有可读取正文，且不超过 50000 字；扫描件请先转为文本");
        return Result.success(Map.of("content",text));
    }

    @GetMapping("/resumes") @SaCheckPermission("hrm:recruit:candidate:query")
    public Result<?> resumes(@RequestParam long candidateId) {
        candidates.validateRecruitCandidateExists(candidateId);
        return Result.success(jdbc.queryForList("SELECT id,filename,created_at AS createdAt FROM hrm_candidate_attachment WHERE candidate_id=? ORDER BY id DESC",candidateId));
    }
    @PostMapping("/resumes") @SaCheckPermission("hrm:recruit:candidate:update")
    @org.springframework.transaction.annotation.Transactional(rollbackFor=Exception.class)
    public Result<?> upload(@RequestParam long candidateId,@RequestParam MultipartFile file) throws Exception {
        candidates.validateRecruitCandidateExists(candidateId); byte[] bytes=check(file);
        jdbc.queryForList("SELECT id FROM hrm_recruit_candidate WHERE id=? AND deleted=0 FOR UPDATE",candidateId);
        if(jdbc.queryForObject("SELECT COUNT(*) FROM hrm_candidate_attachment WHERE candidate_id=?",Long.class,candidateId)>=5)
            throw new BizException("每位候选人最多保留 5 份简历附件");
        String name=file.getOriginalFilename();String lower=name.toLowerCase(Locale.ROOT);
        String type=lower.endsWith(".pdf")?"application/pdf":lower.endsWith(".docx")?"application/vnd.openxmlformats-officedocument.wordprocessingml.document":"";
        if(type.isBlank()) throw new BizException("简历仅支持 PDF、DOCX");
        if(lower.endsWith(".pdf")) { try(var ignored=Loader.loadPDF(bytes)) {} }
        else { try(var ignored=new XWPFDocument(new ByteArrayInputStream(bytes))) {} }
        jdbc.update("INSERT INTO hrm_candidate_attachment(candidate_id,filename,media_type,content) VALUES(?,?,?,?)",candidateId,name,type,bytes);
        return Result.success(true);
    }
    @GetMapping("/resumes/{id}/file") @SaCheckPermission("hrm:recruit:candidate:query")
    public ResponseEntity<byte[]> file(@PathVariable long id) {
        var rows=jdbc.queryForList("SELECT * FROM hrm_candidate_attachment WHERE id=?",id);
        if(rows.isEmpty()) throw new BizException("简历附件不存在");var r=rows.getFirst();
        candidates.validateRecruitCandidateExists(((Number)r.get("candidate_id")).longValue());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType((String)r.get("media_type")))
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename((String)r.get("filename"),StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL,"no-store").header("X-Content-Type-Options","nosniff").body((byte[])r.get("content"));
    }
    private byte[] check(MultipartFile file) throws Exception {
        if(file.isEmpty() || file.getSize()>10*1024*1024) throw new BizException("文件须为 1 字节至 10MB");
        String name=file.getOriginalFilename();if(name==null || name.isBlank() || name.length()>255)throw new BizException("文件名无效");
        return file.getBytes();
    }
}
