package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.core.exception.BizException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class WorkspaceValidation {
    private WorkspaceValidation() {}
    public static String text(Map<String,Object> map,String key,int max,boolean required) {
        Object raw=map.get(key);if(raw!=null && !(raw instanceof String)) throw new BizException(key+" 格式不正确");
        String value=raw==null?"":raw.toString().trim();
        if ((required&&value.isEmpty())||value.length()>max) throw new BizException("请填写有效的"+key+"，最多 "+max+" 字");return value;
    }
    public static long id(Object raw) {
        try {long n=new BigDecimal(String.valueOf(raw)).longValueExact();if(n<=0)throw new IllegalArgumentException();return n;}catch(Exception e){throw new BizException("编号格式不正确");}
    }
    public static int version(Map<String,Object> body) {
        Object raw=body.get("version");
        if(raw==null)return 0;
        try {int value=new BigDecimal(String.valueOf(raw)).intValueExact();if(value<0)throw new IllegalArgumentException();return value;}
        catch(Exception e){throw new BizException("版本格式不正确，请刷新后重试");}
    }
    public static double decimal(Object raw,double min,double max,String label) {
        try {double n=Double.parseDouble(String.valueOf(raw));if(!Double.isFinite(n)||n<min||n>max)throw new IllegalArgumentException();return n;}catch(Exception e){throw new BizException(label+"应在 "+min+" 至 "+max+" 之间");}
    }
    public static LocalDate date(Map<String,Object> body,String key) {
        try{return LocalDate.parse(text(body,key,10,true));}catch(Exception e){throw new BizException("请选择有效的"+key);}
    }
    public static LocalDateTime time(Map<String,Object> body,String key) {
        try{return LocalDateTime.parse(text(body,key,30,true));}catch(Exception e){throw new BizException("请选择有效的"+key);}
    }
    public static Map<String,Object> object(Object raw) {
        if(!(raw instanceof Map<?,?> value))throw new BizException("资料必须为对象");
        var out=new LinkedHashMap<String,Object>();value.forEach((k,v)->out.put(String.valueOf(k),v));return out;
    }
    public static List<Map<String,Object>> objects(Object raw,int max,String label) {
        if(!(raw instanceof List<?> list)||list.size()>max)throw new BizException(label+"数量不能超过 "+max);return list.stream().map(WorkspaceValidation::object).toList();
    }
    public static List<Long> ids(Object raw,int max) {
        if(!(raw instanceof List<?> list)||list.size()>max)throw new BizException("请选择有效的人员或部门");return list.stream().map(WorkspaceValidation::id).distinct().toList();
    }
    public static void oneOf(String value,String... allowed) {
        if(!Arrays.asList(allowed).contains(value))throw new BizException("无效的业务类型或状态");
    }
}
