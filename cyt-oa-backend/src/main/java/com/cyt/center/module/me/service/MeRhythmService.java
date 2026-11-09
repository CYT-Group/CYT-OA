package com.cyt.center.module.me.service;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.me.mapper.RhythmFollowMapper;
import com.cyt.center.module.me.mapper.RhythmNodeMapper;
import com.cyt.center.module.me.model.entity.RhythmFollow;
import com.cyt.center.module.me.model.entity.RhythmNode;
import com.cyt.center.module.me.model.vo.MeVos.DeptAction;
import com.cyt.center.module.me.model.vo.MeVos.RhythmNodeView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 大促 · 新品 · 公司活动的节奏节点。节点内容全公司一致，「关注」是每个人自己的开关。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeRhythmService {

    private final RhythmNodeMapper nodes;
    private final RhythmFollowMapper follows;
    private final ObjectMapper objectMapper;

    /**
     * @param from 起始日，为空取今天前 60 天
     * @param to   结束日，为空取今天后 120 天
     * @param kind sale / new / event，为空不过滤
     */
    public List<RhythmNodeView> list(Long userId, LocalDate from, LocalDate to, String kind) {
        LocalDate today = LocalDate.now();
        LocalDate begin = from == null ? today.minusDays(60) : from;
        LocalDate end = to == null ? today.plusDays(120) : to;
        List<RhythmNode> list = nodes.selectListInRange(begin, end, kind);
        Set<Long> followedIds = followedNodeIds(userId);
        return list.stream()
                .sorted(Comparator.comparing(RhythmNode::getStartDate))
                .map(node -> toView(node, today, followedIds))
                .toList();
    }

    /** 概览横幅：正在进行的节点里最早开始的；没有就取最近的将来节点。 */
    public RhythmNodeView current(Long userId) {
        List<RhythmNodeView> views = list(userId, null, null, null);
        return views.stream().filter(view -> "ongoing".equals(view.state())).findFirst()
                .orElseGet(() -> views.stream().filter(view -> "upcoming".equals(view.state())).findFirst().orElse(null));
    }

    public RhythmNodeView get(Long userId, Long id) {
        RhythmNode node = nodes.selectById(id);
        if (node == null) {
            throw new BizException("节点不存在或已结束归档");
        }
        return toView(node, LocalDate.now(), followedNodeIds(userId));
    }

    public void follow(Long userId, Long nodeId, Integer remindBefore) {
        RhythmNode node = nodes.selectById(nodeId);
        if (node == null) {
            throw new BizException("节点不存在");
        }
        if (follows.selectByUserAndNode(userId, nodeId) != null) {
            return;
        }
        RhythmFollow follow = new RhythmFollow();
        follow.setUserId(userId);
        follow.setNodeId(nodeId);
        follow.setRemindBefore(remindBefore == null ? 3 : Math.max(0, Math.min(remindBefore, 30)));
        follows.insert(follow);
    }

    public void unfollow(Long userId, Long nodeId) {
        follows.deleteByUserAndNode(userId, nodeId);
    }

    /** 关注表按用户取节点号，避免在循环里逐条判断。 */
    public Set<Long> followedNodeIds(Long userId) {
        return follows.selectList(RhythmFollow::getUserId, userId).stream()
                .map(RhythmFollow::getNodeId)
                .collect(Collectors.toSet());
    }

    /** 到期提醒用：从今天起 remindBefore 天内开始的、我关注的节点。 */
    public List<RhythmNode> upcomingFollowedNodes(Long userId, LocalDate today) {
        List<RhythmFollow> mine = follows.selectList(RhythmFollow::getUserId, userId);
        if (mine.isEmpty()) {
            return List.of();
        }
        List<RhythmNode> all = nodes.selectListInRange(today, today.plusDays(60), null);
        List<RhythmNode> hit = new ArrayList<>();
        for (RhythmFollow follow : mine) {
            for (RhythmNode node : all) {
                long days = ChronoUnit.DAYS.between(today, node.getStartDate());
                if (node.getId().equals(follow.getNodeId()) && days >= 0 && days <= follow.getRemindBefore()) {
                    hit.add(node);
                }
            }
        }
        return hit;
    }

    /** daysLeft 恒为「还剩几天」：未开始数到开始日，进行中数到结束日，已结束为负数（N 天前收场）。 */
    private RhythmNodeView toView(RhythmNode node, LocalDate today, Set<Long> followedIds) {
        String state;
        int daysLeft;
        if (today.isBefore(node.getStartDate())) {
            state = "upcoming";
            daysLeft = (int) ChronoUnit.DAYS.between(today, node.getStartDate());
        } else {
            state = today.isAfter(node.getEndDate()) ? "past" : "ongoing";
            daysLeft = (int) ChronoUnit.DAYS.between(today, node.getEndDate());
        }
        return new RhythmNodeView(node.getId(), node.getName(), node.getKind(), kindText(node.getKind()),
                node.getStartDate(), node.getEndDate(), state, daysLeft, node.getNote(),
                parseActions(node), splitRefs(node.getLinkedRefs()), followedIds.contains(node.getId()));
    }

    private List<DeptAction> parseActions(RhythmNode node) {
        String json = node.getDeptActions();
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<DeptAction>>() {
            });
        } catch (Exception ex) {
            // 节点内容来自后台维护，脏数据不该让整个日历页打不开
            log.warn("[parseActions][节点 {} 的作战安排不是合法 JSON]", node.getId(), ex);
            return List.of();
        }
    }

    private List<String> splitRefs(String refs) {
        if (refs == null || refs.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(refs.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String kindText(String kind) {
        return switch (kind == null ? "" : kind) {
            case RhythmNode.KIND_SALE -> "大促";
            case RhythmNode.KIND_NEW -> "新品";
            case RhythmNode.KIND_EVENT -> "公司活动";
            default -> "节点";
        };
    }
}
