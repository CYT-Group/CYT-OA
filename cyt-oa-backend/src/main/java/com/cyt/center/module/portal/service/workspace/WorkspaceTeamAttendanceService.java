package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.portal.service.admin.PortalRows;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.YearMonth;
import java.sql.Timestamp;
import java.util.*;
import static com.cyt.center.module.portal.service.workspace.WorkspaceValidation.*;

@Service
@RequiredArgsConstructor
public class WorkspaceTeamAttendanceService {
    private final WorkspaceIdentity identity;
    private final JdbcTemplate jdbc;

    public Map<String,Object> page(String month,String scope,int pageNo,int pageSize){
        identity.personal();oneOf(scope,"team","company");
        if(scope.equals("company")&&!identity.admin())throw AuthException.forbidden("只有系统管理员可查看全公司考勤");
        if(pageNo<1||pageNo>100000||pageSize<1||pageSize>100)throw new BizException("分页参数无效");
        YearMonth selected;
        try{if(month==null||!month.matches("20\\d{2}-(0[1-9]|1[0-2])"))throw new IllegalArgumentException();selected=YearMonth.parse(month);}
        catch(Exception e){throw new BizException("请选择有效月份，格式为 YYYY-MM");}
        var args=new ArrayList<Object>();
        args.add(Timestamp.valueOf(selected.atDay(1).atStartOfDay()));args.add(Timestamp.valueOf(selected.plusMonths(1).atDay(1).atStartOfDay()));
        String where="a.deleted=0 AND a.attendance_time>=? AND a.attendance_time<?";
        if(scope.equals("team")){
            var members=identity.team();
            if(members.isEmpty()&&!identity.admin())throw AuthException.forbidden("没有团队考勤数据权限");
            where+=members.isEmpty()?" AND 1=0":" AND a.user_id IN ("+String.join(",",Collections.nCopies(members.size(),"?"))+")";args.addAll(members);
        }
        String from=" FROM oa_attendance a LEFT JOIN sys_user u ON u.id=a.user_id LEFT JOIN sys_dept d ON d.id=u.main_dept_id WHERE "+where;
        Long total=jdbc.queryForObject("SELECT COUNT(*)"+from,Long.class,args.toArray());
        var pageArgs=new ArrayList<>(args);pageArgs.add(pageSize);pageArgs.add((pageNo-1)*pageSize);
        var list=PortalRows.camel(jdbc.queryForList("SELECT CAST(a.id AS CHAR) AS id,CAST(a.user_id AS CHAR) AS user_id,u.name,d.name AS dept_name,a.attendance_time,a.type,a.status,a.status AS result,a.remark"+from+" ORDER BY a.attendance_time DESC,a.id DESC LIMIT ? OFFSET ?",pageArgs.toArray()));
        var summary=jdbc.queryForMap("SELECT COUNT(*) AS records,COALESCE(SUM(a.status=1),0) AS normal,COALESCE(SUM(a.status=2),0) AS late,COALESCE(SUM(a.status=3),0) AS early,COALESCE(SUM(a.status=4),0) AS `leave`,COALESCE(SUM(a.status=5),0) AS travel"+from,args.toArray());
        return Map.of("list",list,"total",total==null?0:total,"pageNo",pageNo,"pageSize",pageSize,"summary",summary);
    }
}
