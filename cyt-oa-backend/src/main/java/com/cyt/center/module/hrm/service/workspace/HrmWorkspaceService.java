package com.cyt.center.module.hrm.service.workspace;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.workspace.HrmWorkspaceController.*;
import com.cyt.center.module.hrm.controller.admin.employee.vo.employee.HrmEmployeeConfirmEntryReqVO;
import com.cyt.center.module.hrm.controller.admin.employee.vo.contract.HrmEmployeeContractSaveReqVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.candidate.HrmRecruitCandidateEntryReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.hrm.service.employee.employment.HrmEmployeeContractService;
import com.cyt.center.module.hrm.service.recruit.candidate.HrmRecruitCandidateService;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.cyt.center.module.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/** 组织与员工只复用 HRM，不建立第二份花名册。文书、合同登记与任职变更在同一事务提交。 */
@Service
@RequiredArgsConstructor
public class HrmWorkspaceService {
    private final JdbcTemplate jdbc;
    private final CurrentUser current;
    private final HrmEmployeeService employees;
    private final HrmRecruitCandidateService candidates;
    private final HrmEmployeeContractService contracts;
    private final SysUserService users;
    private final PortalAudit audit;
    private final HrmDocumentRenderer renderer;

    public List<Map<String,Object>> departments() {
        return jdbc.queryForList("SELECT CAST(id AS TEXT) AS value,name AS label FROM sys_dept WHERE active=1 ORDER BY name,id");
    }

    public Map<String,Object> interviews(int page,int size,String search,Integer result) {
        if(page<1 || page>100000 || size<1 || size>100 || search.length()>100)throw new BizException("分页或搜索条件无效");
        String from=" FROM hrm_recruit_interview i JOIN hrm_recruit_candidate c ON c.id=i.candidate_id AND c.deleted=0 "
                + "LEFT JOIN hrm_recruit_post p ON p.id=c.post_id AND p.deleted=0 "
                + "LEFT JOIN hrm_employee e ON e.id=i.interview_employee_id AND e.deleted=0 WHERE i.deleted=0 ";
        var args=new ArrayList<Object>();
        if(!search.isBlank()) { from+=" AND (c.name LIKE ? OR p.post_name LIKE ?)";args.add("%"+search+"%");args.add("%"+search+"%"); }
        if(result!=null) { from+=" AND i.result=?";args.add(result); }
        long total=jdbc.queryForObject("SELECT COUNT(*)"+from,Long.class,args.toArray());
        args.add(size);args.add((page-1)*size);
        var rows=jdbc.queryForList("SELECT i.id,i.candidate_id AS \"candidateId\",c.name AS \"candidateName\",p.post_name AS \"postName\","
                + "i.stage_number AS \"stageNumber\",i.type,i.interview_time AS \"interviewTime\",i.address,i.result,i.evaluate,i.cancel_reason AS \"cancelReason\","
                + "e.name AS \"interviewEmployeeName\""+from+" ORDER BY i.interview_time DESC NULLS LAST,i.id DESC LIMIT ? OFFSET ?",args.toArray());
        return Map.of("list",rows,"total",total);
    }

    public List<Map<String,Object>> templates() {
        return jdbc.queryForList("SELECT t.id,t.name,t.version,t.kind,t.contract_type AS \"contractType\",t.scope,t.content,t.status,t.review_note AS \"reviewNote\","
                + "t.created_at AS \"createdAt\",t.reviewed_at AS \"reviewedAt\",u.name AS \"creatorName\" FROM hrm_contract_template t "
                + "LEFT JOIN sys_user u ON u.id=t.creator ORDER BY t.id DESC");
    }

    @Transactional
    public long submitTemplate(Template r) {
        renderer.validate(r.content());
        try {
            long id = insert("INSERT INTO hrm_contract_template(name,version,kind,contract_type,scope,content,creator) VALUES(?,?,?,?,?,?,?)",
                    r.name().trim(),r.version().trim(),r.kind(),r.contractType(),r.scope().trim(),r.content(),current.requireUserId());
            audit.record(current.requireUserId(),"hr_template_submit","hrm_contract_template",id,null,null,r.name());
            return id;
        } catch (DuplicateKeyException ex) { throw new BizException("同名同版本模板已存在，请使用新版本号"); }
    }

