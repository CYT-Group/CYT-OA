package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import static com.cyt.center.module.portal.service.workspace.WorkspaceValidation.*;

/** 述职与周/月报分开存储；本人提交，直属上级点评。 */
@Service
@RequiredArgsConstructor
public class WorkspaceReviewReportService {
    private static final String KIND="review-report";
    private final PortalWorkspaceStore store;
    private final WorkspaceIdentity identity;

    public List<Map<String,Object>> list(String view){
        identity.personal();oneOf(view,"mine","team");
        List<Long> users=view.equals("mine")?List.of(identity.user()):identity.admin()?identity.allUsers():identity.team();
        if(view.equals("team")&&users.isEmpty())throw AuthException.forbidden("没有团队述职数据权限");
        String where=users.isEmpty()?"1=0":"owner_id IN ("+String.join(",",Collections.nCopies(users.size(),"?"))+")";
        if(view.equals("team"))where+=" AND status='submitted'";
        return store.select(KIND,where,new ArrayList<>(users)).stream().map(this::view).toList();
    }
    public Map<String,Object> get(long id){identity.personal();var row=store.get(KIND,id,false);visible(row);return view(row);}
    private void visible(PortalWorkspaceStore.Row row){
        if(row.ownerId()==identity.user())return;
        if(row.status().equals("submitted")&&(identity.admin()||identity.manages(row.ownerId())))return;
        throw AuthException.forbidden("只能查看本人或直属下属已提交的述职报告");
    }
    @Transactional
    public Map<String,Object> save(Long id,Map<String,Object> body){
        identity.personal();var old=id==null?null:store.get(KIND,id,true);
        if(old!=null&&(old.ownerId()!=identity.user()||!old.status().equals("draft")))throw AuthException.forbidden("只能编辑本人的述职草稿");
        String title=text(body,"title",255,true);var data=validate(body,false);data.put("comments",List.of());long saved;
        if(old==null)saved=store.insert(KIND,null,identity.user(),identity.dept(),null,title,"draft",data);
        else{store.update(old,version(body),title,"draft",data,null);saved=old.id();}
        return view(store.get(KIND,saved,false));
    }
    @Transactional
    public Map<String,Object> submit(long id){
        identity.personal();var row=store.get(KIND,id,true);
        if(row.ownerId()!=identity.user()||!row.status().equals("draft"))throw AuthException.forbidden("只能提交本人的述职草稿");
        validate(row.data(),true);store.update(row,row.version(),row.title(),"submitted",row.data(),null);
        return view(store.get(KIND,id,false));
    }
    @Transactional
    public Map<String,Object> comment(long id,Map<String,Object> body){
        identity.personal();var row=store.get(KIND,id,true);visible(row);
        if(row.ownerId()==identity.user()||!row.status().equals("submitted")||(!identity.admin()&&!identity.manages(row.ownerId())))throw AuthException.forbidden("仅直属上级可点评已提交的述职");
        String note=text(body,"reviewText",2000,true),grade=text(body,"grade",30,false);
        var data=new LinkedHashMap<>(row.data());var comments=new ArrayList<>(objects(data.getOrDefault("comments",List.of()),200,"点评"));
        if(comments.size()>=200)throw new BizException("该述职的点评已达上限");
        comments.add(Map.of("id",UUID.randomUUID().toString(),"reviewerId",String.valueOf(identity.user()),"reviewerName",identity.principal().name(),"reviewText",note,"grade",grade,"createdAt",LocalDateTime.now(ZoneId.of("Asia/Shanghai")).toString()));
        data.put("comments",comments);store.update(row,row.version(),row.title(),row.status(),data,null);
        identity.notify(row.ownerId(),"review-comment",id,"你的述职收到点评："+row.title(),"/me/review-reports?id="+id);
        return view(store.get(KIND,id,false));
    }
    Map<String,Object> view(PortalWorkspaceStore.Row row){
        var out=store.view(row);out.put("ownerName",identity.name(row.ownerId()));
        out.put("editable",row.ownerId()==identity.user()&&row.status().equals("draft"));
        out.put("canComment",row.ownerId()!=identity.user()&&row.status().equals("submitted")&&(identity.admin()||identity.manages(row.ownerId())));return out;
    }
    static Map<String,Object> validate(Map<String,Object> body,boolean submit){
        return new LinkedHashMap<>(Map.of("period",text(body,"period",100,true),"summary",text(body,"summary",4000,submit),"plan",text(body,"plan",4000,false),"problem",text(body,"problem",2000,false)));
    }
}
