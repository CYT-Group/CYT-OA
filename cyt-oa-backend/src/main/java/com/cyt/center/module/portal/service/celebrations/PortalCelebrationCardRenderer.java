package com.cyt.center.module.portal.service.celebrations;

import com.cyt.center.core.exception.BizException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;

/** 沿用 Portal 两张背景素材与 960×784 画布。用户输入只作为文字/校验后位图，绝不执行 HTML。 */
@Component
public class PortalCelebrationCardRenderer {
    private final java.util.concurrent.ConcurrentHashMap<String,Font> fonts=new java.util.concurrent.ConcurrentHashMap<>();
    public byte[] render(String name,String kind,String title,String message,Integer years,String avatarData) {
        try {
            int width=960,height=784,top=kind.equals("anniversary")?409:352;
            BufferedImage canvas=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=canvas.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);g.fillRect(0,0,width,height);
            String asset=kind.equals("anniversary")?"bg-anniversary.jpg":"bg-balloons.jpg";
            try(var in=new ClassPathResource("templates/celebrations/"+asset).getInputStream()) {g.drawImage(ImageIO.read(in),0,0,width,top,null);}
            int cx=kind.equals("anniversary")?597:739,cy=kind.equals("anniversary")?325:253,r=kind.equals("anniversary")?44:51;
            g.setColor(Color.WHITE);g.fillOval(cx-r-6,cy-r-6,2*r+12,2*r+12);
            BufferedImage avatar=avatar(avatarData);
            if(avatar!=null){var old=g.getClip();g.setClip(new Ellipse2D.Double(cx-r,cy-r,2*r,2*r));int side=Math.min(avatar.getWidth(),avatar.getHeight()),sx=(avatar.getWidth()-side)/2,sy=(avatar.getHeight()-side)/2;g.drawImage(avatar,cx-r,cy-r,cx+r,cy+r,sx,sy,sx+side,sy+side,null);g.setClip(old);}
            else {g.setColor(new Color(228,222,246));g.fillOval(cx-r,cy-r,2*r,2*r);g.setColor(new Color(120,108,190));g.setFont(font(42,true));String initial=name.substring(0,name.offsetByCodePoints(0,1));g.drawString(initial,cx-g.getFontMetrics().stringWidth(initial)/2,cy+15);}
            if(kind.equals("birthday")){g.setColor(new Color(233,86,118));g.fillPolygon(new int[]{cx-28,cx+28,cx+12},new int[]{cy-r+8,cy-r+8,cy-r-62},3);g.setColor(Color.WHITE);g.fillOval(cx+5,cy-r-69,15,15);}
            int y=top+48,titleSize=36;List<String> titleLines;
            do{g.setFont(font(titleSize,true));titleLines=lines(g,title,848,1000);if(titleLines.size()<=3||titleSize==20)break;titleSize--;}while(true);
            g.setColor(new Color(44,36,84));for(String line:titleLines){g.drawString(line,56,y);y+=titleSize+8;}
            g.setColor(new Color(214,176,120));g.fillRect(56,y+2,58,3);y+=30;
            int bodySize=22;List<String> bodyLines;
            do{g.setFont(font(bodySize,false));bodyLines=lines(g,message,848,1000);if((bodyLines.size()-1)*(bodySize+6)<=height-80-y||bodySize==12)break;bodySize--;}while(true);
            if((bodyLines.size()-1)*(bodySize+6)>height-80-y)throw new BizException("祝福文字行数过多，请减少空行或缩短文案后生成图片");
            g.setColor(new Color(96,92,122));for(String line:bodyLines){g.drawString(line,56,y);y+=bodySize+6;}
            g.setColor(new Color(150,147,170));g.setFont(font(17,false));g.drawString(LocalDate.now(ZoneId.of("Asia/Shanghai")).toString(),56,height-40);g.drawString("畅韵科技",800,height-40);
            g.dispose();ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(canvas,"jpg",out);return out.toByteArray();
        }catch(BizException ex){throw ex;}catch(Exception ex){throw new BizException("贺卡图片生成失败，请检查服务器素材与中文字体配置");}
    }
    private Font font(int size,boolean bold) {
        String cacheKey=size+":"+bold;Font cached=fonts.get(cacheKey);if(cached!=null)return cached;
        String custom=System.getenv("PORTAL_CARD_FONT");
        for(String file:List.of(custom==null?"":custom,"C:/Windows/Fonts/msyh.ttc","/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc","/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc")) {
            if(file.isBlank()||!Files.isRegularFile(Path.of(file)))continue;
            try{Font base=fonts.computeIfAbsent("base:"+file,k->{try{return Font.createFont(Font.TRUETYPE_FONT,Path.of(file).toFile());}catch(Exception e){throw new IllegalStateException(e);}});Font font=base.deriveFont(bold?Font.BOLD:Font.PLAIN,(float)size);if(font.canDisplay('生')){fonts.putIfAbsent(cacheKey,font);return font;}}catch(Exception ignored){}
        }
        Font fallback=new Font(Font.SANS_SERIF,bold?Font.BOLD:Font.PLAIN,size);
        if(!fallback.canDisplay('生'))throw new BizException("服务器缺少中文字体，请安装 Noto CJK 或设置 PORTAL_CARD_FONT");fonts.putIfAbsent(cacheKey,fallback);return fallback;
    }
    private List<String> lines(Graphics2D g,String text,int width,int max) {
        java.util.ArrayList<String> out=new java.util.ArrayList<>();StringBuilder line=new StringBuilder();
        for(int i=0;i<text.length();){int cp=text.codePointAt(i);i+=Character.charCount(cp);String unit=new String(Character.toChars(cp));if(cp=='\n'||g.getFontMetrics().stringWidth(line+unit)>width){out.add(line.toString());line.setLength(0);if(out.size()>=max)return out;if(cp=='\n')continue;}line.append(unit);}
        if(!line.isEmpty()&&out.size()<max)out.add(line.toString());return out;
    }
    private BufferedImage avatar(String value) throws Exception {
        if(value==null||value.isBlank())return null;
        if(value.length()>4*1024*1024+100)throw new BizException("头像不能超过3MB");
        int comma=value.indexOf(',');if(comma<0||!value.substring(0,comma).matches("data:image/(png|jpeg);base64"))throw new BizException("头像须为PNG或JPEG图片");
        byte[] bytes;try{bytes=Base64.getDecoder().decode(value.substring(comma+1));}catch(Exception ex){throw new BizException("头像编码无效");}
        if(bytes.length>3*1024*1024)throw new BizException("头像不能超过3MB");
        try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new BizException("头像不是有效图片");ImageReader reader=readers.next();try{reader.setInput(stream);int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||(long)w*h>16000000)throw new BizException("头像像素不能超过1600万");return reader.read(0);}finally{reader.dispose();}}
    }
}
