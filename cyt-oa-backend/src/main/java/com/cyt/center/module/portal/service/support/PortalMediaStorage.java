package com.cyt.center.module.portal.service.support;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.TRAINING_LESSON_FIELDS_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.TRAINING_NAS_UNAVAILABLE;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.TRAINING_UPLOAD_INVALID;

/**
 * NAS MinIO 适配层 — 培训资料桶（cyt-training）的预签名与目录列举（迁自 Portal 的 services/minio_storage.py）。
 *
 * 所在层：service/support = 基础设施层，不含业务规则与业务 SQL。后端只做两件事——给浏览器签短时效 URL
 * （纯计算）与导入扫描时列举整桶（要能连上 NAS）。
 *
 * 两条硬约束（照搬 Portal）：
 * - 配置一律<b>调用时</b>读 env（凭据轮换后无需重启）；任一项缺失抛 Unavailable → 503「NAS 存储未配置」，
 *   fail-closed 不做假 URL。
 * - 预签名必须离线：minio SDK 不知道 bucket 所在 region 时会先联网查位置（不可达地址上 presign 直接挂死），
 *   所以客户端固定传 region，签名就成了纯本地计算。
 */
@Slf4j
@Component
public class PortalMediaStorage {

    public static final String DEFAULT_BUCKET = "cyt-training";
    public static final int DEFAULT_HEAD_BYTES = 65536;
    public static final String DEFAULT_REGION = "us-east-1";
    public static final int MAX_KEY_BYTES = 500;
    /** 管理端手动上传只放行的两种课时文件；扩展名 → kind 的对应关系在 uploadKey() 校验 */
    public static final Map<String, String> UPLOAD_EXT_BY_KIND = Map.of("video", "mp4", "pdf", "pdf");
    public static final String NOT_CONFIGURED_DETAIL = "NAS 存储未配置";

    /** 一次调用内用到的全部连接参数（从 env 解析，不缓存）。 */
    private record Config(String endpoint, String bucket, String accessKey, String secretKey, String region) {
    }

