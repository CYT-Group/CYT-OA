package com.cyt.center.module.portal.service.celebrations;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;

class PortalCelebrationCardRendererTest {
    @Test void birthdayAndAnniversaryKeepOriginalCanvasSize() throws Exception {
        // 无中文字体的CI环境明确跳过位图排版验证；生产部署仍由renderer fail-closed提示缺字体。
        boolean available=new java.awt.Font(java.awt.Font.SANS_SERIF,0,12).canDisplay('生')
                || java.nio.file.Files.isRegularFile(java.nio.file.Path.of("C:/Windows/Fonts/msyh.ttc"))
                || java.nio.file.Files.isRegularFile(java.nio.file.Path.of("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"));
        org.junit.jupiter.api.Assumptions.assumeTrue(available);
        var renderer=new PortalCelebrationCardRenderer();
        for(String kind:new String[]{"birthday","anniversary"}){
            var image=ImageIO.read(new ByteArrayInputStream(renderer.render("测试员工",kind,"祝你快乐","每一份付出都被看见，每一段同行都值得珍藏。",3,null)));
            assertEquals(960,image.getWidth());assertEquals(784,image.getHeight());
        }
    }
}