    @Transactional
    public void review(long id, Review r) {
        if (!current.require().isAdmin()) throw new BizException("仅系统管理员可审核合同模板");
        var t = template(id,false);
        if (!"pending".equals(t.get("status"))) throw new BizException("该模板已审核，请刷新列表");
        if (!r.approved() && (r.note()==null || r.note().isBlank())) throw new BizException("请填写驳回原因");
        jdbc.update("UPDATE hrm_contract_template SET status=?,reviewer=?,review_note=?,reviewed_at=NOW() WHERE id=?",
                r.approved()?"approved":"rejected",current.requireUserId(),r.note(),id);
        audit.record(current.requireUserId(),"hr_template_review","hrm_contract_template",id,null,null,r.approved()?"通过":"驳回");
    }

    private Map<String,Object> template(long id, boolean approved) {
        var rows=jdbc.queryForList("SELECT * FROM hrm_contract_template WHERE id=? FOR UPDATE",id);
        if(rows.isEmpty()) throw new BizException("合同模板不存在");
        var t=rows.getFirst();
        if(approved && !"approved".equals(t.get("status"))) throw new BizException("请先选用审核通过的合同模板");
        return t;
    }

    public String preview(Onboard r) { return renderOnboard(r,template(r.document().templateId(),true)); }