    private Config loadConfig() {
        String endpoint = env("NAS_MINIO_ENDPOINT");
        String accessKey = env("NAS_MINIO_ACCESS_KEY");
        String secretKey = env("NAS_MINIO_SECRET_KEY");
        String bucket = env("NAS_MINIO_BUCKET");
        String region = env("NAS_MINIO_REGION");
        if (bucket.isEmpty()) {
            bucket = DEFAULT_BUCKET;
        }
        if (region.isEmpty()) {
            region = DEFAULT_REGION;
        }
        if (endpoint.isEmpty() || accessKey.isEmpty() || secretKey.isEmpty()) {
            throw exception(TRAINING_NAS_UNAVAILABLE, NOT_CONFIGURED_DETAIL);
        }
        try {
            URI uri = URI.create(endpoint);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("无协议或主机");
            }
        } catch (IllegalArgumentException e) {
            throw exception(TRAINING_NAS_UNAVAILABLE, NOT_CONFIGURED_DETAIL + "：NAS_MINIO_ENDPOINT 须含协议与主机");
        }
        return new Config(endpoint, bucket, accessKey, secretKey, region);
    }

    private MinioClient client(Config cfg) {
        return MinioClient.builder()
                .endpoint(cfg.endpoint())
                .credentials(cfg.accessKey(), cfg.secretKey())
                .region(cfg.region())
                .build();
    }

    /** 对象键校验（Portal 的 validate_key）：拒绝空、..、以 / 开头、控制字符、超 500 字节。 */
    public String validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw exception(TRAINING_LESSON_FIELDS_INVALID, "对象键不能为空");
        }
        if (key.startsWith("/")) {
            throw exception(TRAINING_LESSON_FIELDS_INVALID, "对象键不能以 / 开头");
        }
        for (String seg : key.split("/")) {
            if ("..".equals(seg)) {
                throw exception(TRAINING_LESSON_FIELDS_INVALID, "对象键不能包含 ..");
            }
        }
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if ((c >= 0x00 && c <= 0x1f) || c == 0x7f) {
                throw exception(TRAINING_LESSON_FIELDS_INVALID, "对象键含非法控制字符");
            }
        }
        if (key.getBytes(StandardCharsets.UTF_8).length > MAX_KEY_BYTES) {
            throw exception(TRAINING_LESSON_FIELDS_INVALID, "对象键超过 " + MAX_KEY_BYTES + " 字节");
        }
        return key;
    }

    /** 签发浏览器直读 URL（GET），默认 1 小时；纯本地计算不联网。 */
    public String presignGet(String key, int expires) {
        Config cfg = loadConfig();
        validateKey(key);
        try {
            return client(cfg).getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(cfg.bucket())
                    .object(key)
                    .expiry(expires)
                    .build());
        } catch (Exception e) {
            throw exception(TRAINING_NAS_UNAVAILABLE, "NAS 存储不可达: " + brief(e));
        }
    }

    /** 签发浏览器直传 URL（PUT），默认 15 分钟；纯本地计算不联网。 */
    public String presignPut(String key, int expires) {
        Config cfg = loadConfig();
        validateKey(key);
        try {
            return client(cfg).getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(cfg.bucket())
                    .object(key)
                    .expiry(expires)
                    .build());
        } catch (Exception e) {
            throw exception(TRAINING_NAS_UNAVAILABLE, "NAS 存储不可达: " + brief(e));
        }
    }

    /** 列举整桶对象 → [(key, size)]；目录占位对象（以 / 结尾）过滤掉。本模块唯一真正出网的函数。 */
    public List<ObjectEntry> listTree() {
        Config cfg = loadConfig();
        try {
            MinioClient client = client(cfg);
            Iterable<Result<Item>> items = client.listObjects(ListObjectsArgs.builder()
                    .bucket(cfg.bucket())
                    .recursive(true)
                    .build());
            List<ObjectEntry> out = new ArrayList<>();
            for (Result<Item> result : items) {
                Item item = result.get();
                String name = item.objectName();
                if (name == null || name.isEmpty() || name.endsWith("/") || item.isDir()) {
                    continue;
                }
                out.add(new ObjectEntry(name, item.size()));
            }
            return out;
        } catch (Exception e) {
            throw exception(TRAINING_NAS_UNAVAILABLE, "NAS 存储不可达: " + brief(e));
        }
    }

    /** Range GET 对象前 n 字节（导入扫描探图片尺寸用）；任何失败返回 null 不抛。 */
    public byte[] readHead(String key, int n) {
        try {
            Config cfg = loadConfig();
            validateKey(key);
            try (GetObjectResponse resp = client(cfg).getObject(GetObjectArgs.builder()
                    .bucket(cfg.bucket())
                    .object(key)
                    .offset(0L)
                    .length((long) n)
                    .build())) {
                return resp.readAllBytes();
            }
        } catch (Exception e) {
            log.warn("读取对象头失败 key={}: {}", key, brief(e));
            return null;
        }
    }

    /** 生成管理端手动上传的新对象键 portal/{course_id}/{uuid12}.{ext}，并按 kind 校验扩展名。 */
    public String uploadKey(Long courseId, String filename, String kind) {
        String extExpected = UPLOAD_EXT_BY_KIND.get(kind);
        if (extExpected == null) {
            throw exception(TRAINING_UPLOAD_INVALID, "上传类型只支持 video / pdf");
        }
        String ext = "";
        if (filename != null && filename.indexOf('.') >= 0) {
            ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }
        if (!extExpected.equals(ext)) {
            throw exception(TRAINING_UPLOAD_INVALID, kind + " 课时只接受 ." + extExpected + " 文件");
        }
        return "portal/" + courseId + "/" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                + "." + extExpected;
    }

    /** 一次列举结果里的一条对象。 */
    public record ObjectEntry(String key, long size) {
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private static String brief(Exception e) {
        String msg = e.getMessage();
        return msg == null ? e.getClass().getSimpleName() : (msg.length() > 120 ? msg.substring(0, 120) : msg);
    }
}
