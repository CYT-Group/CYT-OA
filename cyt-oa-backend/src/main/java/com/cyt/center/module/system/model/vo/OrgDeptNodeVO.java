package com.cyt.center.module.system.model.vo;

/**
 * 组织架构树节点，一次给全、由前端按 parentId 组装。
 * memberCount 只数直接挂在部门下的人，子部门另算；namePath 是给是人看的「畅韵科技/市场部/…」。
 */
public record OrgDeptNodeVO(
        Long id,
        Long parentId,
        String name,
        String namePath,
        String leaderUserid,
        String leaderName,
        Long memberCount) {
}
