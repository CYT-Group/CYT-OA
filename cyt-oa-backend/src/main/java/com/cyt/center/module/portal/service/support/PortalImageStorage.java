package com.cyt.center.module.portal.service.support;

import com.cyt.center.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PortalImageStorage {
    private final PortalDocumentStorage storage;
    public String upload(String folder,String filename,byte[] bytes) {
        if(folder==null||!folder.matches("(?:news|wiki|hr|hrdoc|covers|training|products|materials|brand-docs)(?:/[a-z0-9-]{1,32}){0,2}")) throw new BizException("无效的图片分类");
        if(bytes.length==0||bytes.length>20*1024*1024)throw new BizException("图片须为 1 字节至 20MB");
        String ext=filename==null||!filename.contains(".")?"":filename.substring(filename.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        boolean valid=switch(ext){case "png"->starts(bytes,new byte[]{(byte)137,80,78,71,13,10,26,10});case "jpg","jpeg"->starts(bytes,new byte[]{(byte)255,(byte)216,(byte)255});case "gif"->starts(bytes,"GIF87a".getBytes(StandardCharsets.US_ASCII))||starts(bytes,"GIF89a".getBytes(StandardCharsets.US_ASCII));case "webp"->bytes.length>12&&new String(bytes,0,4,StandardCharsets.US_ASCII).equals("RIFF")&&new String(bytes,8,4,StandardCharsets.US_ASCII).equals("WEBP");default->false;};
        if(!valid) throw new BizException("仅支持内容与扩展名一致的 PNG、JPEG、GIF、WebP 图片");
        String key=storage.store("images",ext,bytes);return "/api/portal/media/"+key;
    }
    public byte[] read(String name) { mime(name);return storage.read("images/"+name); }
    public String mime(String name) {
        if(name==null||!name.matches("[a-f0-9]{32}\\.(png|jpe?g|gif|webp)"))throw new BizException("图片不存在");
        String ext=name.substring(name.lastIndexOf('.')+1);return ext.equals("jpg")||ext.equals("jpeg")?"image/jpeg":"image/"+ext;
    }
    private boolean starts(byte[] bytes,byte[] prefix) { return bytes.length>=prefix.length&&Arrays.equals(Arrays.copyOf(bytes,prefix.length),prefix); }
}
