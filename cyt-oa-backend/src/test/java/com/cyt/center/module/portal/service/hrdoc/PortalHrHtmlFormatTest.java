package com.cyt.center.module.portal.service.hrdoc;

import com.cyt.center.module.portal.service.support.PortalHtmlSanitizer;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortalHrHtmlFormatTest {
    @Test void uploadedRelativeImageAndInternalDocumentLinksSurviveSaving(){
        var html=Jsoup.parseBodyFragment(PortalHtmlSanitizer.sanitize("<p><img src='/api/portal/media/images/hr/cover.png' alt='封面'><a href='/article/7'>制度</a><a href='../article/8'>相对</a><a href='#第一章'>目录</a></p>"));
        assertEquals("/api/portal/media/images/hr/cover.png",html.selectFirst("img").attr("src"));
        assertEquals("/article/7",html.select("a").get(0).attr("href"));assertEquals("../article/8",html.select("a").get(1).attr("href"));assertEquals("#第一章",html.select("a").get(2).attr("href"));
        assertFalse(html.html().contains("portal-sanitizer.invalid"));
    }
    @Test void dangerousAndEncodedLinkProtocolsAreStillRejected(){
        var html=Jsoup.parseBodyFragment(PortalHtmlSanitizer.sanitize("<img src='data:image/svg+xml,evil'><img src='javascript:alert(1)'><a href='jav&#x61;script:alert(1)'>坏链接</a><a href='data:text/html,evil'>坏数据</a><a href='https://example.com/path'>正常</a>"));
        assertFalse(html.select("img").get(0).hasAttr("src"));assertFalse(html.select("img").get(1).hasAttr("src"));
        assertFalse(html.select("a").get(0).hasAttr("href"));assertFalse(html.select("a").get(1).hasAttr("href"));assertEquals("https://example.com/path",html.select("a").get(2).attr("href"));
    }
    @Test void tableAppearanceHeaderBackgroundAndColumnWidthsSurvive(){
        String input="<table class='hr-doc-table hr-doc--first-col hr-doc--no-zebra' style='width:100%;border:1px solid #ddd'><tbody><tr><th colspan='2' scope='col' data-colwidth='180,180' style='width:360px;background-color:#6366f1;color:white'>标题</th></tr><tr><td style='width:180px;text-align:right'>内容</td><td>内容</td></tr></tbody></table>";
        var html=Jsoup.parseBodyFragment(PortalHtmlSanitizer.sanitize(input));
        var table=html.selectFirst("table");assertTrue(table.hasClass("hr-doc--first-col"));assertTrue(table.hasClass("hr-doc--no-zebra"));assertTrue(table.attr("style").contains("width:100%"));
        var header=html.selectFirst("th");assertEquals("2",header.attr("colspan"));assertTrue(header.attr("style").contains("width:360px"));assertTrue(header.attr("style").contains("background-color:#6366f1"));
        assertFalse(header.hasAttr("data-colwidth"));assertTrue(html.selectFirst("td").attr("style").contains("width:180px"));
    }
    @Test void quoteColorsAndSafeBackgroundImagesRemainWhileCssExecutionIsRemoved(){
        var html=Jsoup.parseBodyFragment(PortalHtmlSanitizer.sanitize("<blockquote style='border-left-color:#aabbcc;background-color:rgba(1,2,3,.1);width:expression(evil())'>说明</blockquote><p style=\"background-image:url('/api/portal/media/images/a.png');behavior:url(evil.htc)\">正常背景</p><p style=\"background-image:url('data:image/svg+xml,evil')\">坏背景</p><p style=\"background-image:url(j\\61vascript:evil)\">转义</p>"));
        String quote=html.selectFirst("blockquote").attr("style");assertTrue(quote.contains("border-left-color:#aabbcc"));assertTrue(quote.contains("background-color:rgba(1,2,3,.1)"));assertFalse(quote.contains("expression"));
        assertTrue(html.select("p").get(0).attr("style").contains("/api/portal/media/images/a.png"));assertFalse(html.html().contains("behavior"));assertFalse(html.select("p").get(1).hasAttr("style"));assertFalse(html.select("p").get(2).hasAttr("style"));
    }
}
