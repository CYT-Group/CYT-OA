package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.module.portal.service.admin.PortalRows;
import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;

/** 本人记录读取使用登录身份，不能由请求参数指定其他员工。 */
@Service
@RequiredArgsConstructor
public class WorkspacePersonalService {
    private final WorkspaceIdentity identity;
    private final JdbcTemplate jdbc;

    public boolean canApproveMaterials(){
        return identity.admin()||jdbc.queryForObject("SELECT COUNT(*) FROM portal_material_site_owners WHERE owner_username=?",Long.class,identity.principal().username())>0;
    }

    public boolean canApproveRequests(){
        return jdbc.queryForObject("SELECT COUNT(*) FROM portal_workspace_approval_steps s JOIN portal_workspace_records r ON r.id=s.request_id AND r.kind='request' WHERE s.approver_id=? AND s.status='pending' AND r.status='pending'",Long.class,identity.user())>0;
    }

    public List<Map<String,Object>> materialRecords(String kind,boolean approvals){
        identity.personal();
        if(!Set.of("office","sample","all").contains(kind)||(!approvals&&kind.equals("all")))throw new BizException("请选择有效的物资类型");
        if(approvals&&!canApproveMaterials())throw AuthException.forbidden("只有站点负责人可查看物资审批记录");
        boolean allSites=approvals&&identity.admin();
        var args=new ArrayList<Object>();String where="a.apply_type IN ('office_receive','sample_borrow')";
        if(!kind.equals("all")){where+=" AND a.apply_type=?";args.add(kind.equals("office")?"office_receive":"sample_borrow");}
        if(!approvals){where+=" AND a.applicant_username=?";args.add(identity.principal().username());}
        else if(!allSites){where+=" AND EXISTS (SELECT 1 FROM portal_material_apply_items i JOIN portal_materials m ON m.id=i.material_id JOIN portal_material_site_owners o ON o.kind=m.kind AND o.site=m.site WHERE i.apply_id=a.id AND o.owner_username=?)";args.add(identity.principal().username());}
        return jdbc.queryForList("SELECT a.id,a.apply_no,a.apply_type,a.applicant_name,a.department,a.purpose,a.expect_return_date,a.actual_return_date,a.status,a.remark,a.created_at FROM portal_material_applies a WHERE "+where+" ORDER BY a.created_at DESC,a.id DESC",args.toArray()).stream().map(raw->{
            var out=PortalRows.camel(raw);out.put("id",String.valueOf(raw.get("id")));
            var itemArgs=new ArrayList<Object>();itemArgs.add(raw.get("id"));
            String itemWhere="i.apply_id=?";
            if(approvals&&!allSites){itemWhere+=" AND EXISTS (SELECT 1 FROM portal_material_site_owners o WHERE o.kind=m.kind AND o.site=m.site AND o.owner_username=?)";itemArgs.add(identity.principal().username());}
            out.put("items",PortalRows.camel(jdbc.queryForList("SELECT CAST(i.id AS CHAR) AS id,i.material_name,i.material_sku,i.spec,i.quantity,i.approved_qty,i.returned_quantity,m.site FROM portal_material_apply_items i LEFT JOIN portal_materials m ON m.id=i.material_id WHERE "+itemWhere+" ORDER BY i.id",itemArgs.toArray())));
            return out;
        }).toList();
    }

    public List<Map<String,Object>> celebrations(){
        identity.personal();
        return jdbc.queryForList("SELECT id,kind,recipient_name,title,message,years,format,sent_at,token FROM portal_celebration_cards WHERE recipient_username=? AND sent_ok=1 AND sent_at IS NOT NULL AND sent_at<=CURRENT_TIMESTAMP ORDER BY sent_at DESC,id DESC",identity.principal().username()).stream().map(row->{
            var out=PortalRows.camel(row);
            String token=String.valueOf(out.remove("token"));
            String base="/api/portal/celebrations/cards/"+token;
            out.put("id",String.valueOf(row.get("id")));out.put("sentOk",true);
            out.put("url",base+"/page");out.put("imageUrl",base+"/image");
            return out;
        }).toList();
    }
}
