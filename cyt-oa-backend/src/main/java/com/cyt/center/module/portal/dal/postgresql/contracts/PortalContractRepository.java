package com.cyt.center.module.portal.dal.postgresql.contracts;

import com.cyt.center.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PortalContractRepository {
    private final JdbcTemplate jdbc;
    public List<Map<String,Object>> entities() { return jdbc.queryForList("SELECT id,name,address,legal_rep AS \"legalRep\" FROM portal_contract_entities WHERE is_active=1 ORDER BY id"); }
    public Map<String,Object> get(long id) { return entities().stream().filter(r -> ((Number)r.get("id")).longValue()==id).findFirst().orElseThrow(() -> new BizException("签约主体不存在或已停用")); }
    public long insert(String name,String address,String rep,String actor) {
        GeneratedKeyHolder key=new GeneratedKeyHolder();
        jdbc.update(c -> { var p=c.prepareStatement("INSERT INTO portal_contract_entities(name,address,legal_rep,created_by) VALUES(?,?,?,?)",new String[]{"id"});
            p.setString(1,name);p.setString(2,address);p.setString(3,rep);p.setString(4,actor); return p; },key);
        return key.getKey().longValue();
    }
    public void update(long id,String name,String address,String rep) { get(id); jdbc.update("UPDATE portal_contract_entities SET name=?,address=?,legal_rep=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",name,address,rep,id); }
}
