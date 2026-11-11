package com.cyt.center.module.portal.service.branddoc;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.portal.dal.mysql.branddoc.PortalBrandDocumentRepository;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.cyt.center.module.portal.service.support.PortalDocumentStorage;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortalBrandDocumentService {
    private final PortalBrandDocumentRepository repository;
    private final PortalDocumentStorage storage;
    private final PortalOperatorResolver operators;
    private final PortalAudit audit;

    public PageResult<Map<String,Object>> page(String q, String brand, String status, int page, int size) {
        int n=Math.max(1,page), s=Math.min(100,Math.max(1,size));
        return new PageResult<>(repository.count(q,brand,status),(long)n,(long)s,repository.page(q,brand,status,(n-1)*s,s));
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> upload(Long userId,String brand,String type,String title,String filename,byte[] bytes) {
        brand=slug(brand); type=slug(type); title=title(title);
        if(filename==null || !filename.toLowerCase(java.util.Locale.ROOT).endsWith(".html") || bytes.length==0 || bytes.length>20*1024*1024)
            throw new BizException("仅接受不超过 20MB 的 HTML 文件");
        try {
            String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            if(!text.substring(0,Math.min(4096,text.length())).toLowerCase(java.util.Locale.ROOT).contains("<html")) throw new Exception();
        } catch(Exception e) { throw new BizException("HTML 文件须使用 UTF-8 编码并包含 html 标记"); }
        String key=storage.store("brand-docs","html",bytes);
        String actor=operators.resolve(userId).displayNameOrUsername();
        Long id=repository.slot(brand,type);
        boolean replaced=id!=null;
        if(id==null) {
            try { id=repository.insert(brand,type,title,key,bytes.length,actor); }
            catch(DuplicateKeyException e) { throw new BizException("该文档刚被其他人创建，请刷新后重试"); }
        } else repository.replace(id,title,key,bytes.length,actor);
        repository.version(id,key,bytes.length,actor);
        audit.record(userId,replaced?"brand_doc_replace":"brand_doc_create","portal_brand_docs",id,null,brand+"/"+type,null);
        return repository.get(id,false);
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> update(Long userId,long id,String title,String status) {
        Map<String,Object> row=repository.get(id,true);
        if(title==null) title=(String)row.get("title"); else title=title(title);
        if(status==null) status=(String)row.get("status");
        if(!List.of("published","offline").contains(status)) throw new BizException("状态必须为 published 或 offline");
        repository.update(id,title,status,operators.resolve(userId).displayNameOrUsername());
        audit.record(userId,"brand_doc_update","portal_brand_docs",id,null,title+" "+status,null);
        return repository.get(id,false);
    }

    public List<Map<String,Object>> versions(long id) { repository.get(id,false); return repository.versions(id); }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> rollback(Long userId,long id,long versionId) {
        Map<String,Object> row=repository.get(id,true), v=repository.version(id,versionId);
        repository.replace(id,(String)row.get("title"),(String)v.get("fileUrl"),((Number)v.get("fileSize")).longValue(),operators.resolve(userId).displayNameOrUsername());
        audit.record(userId,"brand_doc_rollback","portal_brand_docs",id,null,"version="+versionId,null);
        return repository.get(id,false);
    }

    @Transactional(rollbackFor=Exception.class)
    public void delete(Long userId,long id) {
        repository.get(id,true); repository.delete(id);
        audit.record(userId,"brand_doc_delete","portal_brand_docs",id,null,null,null);
    }

    public Map<String,Object> get(long id) { return repository.get(id,false); }
    public byte[] file(long id) { return storage.read((String)get(id).get("fileUrl")); }
    private String slug(String s) { if(s==null || !s.matches("[a-z0-9][a-z0-9-]{0,30}")) throw new BizException("品牌和文档类型须为小写字母、数字或连字符，最多 31 字符"); return s; }
    private String title(String s) { if(s==null || s.isBlank() || s.trim().length()>120) throw new BizException("标题须为 1 至 120 字"); return s.trim(); }
}
