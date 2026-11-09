package com.cyt.center.framework.common.util.http;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * HTTP 工具类
 *
 * 只保留本项目用到的能力：Content-Disposition 文件名、URL 参数的编解码。
 */
public class HttpUtils {

    /**
     * 编码 URL 参数
     *
     * @param value 参数
     * @return 编码后的参数
     */
    public static String encodeUtf8(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 解码 URL 参数（query parameter）
     * 注意：此方法会将 + 解码为空格，适用于 query parameter，不适用于 URL path
     *
     * @param value 参数
     * @return 解码后的参数
     */
    public static String decodeUtf8(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

}
