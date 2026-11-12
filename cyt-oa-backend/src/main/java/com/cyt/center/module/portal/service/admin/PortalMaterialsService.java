package com.cyt.center.module.portal.service.admin;

import static com.cyt.center.module.portal.service.admin.PortalRows.*;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Statement;
import java.time.*;
import java.util.*;

/** 样品和办公用品共用 SPU/SKU、站点负责人和借还流水，库存变更与单据同事务。 */
@Service
@RequiredArgsConstructor
public class PortalMaterialsService {
    private final JdbcTemplate jdbc;
    private final CurrentUser user;
    private final PortalAudit audit;
    private final ObjectMapper json;
    private static final ZoneId CN = ZoneId.of("Asia/Shanghai");

    private void manage() {
        user.requirePermission("can_manage_materials");
    }

    private String kind(String value) {
        if (!Set.of("sample", "office").contains(value)) throw new BizException("物资类型无效");
        return value;
    }

    private Map<String, Object> row(String sql, Object... args) {
        var rows = jdbc.queryForList(sql, args);
        if (rows.isEmpty()) throw new BizException("记录不存在");
        return rows.getFirst();
    }

    private long insert(String sql, Object... args) {
        var holder = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var st = c.prepareStatement(sql, new String[]{"id"});
                    for (int i = 0; i < args.length; i++) st.setObject(i + 1, com.cyt.center.core.config.PostgreSqlJdbcConfiguration.databaseValue(args[i]));
                    return st;
                },
                holder);
        return Objects.requireNonNull(holder.getKey()).longValue();
    }

    public Map<String, Object> page(
            String type, String resource, String q, boolean archived, int pageNo, int pageSize) {
        return page(type, resource, q, archived, pageNo, pageSize, new Filters(null,null,null,null,null));
    }

    public record Filters(Long categoryId, String brand, String site, String status, String stockState) {}

    public Map<String, Object> page(
            String type, String resource, String q, boolean archived, int pageNo, int pageSize, Filters filters) {
        manage();
        kind(type);
        if(pageNo<1 || pageNo>100000 || pageSize<1 || pageSize>200)throw new BizException("分页参数无效");
        int size = pageSize, offset = (pageNo - 1) * size;
        String table =
                switch (resource) {
                    case "products" -> "portal_material_products";
                    case "inventory" -> "portal_materials";
                    case "applies" -> "portal_material_applies";
                    default -> throw new BizException("台账类型无效");
                };
        String where =
                resource.equals("applies")
                        ? "m.apply_type=?"
                        : "m.kind=? AND m.archived_at IS " + (archived ? "NOT NULL" : "NULL");
        Object typeValue =
                resource.equals("applies")
                        ? (type.equals("sample") ? "sample_borrow" : "office_receive")
                        : type;
        String search =
                resource.equals("applies")
                        ? "CONCAT_WS(' ',m.apply_no,m.applicant_name,m.purpose)"
                        : "CONCAT_WS(' ',m.name,m.brand)";
        if(q!=null&&q.length()>200)throw new BizException("搜索内容最多200字");
        String term = "%" + (q == null ? "" : q.trim()) + "%";
        var args = new ArrayList<Object>();args.add(typeValue);args.add(term);
        where += " AND " + search + " LIKE ?";
        if(filters.categoryId()!=null){
            if(filters.categoryId()<1)throw new BizException("分类编号无效");
            where += switch(resource){
                case "products" -> " AND m.category_id=?";
                case "inventory" -> " AND EXISTS (SELECT 1 FROM portal_material_products p WHERE p.id=m.product_id AND p.category_id=?)";
                default -> " AND EXISTS (SELECT 1 FROM portal_material_apply_items i JOIN portal_materials s ON s.id=i.material_id JOIN portal_material_products p ON p.id=s.product_id WHERE i.apply_id=m.id AND p.category_id=?)";
            };args.add(filters.categoryId());
        }
        if(filters.brand()!=null&&!filters.brand().isBlank()){
            if(filters.brand().length()>50)throw new BizException("品牌最多50字");
            where += resource.equals("applies") ? " AND EXISTS (SELECT 1 FROM portal_material_apply_items i JOIN portal_materials s ON s.id=i.material_id WHERE i.apply_id=m.id AND s.brand=?)" : " AND m.brand=?";
            args.add(filters.brand().trim());
        }
        if(filters.site()!=null&&!filters.site().isBlank()){
            if(!Set.of("SZ","CD").contains(filters.site()))throw new BizException("站点无效");
            where += switch(resource){
                case "inventory" -> " AND m.site=?";
                case "products" -> " AND EXISTS (SELECT 1 FROM portal_materials s WHERE s.product_id=m.id AND s.site=? AND s.archived_at IS NULL)";
                default -> " AND EXISTS (SELECT 1 FROM portal_material_apply_items i JOIN portal_materials s ON s.id=i.material_id WHERE i.apply_id=m.id AND s.site=?)";
            };args.add(filters.site());
        }
        if(filters.status()!=null&&!filters.status().isBlank()){
            if(!resource.equals("applies")||!Set.of("pending","issued","borrowed","returned","rejected","closed").contains(filters.status()))throw new BizException("单据状态无效");
            where += " AND m.status=?";args.add(filters.status());
        }
        if(filters.stockState()!=null&&!filters.stockState().isBlank()){
            if(!resource.equals("inventory")||!Set.of("available","out").contains(filters.stockState()))throw new BizException("库存状态无效");
            where += filters.stockState().equals("available") ? " AND m.stock_qty>0" : " AND m.stock_qty=0";
        }
        var pageArgs = new ArrayList<>(args);pageArgs.add(size);pageArgs.add(offset);
        var rows =
                jdbc.queryForList(
                        "SELECT m.* FROM "
                                + table
                                + " m WHERE "
                                + where
                                + " ORDER BY m.id DESC LIMIT ? OFFSET ?",
                        pageArgs.toArray());
        if (resource.equals("inventory"))
            for (var r : rows) {
                var owners =
                        jdbc.queryForList(
                                "SELECT owner_username,owner_name FROM portal_material_site_owners"
                                    + " WHERE kind=? AND site=?",
                                type,
                                r.get("site"));
                if (!owners.isEmpty()) r.putAll(owners.getFirst());
            }
        return Map.of(
                "list",
                camel(rows),
                "total",
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM "
                                + table
                                + " m WHERE "
                                + where,
                        Long.class,
                        args.toArray()),
                "summary", summary(type,archived));
    }

    /** 全局概况独立于当前表格筛选，按物资类型及归档范围汇总。 */
    private Map<String,Object> summary(String type,boolean archived){
        String archive=archived?"IS NOT NULL":"IS NULL";
        Long products=jdbc.queryForObject("SELECT COUNT(*) FROM portal_material_products WHERE kind=? AND archived_at "+archive,Long.class,type);
        var stock=jdbc.queryForMap("SELECT COUNT(*) AS \"skuCount\",COALESCE(SUM(stock_qty),0) AS \"stockQty\",COALESCE(SUM(CASE WHEN stock_qty=0 THEN 1 ELSE 0 END),0) AS \"outOfStockCount\" FROM portal_materials WHERE kind=? AND archived_at "+archive,type);
        Long borrowed=type.equals("sample")?jdbc.queryForObject("SELECT COALESCE(SUM(COALESCE(i.approved_qty,i.quantity)-i.returned_quantity),0) FROM portal_material_apply_items i JOIN portal_material_applies a ON a.id=i.apply_id JOIN portal_materials m ON m.id=i.material_id WHERE a.apply_type='sample_borrow' AND a.status='borrowed' AND m.kind=? AND m.archived_at "+archive,Long.class,type):0L;
        return Map.of("productCount",products==null?0:products,"skuCount",stock.getOrDefault("skuCount",0),"stockQty",stock.getOrDefault("stockQty",0),"borrowedQty",borrowed==null?0:borrowed,"outOfStockCount",stock.getOrDefault("outOfStockCount",0));
    }

    public Map<String, Object> options(String type) {
        manage();
        kind(type);
        return Map.of(
                "currentUsername",
                user.require().username(),
                "brands",
                jdbc.queryForList("SELECT brand FROM (SELECT brand FROM portal_material_products WHERE kind=? AND archived_at IS NULL UNION SELECT brand FROM portal_materials WHERE kind=? AND archived_at IS NULL) b WHERE brand IS NOT NULL AND TRIM(brand)<>'' ORDER BY brand",String.class,type,type),
                "users",
                camel(
                        jdbc.queryForList(
                                "SELECT username,name FROM sys_user WHERE enabled=1 ORDER BY"
                                    + " name,id")),
                "owners",
                camel(
                        jdbc.queryForList(
                                "SELECT * FROM portal_material_site_owners WHERE kind=? ORDER BY"
                                    + " site",
                                type)),
                "products",
                camel(
                        jdbc.queryForList(
                                "SELECT id,name,category_id,variant_dims FROM"
                                    + " portal_material_products WHERE kind=? AND archived_at IS"
                                    + " NULL ORDER BY name,id",
                                type)),
                "categories",
                camel(
                        jdbc.queryForList(
                                "SELECT id,name FROM portal_categories WHERE scope=? AND"
                                    + " archived_at IS NULL ORDER BY sort_order,id",
                                "material." + type)));
    }

    public Map<String, Object> detail(long id) {
        manage();
        var apply = camel(row("SELECT * FROM portal_material_applies WHERE id=?", id));
        var items =
                jdbc.queryForList(
                        "SELECT * FROM portal_material_apply_items WHERE apply_id=? ORDER BY"
                            + " material_id,id",
                        id);
        boolean canHandle = true;
        try {
            handler(items);
        } catch (AuthException denied) {
            canHandle = false;
        }
        apply.put("canHandle", canHandle);
        apply.put("items", camel(items));
        apply.put(
                "logs",
                camel(
                        jdbc.queryForList(
                                "SELECT * FROM portal_material_apply_logs WHERE apply_id=? ORDER BY"
                                    + " id DESC",
                                id)));
        return apply;
    }

    @Transactional
    public long saveProduct(Map<String, Object> body) {
        manage();
        String type = kind(text(body, "kind", "sample"));
        String name = text(body, "name", "").trim();
        if (name.isEmpty() || name.length() > 200) throw new BizException("请输入1至200字商品名");
        long category = number(body.get("categoryId"));
        row(
                "SELECT id FROM portal_categories WHERE id=? AND scope=? AND archived_at IS NULL",
                category,
                "material." + type);
        List<String> dims = strings(body.getOrDefault("variantDims", List.of()));
        if (new HashSet<>(dims).size() != dims.size()
                || dims.size() > 8
                || dims.stream().anyMatch(x -> x.isBlank() || x.length() > 32))
            throw new BizException("规格维度不得重复，最多8项、每项32字");
        String brand = text(body, "brand", "").trim();
        if (brand.length() > 50) throw new BizException("品牌名过长");
        Long id = body.get("id") == null ? null : number(body.get("id"));
        if (id == null) {
            id =
                    insert(
                            "INSERT INTO"
                                + " portal_material_products(kind,category_id,name,brand,description,image_urls,variant_dims,created_by)"
                                + " VALUES(?,?,?,?,?,?,?,?)",
                            type,
                            category,
                            name,
                            brand,
                            text(body, "description", ""),
                            json(strings(body.getOrDefault("imageUrls", List.of()))),
                            json(dims),
                            user.require().username());
        } else {
            var old =
                    row(
                            "SELECT * FROM portal_material_products WHERE id=? AND kind=? AND"
                                + " archived_at IS NULL FOR UPDATE",
                            id,
                            type);
            if (!strings(old.get("variant_dims")).equals(dims)
                    && jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM portal_materials WHERE product_id=?",
                                    Long.class,
                                    id)
                            > 0) throw new BizException("已有规格的商品不能更改维度，请新建商品");
            jdbc.update(
                    "UPDATE portal_material_products SET"
                        + " category_id=?,name=?,brand=?,description=?,image_urls=?,variant_dims=?,updated_at=NOW()"
                        + " WHERE id=?",
                    category,
                    name,
                    brand,
                    text(body, "description", ""),
                    json(strings(body.getOrDefault("imageUrls", List.of()))),
                    json(dims),
                    id);
            jdbc.update(
                    "UPDATE portal_materials SET name=?,brand=?,category=(SELECT name FROM"
                        + " portal_categories WHERE id=?),updated_at=NOW() WHERE product_id=?",
                    name,
                    brand,
                    category,
                    id);
        }
        audit.record(
                user.requireUserId(),
                "material_product_save",
                "portal_material_products",
                id,
                null,
                name,
                null);
        return id;
    }

    @Transactional
    public long saveSku(Map<String, Object> body) {
        manage();
        String type = kind(text(body, "kind", "sample"));
        long productId = number(body.get("productId"));
        var product =
                row(
                        "SELECT * FROM portal_material_products WHERE id=? AND kind=? AND"
                            + " archived_at IS NULL FOR UPDATE",
                        productId,
                        type);
        String site = text(body, "site", "SZ");
        if (!Set.of("SZ", "CD").contains(site)) throw new BizException("站点无效");
        var owner =
                row(
                        "SELECT * FROM portal_material_site_owners WHERE site=? AND kind=? FOR"
                            + " UPDATE",
                        site,
                        type);
        Map<String, Object> values =
                body.get("variantValues") instanceof Map<?, ?> map
                        ? new LinkedHashMap<>((Map<String, Object>) map)
                        : Map.of();
        List<String> dims = strings(product.get("variant_dims"));
        if (!values.keySet().equals(new HashSet<>(dims))
                || values.values().stream()
                        .anyMatch(v -> !(v instanceof String s) || s.isBlank() || s.length() > 64))
            throw new BizException("请完整填写商品的规格维度，各维度值须为文字");
        Long id = body.get("id") == null ? null : number(body.get("id"));
        long stock = number(body.getOrDefault("stockQty", 0));
        if (stock < 0) throw new BizException("库存不能为负数");
        if (!values.isEmpty()
                && jdbc.queryForObject(
                                "SELECT COUNT(*) FROM portal_materials WHERE product_id=? AND"
                                    + " site=? AND CAST(variant_values AS jsonb)=CAST(? AS jsonb) AND archived_at"
                                    + " IS NULL AND id<>?",
                                Long.class,
                                productId,
                                site,
                                json(values),
                                id == null ? 0 : id)
                        > 0) throw new BizException("本站点已有相同规格");
        String spec =
                String.join(" / ", dims.stream().map(x -> String.valueOf(values.get(x))).toList());
        if (spec.length() > 64) throw new BizException("规格合计不能超过64字");
        Object price = body.get("unitPrice");
        if (price != null && !price.toString().isBlank())
            try {
                if (new java.math.BigDecimal(price.toString()).signum() < 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                throw new BizException("单价须为非负数字");
            }
        else price = null;
        String category =
                String.valueOf(
                        row(
                                        "SELECT name FROM portal_categories WHERE id=?",
                                        product.get("category_id"))
                                .get("name"));
        if (id == null)
            id =
                    insert(
                            "INSERT INTO"
                                + " portal_materials(kind,product_id,variant_values,site,category,sku,name,brand,spec,unit_price,image_urls,stock_qty,location,owner_username,owner_name,note,created_by)"
                                + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                            type,
                            productId,
                            json(values),
                            site,
                            category,
                            text(body, "sku", ""),
                            product.get("name"),
                            product.get("brand"),
                            spec,
                            price,
                            json(strings(body.getOrDefault("imageUrls", List.of()))),
                            stock,
                            text(body, "location", type.equals("sample") ? "陈列室" : "办公室"),
                            owner.get("owner_username"),
                            owner.get("owner_name"),
                            text(body, "note", ""),
                            user.require().username());
        else {
            var old =
                    row(
                            "SELECT * FROM portal_materials WHERE id=? AND kind=? AND archived_at"
                                + " IS NULL FOR UPDATE",
                            id,
                            type);
            if ((number(old.get("product_id")) != productId || !site.equals(old.get("site")))
                    && jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM portal_material_apply_items i JOIN"
                                        + " portal_material_applies a ON a.id=i.apply_id WHERE"
                                        + " i.material_id=? AND a.status IN ('pending','borrowed')",
                                    Long.class,
                                    id)
                            > 0) throw new BizException("有待处理或在借单据的规格不能改商品和站点");
            jdbc.update(
                    "UPDATE portal_materials SET"
                        + " product_id=?,variant_values=?,site=?,category=?,sku=?,name=?,brand=?,spec=?,unit_price=?,image_urls=?,stock_qty=?,location=?,note=?,updated_at=NOW()"
                        + " WHERE id=?",
                    productId,
                    json(values),
                    site,
                    category,
                    text(body, "sku", ""),
                    product.get("name"),
                    product.get("brand"),
                    spec,
                    price,
                    json(strings(body.getOrDefault("imageUrls", List.of()))),
                    stock,
                    text(body, "location", ""),
                    text(body, "note", ""),
                    id);
            audit.record(
                    user.requireUserId(),
                    "material_stock_adjust",
                    "portal_materials",
                    id,
                    String.valueOf(old.get("stock_qty")),
                    String.valueOf(stock),
                    text(body, "note", ""));
        }
        return id;
    }

    @Transactional
    public void archive(String resource, long id, boolean restore) {
        manage();
        boolean product = "products".equals(resource);
        if (!product && !"inventory".equals(resource)) throw new BizException("台账类型无效");
        if (!product) {
            var parent = row("SELECT product_id FROM portal_materials WHERE id=?", id);
            if (parent.get("product_id") != null)
                row(
                        "SELECT id FROM portal_material_products WHERE id=? FOR UPDATE",
                        parent.get("product_id"));
        }
        String table = product ? "portal_material_products" : "portal_materials";
        var record = row("SELECT * FROM " + table + " WHERE id=? FOR UPDATE", id);
        List<Map<String, Object>> skus =
                product
                        ? jdbc.queryForList(
                                "SELECT * FROM portal_materials WHERE product_id=? ORDER BY id FOR"
                                    + " UPDATE",
                                id)
                        : List.of(record);
        for (var sku : skus)
            if (!restore
                    && jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM portal_material_apply_items i JOIN"
                                        + " portal_material_applies a ON a.id=i.apply_id WHERE"
                                        + " i.material_id=? AND a.status='pending'",
                                    Long.class,
                                    sku.get("id"))
                            > 0) throw new BizException("有待审批单据引用，不能归档");
        if (restore && !product && record.get("product_id") != null) {
            var parent =
                    row(
                            "SELECT archived_at FROM portal_material_products WHERE id=?",
                            record.get("product_id"));
            if (parent.get("archived_at") != null) throw new BizException("请先恢复所属商品");
        }
        Object archivedAt = record.get("archived_at"), group = record.get("archive_group");
        if (restore) {
            if (product) {
                long duplicate =
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM portal_material_products WHERE id<>? AND"
                                    + " kind=? AND archived_at IS NULL AND"
                                    + " LOWER(TRIM(name))=LOWER(TRIM(?)) AND"
                                    + " LOWER(TRIM(COALESCE(brand,'')))=LOWER(TRIM(COALESCE(?,'')))",
                                Long.class,
                                id,
                                record.get("kind"),
                                record.get("name"),
                                record.get("brand"));
                if (duplicate > 0) throw new BizException("已有同名同品牌在役商品，请先调整后恢复");
            }
            for (var sku : skus) {
                if (product && !Objects.equals(group, sku.get("archive_group"))) continue;
                if (sku.get("archived_at") == null) continue;
                long duplicate =
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM portal_materials WHERE id<>? AND product_id=?"
                                    + " AND site=? AND archived_at IS NULL AND"
                                    + " (SELECT COUNT(*) FROM jsonb_object_keys(CAST(variant_values AS jsonb)))>0 AND CAST(variant_values AS jsonb)=CAST(? AS"
                                    + " jsonb)",
                                Long.class,
                                sku.get("id"),
                                sku.get("product_id"),
                                sku.get("site"),
                                sku.get("variant_values"));
                if (duplicate > 0) throw new BizException("已有相同站点和规格的在役库存，请先调整后恢复");
            }
            jdbc.update(
                    "UPDATE "
                            + table
                            + " SET archived_at=NULL,archived_by=NULL,archive_group=NULL,updated_at=NOW()"
                            + " WHERE id=?",
                    id);
            if (product && archivedAt != null) {
                if (group == null) throw new BizException("归档批次尚未迁入，请先执行物资迁移脚本");
                jdbc.update(
                        "UPDATE portal_materials SET"
                            + " archived_at=NULL,archived_by=NULL,archive_group=NULL,updated_at=NOW()"
                            + " WHERE product_id=? AND archive_group=?",
                        id,
                        group);
            }
        } else {
            if (archivedAt != null) throw new BizException("该记录已归档");
            LocalDateTime now = LocalDateTime.now(CN);
            String batch = UUID.randomUUID().toString();
            jdbc.update(
                    "UPDATE "
                            + table
                            + " SET archived_at=?,archived_by=?,archive_group=?,updated_at=NOW()"
                            + " WHERE id=?",
                    now,
                    user.require().username(),
                    batch,
                    id);
            if (product)
                jdbc.update(
                        "UPDATE portal_materials SET"
                            + " archived_at=?,archived_by=?,archive_group=?,updated_at=NOW() WHERE"
                            + " product_id=? AND archived_at IS NULL",
                        now,
                        user.require().username(),
                        batch,
                        id);
        }
        audit.record(
                user.requireUserId(),
                restore ? "material_restore" : "material_archive",
                table,
                id,
                null,
                null,
                null);
    }

    @Transactional
    public void delete(String resource, long id) {
        manage();
        user.requirePermission("can_delete");
        boolean product = "products".equals(resource);
        if (!product && !"inventory".equals(resource)) throw new BizException("台账类型无效");
        String table = product ? "portal_material_products" : "portal_materials";
        var old = row("SELECT * FROM " + table + " WHERE id=? FOR UPDATE", id);
        if (old.get("archived_at") == null) throw new BizException("请先归档再删除");
        long references =
                product
                        ? jdbc.queryForObject(
                                "SELECT COUNT(*) FROM portal_materials WHERE product_id=?",
                                Long.class,
                                id)
                        : jdbc.queryForObject(
                                "SELECT COUNT(*) FROM portal_material_apply_items WHERE"
                                    + " material_id=?",
                                Long.class,
                                id);
        if (references > 0) throw new BizException("仍有规格或历史单据引用，不能删除");
        jdbc.update("DELETE FROM " + table + " WHERE id=?", id);
        audit.record(user.requireUserId(), "material_delete", table, id, json(old), null, null);
    }

    @Transactional
    public void saveOwner(String type, String site, String username) {
        manage();
        user.requirePermission("can_configure_site_owners");
        kind(type);
        if (!Set.of("SZ", "CD").contains(site)) throw new BizException("站点无效");
        var person = row("SELECT name FROM sys_user WHERE username=? AND enabled=1", username);
        jdbc.update(
                "INSERT INTO"
                    + " portal_material_site_owners(site,kind,owner_username,owner_name,updated_by,updated_by_name)"
                    + " VALUES(?,?,?,?,?,?) ON CONFLICT (site,kind) DO UPDATE SET"
                    + " owner_username=EXCLUDED.owner_username,owner_name=EXCLUDED.owner_name,updated_by=EXCLUDED.updated_by,updated_by_name=EXCLUDED.updated_by_name,updated_at=NOW()",
                site,
                type,
                username,
                person.get("name"),
                user.require().username(),
                user.require().name());
        audit.record(
                user.requireUserId(),
                "material_owner",
                "portal_material_site_owners",
                site + "/" + type,
                null,
                username,
                null);
    }

    private void handler(List<Map<String, Object>> items) {
        if (user.require().has("can_approve_any_material_apply")) return;
        if (items.isEmpty()) throw AuthException.forbidden("单据缺少物资明细");
        for (var item : items)
            if (jdbc.queryForObject(
                            "SELECT COUNT(*) FROM portal_materials m JOIN"
                                + " portal_material_site_owners o ON o.site=m.site AND"
                                + " o.kind=m.kind WHERE m.id=? AND o.owner_username=?",
                            Long.class,
                            item.get("material_id"),
                            user.require().username())
                    == 0) throw AuthException.forbidden("仅全部涉及站点的负责人或全站物资审批人可处理该单据");
    }

    @Transactional
    public long createApply(Map<String, Object> body) {
        manage();
        String type = kind(text(body, "kind", "sample"));
        boolean direct = Boolean.TRUE.equals(body.get("directIssue"));
        if (direct && !type.equals("office")) throw new BizException("样品必须通过借用审批");
        String applicant = text(body, "applicantUsername", user.require().username());
        var person =
                row(
                        "SELECT u.name,d.name AS department FROM sys_user u LEFT JOIN sys_dept d ON"
                            + " d.id=u.main_dept_id WHERE u.username=? AND u.enabled=1",
                        applicant);
        String expected = text(body, "expectReturnDate", "");
        if (type.equals("sample")) {
            try {
                if (LocalDate.parse(expected).isBefore(LocalDate.now(CN)))
                    throw new IllegalArgumentException();
            } catch (Exception e) {
                throw new BizException("样品借用须填写不早于今天的归还日期");
            }
        } else if (!expected.isBlank()) throw new BizException("办公领用不填写归还日期");
        var lines = lines(body);
        if (lines.isEmpty() || lines.size() > 100) throw new BizException("请选择1至100条规格");
        Set<Long> ids = new HashSet<>();
        for (var line : lines) {
            if (!ids.add(number(line.get("materialId"))) || number(line.get("quantity")) <= 0)
                throw new BizException("规格不能重复，数量须大于0");
        }
        String applyNo =
                "SQ"
                        + java.time.format.DateTimeFormatter.ofPattern("yyyyMM")
                                .format(LocalDate.now(CN))
                        + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long id =
                insert(
                        "INSERT INTO"
                            + " portal_material_applies(apply_no,apply_type,applicant_username,applicant_name,department,purpose,expect_return_date,remark)"
                            + " VALUES(?,?,?,?,?,?,?,?)",
                        applyNo,
                        type.equals("sample") ? "sample_borrow" : "office_receive",
                        applicant,
                        person.get("name"),
                        person.get("department"),
                        text(body, "purpose", ""),
                        expected.isBlank() ? null : expected,
                        text(body, "remark", ""));
        lines.sort(Comparator.comparingLong(x -> number(x.get("materialId"))));
        String applySite = null;
        for (var line : lines) {
            var sku =
                    row(
                            "SELECT * FROM portal_materials WHERE id=? AND kind=? AND archived_at"
                                + " IS NULL FOR UPDATE",
                            number(line.get("materialId")),
                            type);
            if (applySite == null) applySite = String.valueOf(sku.get("site"));
            else if (!applySite.equals(sku.get("site"))) throw new BizException("不同站点的物资须分开申请");
            insert(
                    "INSERT INTO"
                        + " portal_material_apply_items(apply_id,material_id,material_name,material_sku,spec,quantity)"
                        + " VALUES(?,?,?,?,?,?)",
                    id,
                    sku.get("id"),
                    sku.get("name"),
                    sku.get("sku") == null ? "" : sku.get("sku"),
                    sku.get("spec"),
                    number(line.get("quantity")));
        }
        log(id, "create", null, "pending", text(body, "remark", ""));
        if (direct) action(id, "approve", Map.of("remark", text(body, "remark", "")));
        return id;
    }

    @Transactional
    public void action(long id, String action, Map<String, Object> body) {
        manage();
        var apply = row("SELECT * FROM portal_material_applies WHERE id=? FOR UPDATE", id);
        var items =
                jdbc.queryForList(
                        "SELECT * FROM portal_material_apply_items WHERE apply_id=? ORDER BY"
                            + " material_id,id FOR UPDATE",
                        id);
        handler(items);
        // 同一批规格总按 ID 升序持锁，避免两张单据以不同归还顺序产生死锁。
        for (var item : items)
            row("SELECT id FROM portal_materials WHERE id=? FOR UPDATE", item.get("material_id"));
        String status = String.valueOf(apply.get("status")), next = status;
        boolean sample = "sample_borrow".equals(apply.get("apply_type"));
        String remark = text(body, "remark", "");
        switch (action) {
            case "approve" -> {
                requireStatus(status, "pending");
                var requested = lines(body);
                Map<Long, Long> approvals = new HashMap<>();
                for (var line : requested) {
                    long itemId = number(line.get("id"));
                    if (approvals.put(itemId, number(line.get("quantity"))) != null)
                        throw new BizException("审批明细重复");
                }
                if (approvals.keySet().stream()
                        .anyMatch(key -> items.stream().noneMatch(x -> number(x.get("id")) == key)))
                    throw new BizException("审批明细不存在");
                long total = 0;
                for (var item : items) {
                    long take =
                            approvals.getOrDefault(
                                    number(item.get("id")), number(item.get("quantity")));
                    if (take < 0 || take > number(item.get("quantity")))
                        throw new BizException("审批数量只能减少，不能增加");
                    total += take;
                    var sku =
                            row(
                                    "SELECT * FROM portal_materials WHERE id=? FOR UPDATE",
                                    item.get("material_id"));
                    if (sku.get("archived_at") != null || number(sku.get("stock_qty")) < take)
                        throw new BizException("规格已归档或库存不足：" + sku.get("name"));
                    jdbc.update(
                            "UPDATE portal_material_apply_items SET approved_qty=? WHERE id=?",
                            take,
                            item.get("id"));
                    jdbc.update(
                            "UPDATE portal_materials SET stock_qty=stock_qty-?,updated_at=NOW()"
                                + " WHERE id=?",
                            take,
                            sku.get("id"));
                    if (sample && take > 0) {
                        var borrowers = strings(sku.get("current_borrowers"));
                        borrowers.add(String.valueOf(apply.get("applicant_name")));
                        jdbc.update(
                                "UPDATE portal_materials SET current_borrowers=? WHERE id=?",
                                json(borrowers),
                                sku.get("id"));
                    }
                }
                if (total == 0) throw new BizException("至少批准一件物资；全部拒绝请使用驳回");
                next = sample ? "borrowed" : "issued";
            }
            case "reject" -> {
                requireStatus(status, "pending");
                if (remark.isBlank()) throw new BizException("请填写驳回原因");
                next = "rejected";
            }
            case "return" -> {
                requireStatus(status, "borrowed");
                if (!sample) throw new BizException("办公用品不能走样品归还");
                var returns = lines(body);
                if (returns.isEmpty()) throw new BizException("请输入归还明细");
                Set<Long> seen = new HashSet<>();
                for (var line : returns) {
                    long itemId = number(line.get("id")), qty = number(line.get("quantity"));
                    if (!seen.add(itemId) || qty <= 0) throw new BizException("归还明细重复或数量无效");
                    var item =
                            items.stream()
                                    .filter(x -> number(x.get("id")) == itemId)
                                    .findFirst()
                                    .orElseThrow(() -> new BizException("归还明细不存在"));
                    long
                            cap =
                                    number(
                                            item.get("approved_qty") == null
                                                    ? item.get("quantity")
                                                    : item.get("approved_qty")),
                            returned = number(item.get("returned_quantity")) + qty;
                    if (returned > cap) throw new BizException("归还数量超过在借数量");
                    var sku =
                            row(
                                    "SELECT * FROM portal_materials WHERE id=? FOR UPDATE",
                                    item.get("material_id"));
                    jdbc.update(
                            "UPDATE portal_material_apply_items SET returned_quantity=? WHERE id=?",
                            returned,
                            itemId);
                    item.put("returned_quantity", returned);
                    jdbc.update(
                            "UPDATE portal_materials SET stock_qty=stock_qty+?,updated_at=NOW()"
                                + " WHERE id=?",
                            qty,
                            sku.get("id"));
                    if (returned == cap) {
                        var borrowers = strings(sku.get("current_borrowers"));
                        borrowers.remove(String.valueOf(apply.get("applicant_name")));
                        jdbc.update(
                                "UPDATE portal_materials SET current_borrowers=? WHERE id=?",
                                json(borrowers),
                                sku.get("id"));
                    }
                }
                boolean done =
                        items.stream()
                                .allMatch(
                                        x ->
                                                number(x.get("returned_quantity"))
                                                        == number(
                                                                x.get("approved_qty") == null
                                                                        ? x.get("quantity")
                                                                        : x.get("approved_qty")));
                if (done) {
                    next = "returned";
                    jdbc.update(
                            "UPDATE portal_material_applies SET actual_return_date=? WHERE id=?",
                            LocalDate.now(CN),
                            id);
                }
            }
            case "extend" -> {
                requireStatus(status, "borrowed");
                String date = text(body, "expectReturnDate", "");
                try {
                    LocalDate target = LocalDate.parse(date);
                    if (!target.isAfter(
                            LocalDate.parse(apply.get("expect_return_date").toString())))
                        throw new IllegalArgumentException();
                } catch (Exception e) {
                    throw new BizException("新归还日期须晚于原日期");
                }
                jdbc.update(
                        "UPDATE portal_material_applies SET expect_return_date=? WHERE id=?",
                        date,
                        id);
            }
            case "close" -> {
                if (!Set.of("returned", "issued").contains(status))
                    throw new BizException("仅已归还或已发放的单据可关闭");
                next = "closed";
            }
            default -> throw new BizException("单据操作无效");
        }
        jdbc.update(
                "UPDATE portal_material_applies SET status=?,updated_at=NOW() WHERE id=?",
                next,
                id);
        log(id, action, status, next, remark);
    }

    private void requireStatus(String actual, String expected) {
        if (!expected.equals(actual)) throw new BizException("单据状态已变化，请刷新后操作");
    }

    private void log(long id, String operation, String before, String after, String remark) {
        jdbc.update(
                "INSERT INTO"
                    + " portal_material_apply_logs(apply_id,operation,operator_username,operator_name,before_value,after_value,remark)"
                    + " VALUES(?,?,?,?,?,?,?)",
                id,
                operation,
                user.require().username(),
                user.require().name(),
                before,
                after,
                remark);
    }

    private List<String> strings(Object value) {
        if (value == null) return new ArrayList<>();
        if (value instanceof List<?> list)
            return new ArrayList<>(list.stream().map(String::valueOf).toList());
        try {
            return json.readValue(value.toString(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new BizException("列表格式无效");
        }
    }

    private List<Map<String, Object>> lines(Map<String, Object> body) {
        if (body.get("items") == null) return new ArrayList<>();
        try {
            return new ArrayList<>(
                    json.convertValue(
                            body.get("items"), new TypeReference<List<Map<String, Object>>>() {}));
        } catch (Exception e) {
            throw new BizException("单据明细格式无效");
        }
    }
}
