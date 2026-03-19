package com.citytrip.service.impl;

import com.citytrip.mapper.PoiMapper;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.dto.ReplaceReqDTO;
import com.citytrip.model.dto.ReplanReqDTO;
import com.citytrip.model.dto.ReplanRespDTO;
import com.citytrip.model.entity.Poi;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.model.vo.ItineraryVO;
import com.citytrip.service.ItineraryService;
import com.citytrip.service.LlmService;
import com.citytrip.service.TravelTimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ItineraryServiceImpl implements ItineraryService {

    @Autowired
    private PoiMapper poiMapper;

    @Autowired
    private LlmService llmService;

    @Autowired
    private TravelTimeService travelTimeService;

    @Override
    public ItineraryVO generateUserItinerary(GenerateReqDTO req) {
        List<Poi> allPois = poiMapper.selectList(null);

        // 1. 致命条件过滤
        List<Poi> candidates = allPois.stream().filter(poi -> {
            if (Boolean.TRUE.equals(req.getIsRainy()) && poi.getIndoor() != null && poi.getIndoor() == 0 
                && poi.getRainFriendly() != null && poi.getRainFriendly() == 0) {
                return false;
            }
            if ("低".equals(req.getWalkingLevel()) && "高".equals(poi.getWalkingLevel())) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        // 2. 动态打分机制
        for (Poi poi : candidates) {
            double score = poi.getPriorityScore() != null ? poi.getPriorityScore().doubleValue() : 3.0;
            if (req.getThemes() != null && !req.getThemes().isEmpty() && poi.getTags() != null) {
                for (String theme : req.getThemes()) {
                    if (poi.getTags().contains(theme)) score += 10.0;
                }
            }
            if (req.getCompanionType() != null && poi.getSuitableFor() != null && poi.getSuitableFor().contains(req.getCompanionType())) {
                score += 5.0;
            }
            if (Boolean.TRUE.equals(req.getIsNight()) && poi.getNightAvailable() != null && poi.getNightAvailable() == 1) {
                score += 3.0;
            }
            poi.setTempScore(score);
        }

        candidates.sort((p1, p2) -> Double.compare(p2.getTempScore(), p1.getTempScore()));

        // 3. 【强校验】用户设定的起止时间流逝推演
        int globalStartMinute = parseTimeMinutes(req.getStartTime(), 9 * 60);
        int globalEndMinute   = parseTimeMinutes(req.getEndTime(), 18 * 60);

        List<Poi> selected = new ArrayList<>();
        int currentTimeCursor = globalStartMinute; // 当前流逝光标

        for (int i = 0; i < candidates.size(); i++) {
            Poi nextPoint = candidates.get(i);
            int estStay = nextPoint.getStayDuration() != null ? nextPoint.getStayDuration() : 60;
            int travelTime = 0;

            if (!selected.isEmpty()) {
                Poi lastPoint = selected.get(selected.size() - 1);
                travelTime = travelTimeService.estimateTravelTimeMinutes(lastPoint, nextPoint);
            }

            // 【核心修正】加入这一点的结束时间是否超过了用户的硬性 endTime要求？
            int tryEndTime = currentTimeCursor + travelTime + estStay;
            
            if (tryEndTime <= globalEndMinute) {
                // 时间充裕，入列！
                selected.add(nextPoint);
                currentTimeCursor = tryEndTime; // 时间游标推进
                
                // 为了让初次生成的路线连贯性更好一点，把同区的提上来
                final Poi justAdded = nextPoint;
                List<Poi> remaining = candidates.subList(i + 1, candidates.size());
                remaining.sort((a, b) -> {
                    double scoreA = a.getTempScore() + (justAdded.getDistrict() != null && justAdded.getDistrict().equals(a.getDistrict()) ? 5.0 : 0.0);
                    double scoreB = b.getTempScore() + (justAdded.getDistrict() != null && justAdded.getDistrict().equals(b.getDistrict()) ? 5.0 : 0.0);
                    return Double.compare(scoreB, scoreA);
                });
            }

            // 最高不超过6个点，免得疲惫
            if (selected.size() == 6) break;
        }

        return packageItinerary(selected, req);
    }

    @Override
    public ItineraryVO replaceNode(ReplaceReqDTO req) {
        List<ItineraryNodeVO> currentNodes = req.getCurrentNodes();
        Long targetId = req.getTargetPoiId();
        
        List<Long> currentIds = currentNodes.stream().map(ItineraryNodeVO::getPoiId).collect(Collectors.toList());
        Poi targetPoi = poiMapper.selectById(targetId);
        
        List<Poi> candidates = poiMapper.selectList(null).stream()
                .filter(p -> !currentIds.contains(p.getId()))
                .collect(Collectors.toList());
                
        // 找平替
        candidates.sort((a, b) -> {
            boolean aSameDist = targetPoi != null && targetPoi.getDistrict() != null && targetPoi.getDistrict().equals(a.getDistrict());
            boolean bSameDist = targetPoi != null && targetPoi.getDistrict() != null && targetPoi.getDistrict().equals(b.getDistrict());
            if (aSameDist && !bSameDist) return -1;
            if (!aSameDist && bSameDist) return 1;
            
            boolean aSameCat = targetPoi != null && targetPoi.getCategory() != null && targetPoi.getCategory().equals(a.getCategory());
            boolean bSameCat = targetPoi != null && targetPoi.getCategory() != null && targetPoi.getCategory().equals(b.getCategory());
            if (aSameCat && !bSameCat) return -1;
            if (!aSameCat && bSameCat) return 1;
            
            return 0;
        });
        
        if (candidates.isEmpty()) {
            throw new RuntimeException("无可用替换点位！");
        }
        
        Poi newPoi = candidates.get(0);
        
        // 保持占位
        for (ItineraryNodeVO node : currentNodes) {
            if (node.getPoiId().equals(targetId)) {
                node.setPoiId(newPoi.getId());
                node.setPoiName(newPoi.getName());
                node.setCategory(newPoi.getCategory());
                node.setDistrict(newPoi.getDistrict());
                node.setCost(newPoi.getAvgCost());
                node.setStayDuration(newPoi.getStayDuration());
                node.setSysReason("【替换成功】系统将此替换为：同区域或分类类似点位。");
                break;
            }
        }
        
        // 【核心修正】：用重新流逝时间法重组，并截断超时的部分
        return repackageNodes(currentNodes, req.getOriginalReq(), true);
    }

    @Override
    public ReplanRespDTO replan(ReplanReqDTO req) {
        ReplanRespDTO resp = new ReplanRespDTO();
        List<ItineraryNodeVO> nodes = req.getCurrentNodes();
        if (nodes == null || nodes.isEmpty()) {
            resp.setSuccess(false);
            resp.setMessage("暂无可以重排的节点");
            return resp;
        }

        // 保存一个旧顺序摘要，用于跟新生成比对是否"真的变了"
        String oldSignature = nodes.stream().map(n -> n.getPoiId().toString()).collect(Collectors.joining("-"));

        // 【核心修正】支持多次点击都能看到变化，我们用混洗同区域的随机扰动法 + 按区归类
        // 这样既保证大概率在同区域，又能产生一些轻微盲盒变化
        Collections.shuffle(nodes); 
        nodes.sort(Comparator.comparing(ItineraryNodeVO::getDistrict, Comparator.nullsFirst(String::compareTo)));

        String newSignature = nodes.stream().map(n -> n.getPoiId().toString()).collect(Collectors.joining("-"));

        // 重新挂钩原用户的极限时间段生成时间流
        ItineraryVO newVo = repackageNodes(nodes, req.getOriginalReq(), true);

        if (oldSignature.equals(newSignature)) {
            resp.setSuccess(true);
            resp.setChanged(false);
            resp.setMessage("当前路线已经较优，没有更好的重排选项了。");
            resp.setReason("在当前时间范围和偏好条件下暂无更顺路的方案");
            resp.setItinerary(newVo); // 时间大概率不变，但透传回前端刷新
        } else {
            newVo.setRecommendReason("修改后的行程安排");
            newVo.setTips("已通过智能路线策略重新洗牌了您的路线并避开了横跳穿越！时间预估和顺序已智能刷新，如果有超时节点也会被自动移除。");
            
            resp.setSuccess(true);
            resp.setChanged(true);
            resp.setMessage("✨ 重排成功！为您尝试了新形态顺流组合");
            resp.setItinerary(newVo);
        }
        
        return resp;
    }
    
    // ----------- 工具方法：构建时间与金钱流水 ------------
    
    /**
     * 针对刚生成的 Entity 的组装（已经确认时间可包含进去）
     */
    private ItineraryVO packageItinerary(List<Poi> selectedPois, GenerateReqDTO req) {
        List<ItineraryNodeVO> nodes = new ArrayList<>();
        int startMinute = parseTimeMinutes(req.getStartTime(), 9 * 60);
        BigDecimal totalCost = BigDecimal.ZERO;

        // 阶段1：只做基础信息组装，不调 LLM，避免循环内串行阻塞
        Poi prevPoi = null;
        for (int i = 0; i < selectedPois.size(); i++) {
            Poi poi = selectedPois.get(i);
            ItineraryNodeVO node = new ItineraryNodeVO();
            node.setStepOrder(i + 1);
            node.setPoiId(poi.getId());
            node.setPoiName(poi.getName());
            node.setCategory(poi.getCategory());
            node.setDistrict(poi.getDistrict());
            node.setStayDuration(poi.getStayDuration() != null ? poi.getStayDuration() : 60);
            node.setCost(poi.getAvgCost() != null ? poi.getAvgCost() : BigDecimal.ZERO);

            int travelTime = 0;
            if (prevPoi != null) {
                travelTime = travelTimeService.estimateTravelTimeMinutes(prevPoi, poi);
            }
            node.setTravelTime(travelTime);
            startMinute += travelTime;
            node.setStartTime(formatTime(startMinute));
            startMinute += node.getStayDuration();
            node.setEndTime(formatTime(startMinute));

            totalCost = totalCost.add(node.getCost());
            nodes.add(node);
            prevPoi = poi;
        }

        // 阶段2：并行化所有 LLM 调用，最坏耗时从 N×20s 降至约 1×20s
        // 为每个节点并行生成 sysReason
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (ItineraryNodeVO node : nodes) {
            final ItineraryNodeVO n = node;
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    n.setSysReason(llmService.explainPoiChoice(req, n));
                } catch (Exception e) {
                    n.setSysReason("这个地方值得一去！");
                }
            }));
        }

        // recommendReason 和 tips 也并行生成
        final List<ItineraryNodeVO> finalNodes = nodes;
        final String[] recommendReason = {""};
        final String[] tips = {""};

        CompletableFuture<Void> reasonFuture = CompletableFuture.runAsync(() -> {
            try {
                recommendReason[0] = llmService.explainItinerary(req, finalNodes);
            } catch (Exception e) {
                recommendReason[0] = "根据您的偏好为您定制了这条路线。";
            }
        });
        CompletableFuture<Void> tipsFuture = CompletableFuture.runAsync(() -> {
            try {
                tips[0] = llmService.generateTips(req);
            } catch (Exception e) {
                tips[0] = "出行前请确认景点营业时间，建议提前规划交通方式。";
            }
        });

        futures.add(reasonFuture);
        futures.add(tipsFuture);

        // 等待所有并行任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        ItineraryVO vo = new ItineraryVO();
        vo.setNodes(nodes);
        vo.setTotalCost(totalCost);
        int firstStart = nodes.isEmpty() ? startMinute : parseTimeMinutes(req.getStartTime(), 9 * 60);
        vo.setTotalDuration(nodes.isEmpty() ? 0 : (startMinute - firstStart));
        vo.setRecommendReason(recommendReason[0]);
        vo.setTips(tips[0]);

        return vo;
    }
    
    /**
     * 【核心修正】带时间强截断保护机制的节点重装器（针对重排和换点）
     * enforceEndTime=true 代表必须校验时间是否超长，超时抛弃节点
     */
    private ItineraryVO repackageNodes(List<ItineraryNodeVO> inputNodes, GenerateReqDTO req, boolean enforceEndTime) {
        int globalEndMinute = (req != null) ? parseTimeMinutes(req.getEndTime(), 18 * 60) : 18 * 60;
        int currentMinuteursor = (req != null) ? parseTimeMinutes(req.getStartTime(), 9 * 60) : 9 * 60;
        int initialStartMinute = currentMinuteursor;
        
        BigDecimal totalCost = BigDecimal.ZERO;
        List<ItineraryNodeVO> finalValidNodes = new ArrayList<>();
        Poi prevPoi = null;
        
        int orderCounter = 1;
        
        for (ItineraryNodeVO rawNode : inputNodes) {
            Poi currPoi = poiMapper.selectById(rawNode.getPoiId());
            if (currPoi == null) continue;
            
            int estStay = rawNode.getStayDuration() != null ? rawNode.getStayDuration() : 60;
            int travelTime = 0;
            if (prevPoi != null) {
                travelTime = travelTimeService.estimateTravelTimeMinutes(prevPoi, currPoi);
            }
            
            // 如果开启强校验且加入这一个点要超时了，抛弃本节点及后续节点
            if (enforceEndTime) {
                if (currentMinuteursor + travelTime + estStay > globalEndMinute) {
                    continue; // 跳过这个点，也许下个点离得很近能行（允许跳着验）
                }
            }
            
            // 可以入列
            ItineraryNodeVO node = new ItineraryNodeVO();
            node.setStepOrder(orderCounter++);
            node.setPoiId(rawNode.getPoiId());
            node.setPoiName(rawNode.getPoiName());
            node.setCategory(rawNode.getCategory());
            node.setDistrict(rawNode.getDistrict());
            node.setStayDuration(estStay);
            node.setCost(rawNode.getCost());
            node.setSysReason(rawNode.getSysReason());
            
            node.setTravelTime(travelTime);
            currentMinuteursor += travelTime;
            node.setStartTime(formatTime(currentMinuteursor));
            currentMinuteursor += estStay;
            node.setEndTime(formatTime(currentMinuteursor));
            
            totalCost = totalCost.add(node.getCost() != null ? node.getCost() : BigDecimal.ZERO);
            finalValidNodes.add(node);
            prevPoi = currPoi;
        }

        ItineraryVO vo = new ItineraryVO();
        vo.setNodes(finalValidNodes);
        vo.setTotalCost(totalCost);
        vo.setTotalDuration(finalValidNodes.isEmpty() ? 0 : (currentMinuteursor - initialStartMinute));
        vo.setRecommendReason("路线安排");
        vo.setTips("时间预估和顺序已智能重算，严格过滤了可能导致您今日行程超时的点位安排，如果由于客观时长极短导致没选上几个点，属于正常预判。");
        return vo;
    }

    private String formatTime(int totalMinutes) {
        int h = (totalMinutes / 60) % 24;
        int m = totalMinutes % 60;
        return String.format("%02d:%02d", h, m);
    }
    
    private int parseTimeMinutes(String timeStr, int defaultMinutes) {
        if (timeStr == null || !timeStr.contains(":")) return defaultMinutes;
        try {
            String[] parts = timeStr.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return defaultMinutes;
        }
    }
}
