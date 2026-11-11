package com.cyt.center.module.portal.domain.signoff;

import java.util.*;

/** 历史签收标识候选；不重写历史证据字符串，也不跨版本拼接签收记录。 */
public final class PortalSignoffIdentity {
    private static final Map<String,String> LEGACY=Map.of("handbook","employee-handbook","incentive","hr-incentive");
    private PortalSignoffIdentity(){}
    public static List<String> candidates(Long id,String slug,Collection<String> aliases,String reference){
        LinkedHashSet<String> result=new LinkedHashSet<>();
        if(id!=null)result.add("article:"+id);
        add(result,slug);add(result,reference);
        if(aliases!=null)aliases.forEach(alias->add(result,alias));
        for(var entry:LEGACY.entrySet())if(result.contains(entry.getKey())||result.contains(entry.getValue())){result.add(entry.getKey());result.add(entry.getValue());}
        return new ArrayList<>(result);
    }
    private static void add(Set<String> result,String value){if(value==null||value.isBlank())return;result.add(value);if(value.startsWith("/hr-doc/"))result.add(value.substring("/hr-doc/".length()));}
}
