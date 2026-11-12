package com.cyt.center.module.portal.dal.postgresql.signoff;

import com.cyt.center.module.portal.domain.signoff.PortalSignoffTargetUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

/** 与旧门户 active + department-array overlap 口径相同：任一部门命中即可进入分母。 */
@Repository
@RequiredArgsConstructor
public class PortalSignoffTargetRepository {
    private final JdbcTemplate jdbc;
    public List<PortalSignoffTargetUser> targets(List<String> scope){
        StringBuilder sql=new StringBuilder("SELECT u.id,u.username,u.name,d.name AS department FROM sys_user u LEFT JOIN sys_dept d ON d.id=u.main_dept_id WHERE u.enabled=1");
        List<Object> args=new ArrayList<>();
        if(scope!=null&&!scope.isEmpty()){
            sql.append(" AND EXISTS (SELECT 1 FROM sys_dept target_dept WHERE target_dept.name IN (").append(String.join(",",Collections.nCopies(scope.size(),"?"))).append(") AND (target_dept.id=u.main_dept_id OR EXISTS (SELECT 1 FROM sys_user_dept membership WHERE membership.user_id=u.id AND membership.dept_id=target_dept.id)))");args.addAll(scope);
        }
        sql.append(" ORDER BY u.id");
        return jdbc.query(sql.toString(),(rs,index)->new PortalSignoffTargetUser(rs.getLong("id"),rs.getString("username"),rs.getString("name"),rs.getString("department")),args.toArray());
    }
}
