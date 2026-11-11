package com.cyt.center.module.portal.service.support;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * HR 文档 HTML 服务端清洗（对齐 Portal 的 services/html_sanitizer.py + nh3/ammonia 白名单）。
 *
 * 三层防御：
 * 1. 标签 / 属性白名单（禁 script/iframe/form/on* 等）；
 * 2. url 协议白名单（禁 javascript:/data:）；
 * 3. inline style 按 CSS 属性白名单过滤，非白名单 property 与 url(javascript:) 整条剥掉。
 *
 * jsoup 没有 nh3 的 filter_style_properties 能力，这里补一个后置遍历做 CSS 属性白名单过滤。
 */
public final class PortalHtmlSanitizer {
    /** 仅用于协议判定，不进行请求；没有 base 时 Jsoup 会误删安全的站内相对 URL。 */
    private static final String VALIDATION_BASE = "https://portal-sanitizer.invalid/";
    private static final Pattern CSS_URL = Pattern.compile("url\\s*\\(\\s*(['\"]?)(.*?)\\1\\s*\\)",Pattern.CASE_INSENSITIVE);

    private static final Set<String> TAGS = Set.of(
            "a", "abbr", "article", "aside", "b", "blockquote", "br", "code", "div",
            "em", "figcaption", "figure", "footer", "h1", "h2", "h3", "h4", "h5", "h6",
            "header", "hr", "i", "img", "li", "main", "mark", "nav", "ol", "p", "pre",
            "section", "small", "span", "strong", "sub", "summary", "details", "sup",
            "table", "tbody", "td", "tfoot", "th", "thead", "tr", "u", "ul");

    /** 全部标签通用的属性 */
    private static final String[] COMMON_ATTRS = {"class", "id", "style", "data-tab", "data-key"};

    /** 白名单 CSS 属性（对齐 Portal ALLOWED_CSS_PROPERTIES） */
    private static final Set<String> CSS_PROPERTIES = Set.of(
            "display", "position", "top", "left", "right", "bottom", "z-index",
            "flex", "flex-direction", "flex-wrap", "flex-grow", "flex-shrink", "flex-basis",
            "justify-content", "align-items", "align-self", "gap",
            "grid", "grid-template-columns", "grid-template-rows", "grid-gap", "grid-column", "grid-row",
            "width", "height", "max-width", "max-height", "min-width", "min-height",
            "overflow", "overflow-x", "overflow-y", "visibility",
            "margin", "margin-top", "margin-right", "margin-bottom", "margin-left",
            "padding", "padding-top", "padding-right", "padding-bottom", "padding-left",
            "border", "border-top", "border-right", "border-bottom", "border-left",
            "border-color", "border-top-color", "border-right-color", "border-bottom-color", "border-left-color",
            "border-style", "border-width", "border-radius",
            "box-shadow", "box-sizing", "outline",
            "color", "font", "font-family", "font-size", "font-style", "font-weight",
            "line-height", "letter-spacing", "text-align", "text-decoration", "text-transform",
            "text-indent", "word-break", "white-space", "vertical-align",
            "background", "background-color", "background-image", "background-position",
            "background-repeat", "background-size",
            "cursor", "opacity", "transition", "transform",
            "list-style", "list-style-type", "user-select", "scroll-margin-top");

    private static final Safelist SAFELIST = buildSafelist();

    private PortalHtmlSanitizer() {
    }

    private static Safelist buildSafelist() {
        Safelist safelist = new Safelist();
        safelist.addTags(TAGS.toArray(String[]::new));
        for (String tag : TAGS) {
            safelist.addAttributes(tag, COMMON_ATTRS);
        }
        safelist.addAttributes("a", "href", "target", "title");
        safelist.addAttributes("img", "src", "alt", "title", "width", "height", "loading");
        safelist.addAttributes("table", "border", "cellspacing", "cellpadding");
        safelist.addAttributes("td", "colspan", "rowspan", "align", "valign");
        safelist.addAttributes("th", "colspan", "rowspan", "align", "valign", "scope");
        safelist.addAttributes("ol", "start", "type");
        safelist.addAttributes("ul", "type");
        safelist.addProtocols("a", "href", "http", "https", "mailto", "tel");
        safelist.addProtocols("img", "src", "http", "https");
        safelist.preserveRelativeLinks(true);
        safelist.addEnforcedAttribute("a", "rel", "noopener noreferrer");
        return safelist;
    }

    /** 清洗 HTML；空串返回空串。超出白名单的标签剥标签保留文本（jsoup clean 默认行为）。 */
    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document.OutputSettings settings = new Document.OutputSettings();
        String cleaned = Jsoup.clean(html, VALIDATION_BASE, SAFELIST, settings.prettyPrint(false));
        return filterInlineStyles(cleaned);
    }

    /** 后置遍历：inline style 只保留白名单 CSS 属性，剥掉 url(javascript:) / expression() 与未知 property。 */
    private static String filterInlineStyles(String html) {
        Document doc = Jsoup.parseBodyFragment(html);
        for (Element el : doc.getAllElements()) {
            if (!el.hasAttr("style")) {
                continue;
            }
            String filtered = filterStyleValue(el.attr("style"));
            if (filtered.isBlank()) {
                el.removeAttr("style");
            } else {
                el.attr("style", filtered);
            }
        }
        return doc.body().html();
    }

    private static String filterStyleValue(String style) {
        StringBuilder out = new StringBuilder();
        for (String declaration : style.split(";")) {
            int colon = declaration.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String property = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = declaration.substring(colon + 1).trim();
            if (!CSS_PROPERTIES.contains(property)) {
                continue;
            }
            String lower = value.toLowerCase(Locale.ROOT);
            // CSS 转义/注释会掩盖协议；编辑器输出不依赖它们，直接拒绝整条声明。
            if(value.indexOf('\\')>=0||lower.contains("/*")||lower.contains("*/")||lower.contains("@import")){
                continue;
            }
            if (lower.contains("expression(")) {
                continue;
            }
            var urls=CSS_URL.matcher(value);boolean unsafe=false;int count=0;
            while(urls.find()){
                count++;
                try{
                    String url=urls.group(2).trim();
                    if(url.chars().anyMatch(c->c<=32)){unsafe=true;break;}
                    java.net.URI resolved=java.net.URI.create(VALIDATION_BASE).resolve(url);
                    if(!Set.of("http","https").contains(resolved.getScheme().toLowerCase(Locale.ROOT))||resolved.getHost()==null||resolved.getUserInfo()!=null){unsafe=true;break;}
                }catch(Exception e){unsafe=true;break;}
            }
            if(unsafe||(lower.matches("(?s).*url\\s*\\(.*")&&count==0))continue;
            out.append(property).append(":").append(value).append(";");
        }
        return out.toString();
    }

}