    private String renderOnboard(Onboard r, Map<String,Object> t) {
        if(!"onboard".equals(t.get("kind"))) throw new BizException("请选择入职合同模板");
        var e=r.employee(); var d=r.document();
        if(d.endDate().isBefore(e.getEntryTime().toLocalDate())) throw new BizException("合同结束日不能早于入职日");
        if(d.endDate().isBefore(e.getEntryTime().toLocalDate().plusMonths(Optional.ofNullable(e.getProbation()).orElse(0))))
            throw new BizException("合同期限不能短于试用期");
        Map<String,String> values=new HashMap<>();
        values.put("name",e.getName()); values.put("jobNumber",e.getJobNumber()); values.put("postName",e.getPostName());
        values.put("department",deptName(e.getDeptId())); values.put("entryDate",e.getEntryTime().toLocalDate().toString());
        values.put("probation",String.valueOf(Optional.ofNullable(e.getProbation()).orElse(0))); values.put("salary",d.salary().toPlainString());
        values.put("company",d.company()); values.put("companyCode",d.companyCode()); values.put("companyAddress",d.companyAddress());
        values.put("signDate",d.signDate().toString()); values.put("endDate",d.endDate().toString());
        return renderer.render((String)t.get("content"),values);
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> onboard(Onboard r) {
        var t=template(r.document().templateId(),true);
        String requestHash=fingerprint(r);
        var previous=previous(r.requestKey(),"onboard",requestHash); if(previous!=null) return previous;
        var e=r.employee();
        if(e.getEntryTime().toLocalDate().isAfter(today())) throw new BizException("请在实际入职日确认入职");
        if(!Integer.valueOf(1).equals(e.getEntryStatus())) throw new BizException("确认入职须使用在职状态");
        String content=renderOnboard(r,t);
        long id;
        if(e.getId()!=null) {
            lockEmployee(e.getId());
            var existing=employees.validateEmployeeExists(e.getId());
            if(!Integer.valueOf(2).equals(existing.getEntryStatus())) throw new BizException("仅待入职档案可确认入职，已有档案请先完善任职信息");
            // 身份、部门、岗位及企微维护的入职日期以现有档案为准，防止预览与实际归档不一致。
            if(!Objects.equals(existing.getName(),e.getName()) || !Objects.equals(existing.getDeptId(),e.getDeptId())
                    || !Objects.equals(existing.getPostName(),e.getPostName())
                    || !Objects.equals(existing.getEntryTime(),e.getEntryTime())) throw new BizException("员工资料已变化，请重新选择员工");
            var cmd=BeanUtils.toBean(e,HrmEmployeeConfirmEntryReqVO.class);
            cmd.setUserId(existing.getUserId());
            employees.confirmEmployeeEntry(cmd); id=e.getId();
        } else if(e.getCandidateId()!=null) {
            current.requirePermission("hrm:recruit:candidate:update");
            current.requirePermission("hrm:employee:create");
            id=candidates.convertRecruitCandidateToEmployee(BeanUtils.toBean(e,HrmRecruitCandidateEntryReqVO.class));
        } else {
            current.requirePermission("hrm:employee:create");
            id=employees.createEmployee(e);
        }
        var d=r.document();
        var contract=new HrmEmployeeContractSaveReqVO();
        contract.setEmployeeId(id); contract.setNo("HR-"+r.requestKey()); contract.setType(((Number)t.get("contract_type")).intValue()); contract.setStatus(1);
        contract.setStartTime(e.getEntryTime()); contract.setEndTime(d.endDate().atStartOfDay());
        contract.setSignTime(d.signDate().atStartOfDay()); contract.setSignCompany(d.company());
        contract.setExpireRemind(true); contract.setSort(0); contract.setRemark(t.get("name")+" / "+t.get("version"));
        contracts.createContract(contract);
        long docId=archive(r.requestKey(),requestHash,id,d.templateId(),"onboard",content,e.getEntryTime().toLocalDate());
        return Map.of("employeeId",id,"documentId",docId);
    }

    public String previewOffboard(Offboard r) { return renderOffboard(r,template(r.templateId(),true)); }

    private String renderOffboard(Offboard r,Map<String,Object> t) {
        if(!"offboard".equals(t.get("kind"))) throw new BizException("请选择离职协议模板");
        HrmEmployeeDO e=employees.validateEmployeeExists(r.quit().getEmployeeId());
        if(e.getEntryTime()!=null && r.quit().getPlanQuitTime().isBefore(e.getEntryTime())) throw new BizException("离职日不能早于入职日");
        Map<String,String> v=new HashMap<>();
        v.put("name",e.getName()); v.put("jobNumber",e.getJobNumber()); v.put("department",deptName(e.getDeptId()));
        v.put("postName",e.getPostName()); v.put("company",r.company()); v.put("reason",r.reason());
        v.put("leaveDate",r.quit().getPlanQuitTime().toLocalDate().toString()); v.put("signDate",r.signDate().toString());
        v.put("leaveType",switch(r.quit().getType()) { case 1 -> "主动离职"; case 2 -> "被动离职"; default -> "退休"; });
        return renderer.render((String)t.get("content"),v);
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> offboard(Offboard r) {
        var t=template(r.templateId(),true);
        var previous=previous(r.requestKey(),"offboard",fingerprint(r)); if(previous!=null) return previous;
        long id=r.quit().getEmployeeId(); lockEmployee(id);
        HrmEmployeeDO e=employees.validateEmployeeExists(id);
        if(!Integer.valueOf(1).equals(e.getEntryStatus())) throw new BizException("仅在职员工可办理离职");
        if(e.getUserId()!=null && (e.getUserId().equals(current.requireUserId()) || users.roleCodesOf(e.getUserId()).contains("admin")))
            throw new BizException("不能通过离职办理停用本人或管理员账号，请由其他管理员核对角色后处理");
        String content=renderOffboard(r,t);
        String requestHash=fingerprint(r);
        var cmd=BeanUtils.toBean(r.quit(),com.cyt.center.module.hrm.controller.admin.employee.vo.employee.HrmEmployeeQuitReqVO.class);
        cmd.setRemark(r.reason()); employees.quitEmployee(cmd);
        long doc=archive(r.requestKey(),requestHash,id,r.templateId(),"offboard",content,r.quit().getPlanQuitTime().toLocalDate());
        if(!r.quit().getPlanQuitTime().toLocalDate().isAfter(today())) disableAccount(doc,id,current.requireUserId());
        return Map.of("employeeId",id,"documentId",doc);
    }

    /** 由定时任务在事务内调用；取消离职或再入职后不执行旧文书的停用。 */
    public void disableAccount(long documentId,long employeeId,long actor) {
        lockEmployee(employeeId);
        var e=employees.validateEmployeeExists(employeeId);
        var date=jdbc.queryForObject("SELECT effective_date FROM hrm_employment_document WHERE id=? FOR UPDATE",java.sql.Date.class,documentId);
        if((!Objects.equals(3,e.getEntryStatus()) && !Objects.equals(4,e.getEntryStatus())) || e.getLeaveTime()==null || date==null
                || !date.toLocalDate().equals(e.getLeaveTime().toLocalDate())) {
            jdbc.update("UPDATE hrm_employment_document SET account_disabled=1 WHERE id=?",documentId); return;
        }
        if(e.getUserId()!=null) users.changeStatus(e.getUserId(),false,actor);
        jdbc.update("UPDATE hrm_employment_document SET account_disabled=1 WHERE id=?",documentId);
    }

    public Map<String,Object> document(long id) {
        var list=jdbc.queryForList("SELECT d.id,d.employee_id AS \"employeeId\",d.kind,d.content,d.effective_date AS \"effectiveDate\","
                + "d.created_at AS \"createdAt\",t.name AS \"templateName\",t.version FROM hrm_employment_document d "
                + "JOIN hrm_contract_template t ON t.id=d.template_id WHERE d.id=?",id);
        if(list.isEmpty()) throw new BizException("文书不存在"); return list.getFirst();
    }

    public Map<String,Object> lifecycle(long id) {
        var e=employees.validateEmployeeExists(id);
        var events=new ArrayList<Map<String,Object>>();
        if(e.getEntryTime()!=null) events.add(event("entry","入职登记",e.getEntryTime(),"入职时间以员工档案为准"));
        for(var row:jdbc.queryForList("SELECT id,type,effect_time AS \"eventTime\",applied_time AS \"appliedTime\",remark,"
                + "old_post_name AS \"oldPostName\",new_post_name AS \"newPostName\" FROM hrm_employee_change_record "
                + "WHERE employee_id=? AND deleted=0 ORDER BY effect_time DESC,id DESC",id)) {
            var v=event("change",changeLabel(((Number)row.get("type")).intValue()),row.get("eventTime"),row.get("remark"));
            v.putAll(row); v.put("type","change"); events.add(v);
        }
        for(var row:jdbc.queryForList("SELECT id,archive_time AS \"eventTime\",result_level AS \"resultLevel\",score FROM "
                + "hrm_performance_assessment WHERE employee_id=? AND deleted=0 AND archive_time IS NOT NULL",id)) {
            var v=event("performance","绩效归档",row.get("eventTime"),"等级："+row.get("resultLevel")+" / 得分："+row.get("score")); events.add(v);
        }
        for(var row:jdbc.queryForList("SELECT d.id AS \"documentId\",d.kind,d.created_at AS \"eventTime\",t.name,t.version "
                + "FROM hrm_employment_document d JOIN hrm_contract_template t ON t.id=d.template_id WHERE d.employee_id=?",id)) {
            var v=event("document","文书归档",row.get("eventTime"),row.get("name")+" / "+row.get("version")); v.putAll(row); v.put("type","document"); events.add(v);
        }
        if(e.getLeaveTime()!=null) events.add(event("leave",Integer.valueOf(4).equals(e.getEntryStatus())?"离职生效":"计划离职",e.getLeaveTime(),"任职与账号状态分别记录"));
        events.sort(Comparator.comparing((Map<String,Object> v)->String.valueOf(v.get("eventTime"))).reversed());
        return Map.of("events",events,"contracts",contracts.getContractListByEmployeeId(id));
    }

    public Map<String,String> certificate(long id) {
        var e=employees.validateEmployeeExists(id);
        if(!Integer.valueOf(4).equals(e.getEntryStatus()) || e.getEntryTime()==null || e.getLeaveTime()==null)
            throw new BizException("仅已离职且入离职日期完整的员工可开具证明");
        var list=contracts.getContractListByEmployeeId(id);
        String company=list.stream().map(c->c.getSignCompany()).filter(Objects::nonNull).filter(s->!s.isBlank()).findFirst()
                .orElseThrow(()->new BizException("请先在员工合同中登记签约公司"));
        String content="离职证明\n\n兹证明 "+e.getName()+"（工号："+Optional.ofNullable(e.getJobNumber()).orElse("未登记")+"）自 "
                +e.getEntryTime().toLocalDate()+" 至 "+e.getLeaveTime().toLocalDate()+" 在 "+company+" 任职，岗位："
                +Optional.ofNullable(e.getPostName()).orElse("未登记")+"。该员工已于 "+e.getLeaveTime().toLocalDate()+" 离职。\n\n"
                +"单位（盖章）："+company+"\n开具日期："+today()+"\n";
        audit.record(current.requireUserId(),"hr_certificate","hrm_employee",id,null,null,null);
        return Map.of("content",content);
    }

    private Map<String,Object> event(String type,String title,Object date,Object description) {
        var v=new LinkedHashMap<String,Object>(); v.put("type",type);v.put("title",title);v.put("eventTime",date);v.put("description",description);return v;
    }
    private String changeLabel(int type) { return switch(type) { case 4 -> "转正"; case 5 -> "调岗"; case 6 -> "晋升"; case 7 -> "降级"; case 8 -> "转全职"; case 9 -> "再入职"; case 10 -> "企微组织同步"; default -> "档案维护"; }; }
    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }
    private void lockEmployee(long id) { if(jdbc.queryForList("SELECT id FROM hrm_employee WHERE id=? AND deleted=0 FOR UPDATE",id).isEmpty()) throw new BizException("员工不存在"); }
    private String deptName(Long id) { if(id==null)return ""; var rows=jdbc.queryForList("SELECT name FROM sys_dept WHERE id=?",id);return rows.isEmpty()?"":String.valueOf(rows.getFirst().get("name")); }
    private Map<String,Object> previous(String key,String kind,String hash) {
        var rows=jdbc.queryForList("SELECT employee_id AS \"employeeId\",id AS \"documentId\",kind,creator,request_hash AS \"requestHash\" FROM hrm_employment_document WHERE request_key=?",key);
        if(rows.isEmpty()) return null;
        var row=rows.getFirst();
        if(!kind.equals(row.get("kind")) || ((Number)row.get("creator")).longValue()!=current.requireUserId()) throw new BizException("请求编号已使用，请重新办理");
        if(!hash.equals(row.get("requestHash"))) throw new BizException("该请求已归档其他资料，请刷新后重新办理");
        return Map.of("employeeId",row.get("employeeId"),"documentId",row.get("documentId"));
    }
    private long archive(String key,String hash,long employee,long template,String kind,String content,LocalDate date) {
        long id=insert("INSERT INTO hrm_employment_document(request_key,request_hash,employee_id,template_id,kind,content,effective_date,creator) VALUES(?,?,?,?,?,?,?,?)",
                key,hash,employee,template,kind,content,date,current.requireUserId());
        audit.record(current.requireUserId(),"hr_"+kind,"hrm_employment_document",id,null,null,"员工编号 "+employee);
        return id;
    }
    public static String fingerprint(Object value) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(
                com.cyt.center.framework.common.util.json.JsonUtils.toJsonString(value).getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private long insert(String sql,Object... args) {
        var key=new GeneratedKeyHolder();jdbc.update(c->{var p=c.prepareStatement(sql,new String[]{"id"});for(int i=0;i<args.length;i++)p.setObject(i+1,com.cyt.center.core.config.PostgreSqlJdbcConfiguration.databaseValue(args[i]));return p;},key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }
}
