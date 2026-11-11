package com.cyt.center.module.portal.service.contracts;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.portal.controller.admin.contracts.PortalContractController;
import com.cyt.center.module.portal.dal.mysql.contracts.PortalContractRepository;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortalContractService {
    private final PortalContractRepository repository;
    private final PortalOperatorResolver operators;
    private final PortalAudit audit;
    private final PortalContractTemplate template;

    public Map<String,Object> config() {
        return Map.of("entities",repository.entities(),"defaults",Map.of("reportTo","上级","workCity","成都","workCities",List.of("深圳","成都","东莞"),
                "workSchedule","员工的工作时间每天不超过八（8）小时（不含用餐及休息时间），每周工作五（5）天，周末双休。员工具体的上下班时间为：9:30-11:50；13:20-18:30。公司可根据工作需要调整员工具体的上下班时间。",
                "contractYears",3,"probationMonths",3));
    }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> entity(Long userId,Long id,PortalContractController.Entity r) {
        try {
            if(id==null) id=repository.insert(r.name().trim(),r.address().trim(),r.legalRep().trim(),operators.resolve(userId).username());
            else repository.update(id,r.name().trim(),r.address().trim(),r.legalRep().trim());
        } catch(DuplicateKeyException ex) { throw new BizException("同名签约主体已存在"); }
        audit.record(userId,"contract_entity_save","portal_contract_entities",id,null,r.name(),null);
        return repository.get(id);
    }

    public byte[] generate(Long userId,PortalContractController.Generate r) {
        var d=r.dates();
        if(d.endDate().isBefore(d.startDate()) || d.probationEnd().isBefore(d.startDate()) || d.probationEnd().isAfter(d.endDate()))
            throw new BizException("日期须满足：合同开始日 ≤ 试用期截止日 ≤ 合同结束日");
        Map<String,Object> entity=repository.get(r.companyId());
        var e=r.employee(); var p=r.position(); var s=r.salary();
        Map<String,String> context=new LinkedHashMap<>();
        context.put("sign_date_cn",cn(d.signDate()));context.put("company_name",(String)entity.get("name"));
        context.put("company_address",(String)entity.get("address"));context.put("legal_rep",(String)entity.get("legalRep"));
        context.put("employee_name",e.name().trim());context.put("id_number",e.idNumber().toUpperCase(java.util.Locale.ROOT));
        context.put("phone",e.phone().trim());context.put("home_address",e.homeAddress().trim());context.put("emergency_contact",e.emergencyContact().trim());
        context.put("start_date_cn",cn(d.startDate()));context.put("end_date_cn",cn(d.endDate()));
        context.put("probation_start_cn",cn(d.startDate()));context.put("probation_end_cn",cn(d.probationEnd()));
        context.put("position_title",p.title().trim());context.put("report_to",p.reportTo().trim());context.put("work_city",p.workCity().trim());context.put("work_schedule",p.workSchedule().trim());
        context.put("salary_probation_base",s.probationBase().toString());context.put("salary_probation_allowance",s.probationAllowance().toString());
        context.put("salary_regular_base",s.regularBase().toString());context.put("salary_regular_allowance",s.regularAllowance().toString());context.put("salary_performance",s.performance().toString());
        byte[] bytes=template.render(context,p.duties().stream().map(String::trim).toList());
        // 严禁身份证、电话、住址和薪资进入操作日志。
        audit.record(userId,"contract_generate",null,null,null,e.name()+" / "+p.title()+" / "+entity.get("name"),null);
        return bytes;
    }
    private String cn(LocalDate d) { return d.getYear()+"年"+d.getMonthValue()+"月"+d.getDayOfMonth()+"日"; }
}
