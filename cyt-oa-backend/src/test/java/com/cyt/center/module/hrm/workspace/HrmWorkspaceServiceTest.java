package com.cyt.center.module.hrm.workspace;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.core.security.Principal;
import com.cyt.center.module.hrm.controller.admin.workspace.HrmWorkspaceController.*;
import com.cyt.center.module.hrm.controller.admin.employee.vo.employee.*;
import com.cyt.center.module.hrm.controller.admin.employee.vo.contract.HrmEmployeeContractSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.hrm.service.employee.employment.HrmEmployeeContractService;
import com.cyt.center.module.hrm.service.recruit.candidate.HrmRecruitCandidateService;
import com.cyt.center.module.hrm.service.workspace.*;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.cyt.center.module.system.service.SysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HrmWorkspaceServiceTest {
    JdbcTemplate jdbc=mock(JdbcTemplate.class);
    CurrentUser current=mock(CurrentUser.class);
    HrmEmployeeService employees=mock(HrmEmployeeService.class);
    HrmRecruitCandidateService candidates=mock(HrmRecruitCandidateService.class);
    HrmEmployeeContractService contracts=mock(HrmEmployeeContractService.class);
    SysUserService users=mock(SysUserService.class);
    PortalAudit audit=mock(PortalAudit.class);
    HrmWorkspaceService service=new HrmWorkspaceService(jdbc,current,employees,candidates,contracts,users,audit,new HrmDocumentRenderer());
    LocalDate today=LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
    @BeforeEach void init() {
        when(current.requireUserId()).thenReturn(7L);
        when(current.require()).thenReturn(new Principal(7L,"hr","人事",List.of("hr"),List.of("hrm:employee:update")));
        when(jdbc.queryForList(contains("hrm_contract_template WHERE"),anyLong())).thenReturn(List.of(Map.of("id",1L,"kind","onboard","name","劳动合同","version","V1","status","approved","contract_type",1,"content","姓名：{{name}} / 月薪：{{salary}}")));
        when(jdbc.queryForList(contains("SELECT id FROM hrm_employee"),anyLong())).thenReturn(List.of(Map.of("id",20L)));
        when(jdbc.update(any(PreparedStatementCreator.class),any(KeyHolder.class))).thenAnswer(inv->{ KeyHolder key=inv.getArgument(1);key.getKeyList().add(Map.of("id",99L));return 1; });
    }
    Onboard onboard() {
        var e=new HrmEmployeeSaveReqVO();e.setName("测试员工");e.setJobNumber("TEST001");e.setEntryStatus(1);e.setType(1);e.setStatus(2);e.setProbation(3);e.setEntryTime(today.atStartOfDay());e.setPostName("工程师");
        return new Onboard("test-request-000001",e,new Document(1L,"测试公司","TEST-CODE","测试地址",today,today.plusYears(3),new BigDecimal("12000.00")));
    }
    @Test void archivesContractAndEmployeeTogetherUsingHrmService() {
        when(employees.createEmployee(any())).thenReturn(20L);
        var result=service.onboard(onboard());
        assertEquals(20L,result.get("employeeId"));assertEquals(99L,result.get("documentId"));
        var contract=ArgumentCaptor.forClass(HrmEmployeeContractSaveReqVO.class);verify(contracts).createContract(contract.capture());
        assertEquals(20L,contract.getValue().getEmployeeId());assertEquals(1,contract.getValue().getType());
        verify(jdbc).update(any(PreparedStatementCreator.class),any(KeyHolder.class));
        verify(audit).record(eq(7L),eq("hr_onboard"),anyString(),eq(99L),isNull(),isNull(),eq("员工编号 20"));
    }
    @Test void retryReturnsArchivedResultWithoutCreatingSecondEmployee() {
        when(jdbc.queryForList(contains("WHERE request_key"),anyString())).thenReturn(List.of(Map.of("employeeId",20L,"documentId",99L,"kind","onboard","creator",7L,"requestHash",HrmWorkspaceService.fingerprint(onboard()))));
        assertEquals(20L,service.onboard(onboard()).get("employeeId"));verifyNoInteractions(employees,contracts,candidates);
    }
    @Test void rejectsRequestKeysOwnedByOtherOperators() {
        when(jdbc.queryForList(contains("WHERE request_key"),anyString())).thenReturn(List.of(Map.of("employeeId",20L,"documentId",99L,"kind","onboard","creator",8L)));
        assertThrows(BizException.class,()->service.onboard(onboard()));verifyNoInteractions(employees,contracts);
    }
    @Test void rejectsRetryWhenSignedPayloadChanged() {
        when(jdbc.queryForList(contains("WHERE request_key"),anyString())).thenReturn(List.of(Map.of("employeeId",20L,"documentId",99L,"kind","onboard","creator",7L,"requestHash","other-payload")));
        assertThrows(BizException.class,()->service.onboard(onboard()));verifyNoInteractions(employees,contracts);
    }
    @Test void rejectsFutureEntryAndInvalidContractDateBeforeAnyWrite() {
        var r=onboard();r.employee().setEntryTime(today.plusDays(1).atStartOfDay());
        assertThrows(BizException.class,()->service.onboard(r));verifyNoInteractions(employees,contracts);
        r.employee().setEntryTime(today.atStartOfDay());
        var shortContract=new Onboard(r.requestKey(),r.employee(),new Document(1L,"公司","CODE","地址",today,today.plusDays(1),BigDecimal.ONE));
        assertThrows(BizException.class,()->service.onboard(shortContract));verifyNoInteractions(employees,contracts);
    }
    @Test void stopsBeforeContractArchiveWhenHrmValidationFails() {
        when(employees.createEmployee(any())).thenThrow(new BizException("工号已存在"));
        assertThrows(BizException.class,()->service.onboard(onboard()));verifyNoInteractions(contracts);verify(jdbc,never()).update(any(PreparedStatementCreator.class),any(KeyHolder.class));
    }
    @Test void hrCannotApproveTemplatesEvenIfCalledDirectly() {
        assertThrows(BizException.class,()->service.review(1,new Review(true,"ok")));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void cancelledOrOutdatedExitDocumentCannotDisableRehiredEmployee() {
        var e=new HrmEmployeeDO();e.setId(20L);e.setEntryStatus(1);e.setUserId(30L);
        when(employees.validateEmployeeExists(20L)).thenReturn(e);
        when(jdbc.queryForObject(contains("effective_date"),eq(java.sql.Date.class),eq(99L))).thenReturn(java.sql.Date.valueOf(today));
        service.disableAccount(99,20,7);verifyNoInteractions(users);
        e.setEntryStatus(3);e.setLeaveTime(today.plusDays(30).atStartOfDay());
        service.disableAccount(99,20,7);verifyNoInteractions(users);
    }
    @Test void effectiveExitDisablesLinkedAccount() {
        var e=new HrmEmployeeDO();e.setId(20L);e.setEntryStatus(3);e.setUserId(30L);e.setLeaveTime(today.atStartOfDay());
        when(employees.validateEmployeeExists(20L)).thenReturn(e);
        when(jdbc.queryForObject(contains("effective_date"),eq(java.sql.Date.class),eq(99L))).thenReturn(java.sql.Date.valueOf(today));
        service.disableAccount(99,20,7);verify(users).changeStatus(30L,false,7L);
    }
}
