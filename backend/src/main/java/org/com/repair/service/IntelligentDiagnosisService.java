package org.com.repair.service;

import org.com.repair.DTO.DiagnosisRequest;
import org.com.repair.DTO.DiagnosisResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 智能诊断服务
 * 使用AI技术分析故障描述，提供诊断建议
 */
@Service
public class IntelligentDiagnosisService {
    
    /**
     * 执行智能故障诊断
     * @param request 诊断请求，包含故障描述和车辆信息
     * @return 诊断结果，包含可能的故障原因和建议
     */
    public DiagnosisResponse diagnose(DiagnosisRequest request) {
        String description = request.description().toLowerCase();
        
        // 基于关键词的智能诊断逻辑
        // 实际应用中，这里应该调用真实的AI API（如OpenAI、Claude等）
        
        // 发动机相关问题
        if (containsKeywords(description, "发动机", "引擎", "启动", "熄火", "抖动")) {
            return diagnoseEngine(description, request);
        }
        
        // 刹车系统问题
        if (containsKeywords(description, "刹车", "制动", "刹不住", "异响")) {
            return diagnoseBrake(description, request);
        }
        
        // 电气系统问题
        if (containsKeywords(description, "电", "灯", "电池", "充电", "启动不了", "电瓶")) {
            return diagnoseElectrical(description, request);
        }
        
        // 变速箱问题
        if (containsKeywords(description, "变速", "换挡", "挂挡", "离合")) {
            return diagnoseTransmission(description, request);
        }
        
        // 车身外观问题
        if (containsKeywords(description, "划痕", "凹陷", "碰撞", "掉漆", "生锈")) {
            return diagnoseBodyWork(description, request);
        }
        
        // 空调系统问题
        if (containsKeywords(description, "空调", "制冷", "制热", "温度")) {
            return diagnoseAirConditioning(description, request);
        }
        
        // 轮胎相关问题
        if (containsKeywords(description, "轮胎", "爆胎", "漏气", "磨损")) {
            return diagnoseTire(description, request);
        }
        
        // 默认通用诊断
        return diagnoseGeneral(description, request);
    }
    
    /**
     * 发动机故障诊断
     */
    private DiagnosisResponse diagnoseEngine(String description, DiagnosisRequest request) {
        List<String> recommendedActions = new ArrayList<>();
        String possibleCause;
        String severity;
        Double estimatedCost;
        
        if (containsKeywords(description, "启动不了", "打不着火")) {
            possibleCause = "电池电量不足、启动马达故障或点火系统故障";
            recommendedActions.addAll(Arrays.asList(
                "检查电池电量和接线",
                "检查启动马达工作状况",
                "检查火花塞和点火线圈",
                "检查燃油供应系统"
            ));
            severity = "高";
            estimatedCost = 800.0;
        } else if (containsKeywords(description, "抖动", "怠速不稳")) {
            possibleCause = "节气门积碳、火花塞老化或空气滤清器堵塞";
            recommendedActions.addAll(Arrays.asList(
                "清洗节气门",
                "更换火花塞",
                "清洁或更换空气滤清器",
                "检查点火系统"
            ));
            severity = "中";
            estimatedCost = 500.0;
        } else if (containsKeywords(description, "熄火", "自动熄火")) {
            possibleCause = "燃油泵故障、传感器问题或点火系统故障";
            recommendedActions.addAll(Arrays.asList(
                "检查燃油泵压力",
                "诊断发动机传感器",
                "检查点火系统",
                "清洁喷油嘴"
            ));
            severity = "高";
            estimatedCost = 1200.0;
        } else {
            possibleCause = "发动机系统综合故障，需要详细检测";
            recommendedActions.addAll(Arrays.asList(
                "使用OBD诊断仪读取故障码",
                "全面检查发动机系统",
                "检测气缸压力",
                "检查进排气系统"
            ));
            severity = "中";
            estimatedCost = 1000.0;
        }
        
        return new DiagnosisResponse(
            "发动机故障",
            possibleCause,
            recommendedActions,
            severity,
            estimatedCost,
            "MECHANIC"
        );
    }
    
    /**
     * 刹车系统故障诊断
     */
    private DiagnosisResponse diagnoseBrake(String description, DiagnosisRequest request) {
        List<String> recommendedActions = new ArrayList<>();
        String possibleCause;
        String severity;
        Double estimatedCost;
        
        if (containsKeywords(description, "刹不住", "制动距离长")) {
            possibleCause = "刹车片磨损严重、刹车油不足或制动系统漏油";
            recommendedActions.addAll(Arrays.asList(
                "立即检查刹车片厚度",
                "检查刹车油液位",
                "检查制动管路是否漏油",
                "检测制动力",
                "必要时更换刹车片和刹车盘"
            ));
            severity = "极高";
            estimatedCost = 800.0;
        } else if (containsKeywords(description, "异响", "尖叫", "噪音")) {
            possibleCause = "刹车片磨损或有异物";
            recommendedActions.addAll(Arrays.asList(
                "检查刹车片磨损情况",
                "清理刹车盘表面",
                "检查刹车卡钳",
                "必要时更换刹车片"
            ));
            severity = "中";
            estimatedCost = 600.0;
        } else {
            possibleCause = "刹车系统需要维护检查";
            recommendedActions.addAll(Arrays.asList(
                "全面检查制动系统",
                "检查刹车片和刹车盘",
                "检查刹车油状态",
                "测试制动效能"
            ));
            severity = "中";
            estimatedCost = 500.0;
        }
        
        return new DiagnosisResponse(
            "刹车系统故障",
            possibleCause,
            recommendedActions,
            severity,
            estimatedCost,
            "MECHANIC"
        );
    }
    
    /**
     * 电气系统故障诊断
     */
    private DiagnosisResponse diagnoseElectrical(String description, DiagnosisRequest request) {
        List<String> recommendedActions = new ArrayList<>();
        String possibleCause;
        String severity;
        Double estimatedCost;
        
        if (containsKeywords(description, "电池", "电瓶", "启动不了")) {
            possibleCause = "电池老化或充电系统故障";
            recommendedActions.addAll(Arrays.asList(
                "检测电池电压和容量",
                "检查发电机充电电流",
                "检查电池接线端子",
                "必要时更换电池"
            ));
            severity = "高";
            estimatedCost = 600.0;
        } else if (containsKeywords(description, "灯", "不亮", "闪烁")) {
            possibleCause = "灯泡损坏、保险丝烧断或线路接触不良";
            recommendedActions.addAll(Arrays.asList(
                "检查相关灯泡",
                "检查保险丝",
                "检查线路连接",
                "必要时更换灯泡或修复线路"
            ));
            severity = "低";
            estimatedCost = 200.0;
        } else {
            possibleCause = "电气系统综合问题";
            recommendedActions.addAll(Arrays.asList(
                "使用诊断仪检测电气系统",
                "检查电路保险丝",
                "检查线路连接",
                "检测电池和发电机"
            ));
            severity = "中";
            estimatedCost = 400.0;
        }
        
        return new DiagnosisResponse(
            "电气系统故障",
            possibleCause,
            recommendedActions,
            severity,
            estimatedCost,
            "ELECTRICIAN"
        );
    }
    
    /**
     * 变速箱故障诊断
     */
    private DiagnosisResponse diagnoseTransmission(String description, DiagnosisRequest request) {
        List<String> recommendedActions = new ArrayList<>();
        String possibleCause = "变速箱油液不足、离合器磨损或变速箱内部故障";
        
        recommendedActions.addAll(Arrays.asList(
            "检查变速箱油液位和质量",
            "检测变速箱工作状态",
            "检查离合器片磨损",
            "必要时更换变速箱油",
            "如症状严重，需拆检变速箱"
        ));
        
        return new DiagnosisResponse(
            "变速箱故障",
            possibleCause,
            recommendedActions,
            "高",
            1500.0,
            "MECHANIC"
        );
    }
    
    /**
     * 车身外观故障诊断
     */
    private DiagnosisResponse diagnoseBodyWork(String description, DiagnosisRequest request) {
        List<String> recommendedActions = new ArrayList<>();
        String possibleCause;
        Double estimatedCost;
        
        if (containsKeywords(description, "碰撞", "凹陷")) {
            possibleCause = "车身受到外力撞击造成变形";
            recommendedActions.addAll(Arrays.asList(
                "评估车身损伤程度",
                "钣金修复凹陷部位",
                "重新喷漆恢复外观",
                "检查底盘和悬挂是否受损"
            ));
            estimatedCost = 2000.0;
        } else if (containsKeywords(description, "划痕", "掉漆")) {
            possibleCause = "车身表面漆面受损";
            recommendedActions.addAll(Arrays.asList(
                "清洁受损区域",
                "打磨划痕",
                "补漆处理",
                "抛光护理"
            ));
            estimatedCost = 500.0;
        } else {
            possibleCause = "车身外观需要维护";
            recommendedActions.addAll(Arrays.asList(
                "全面检查车身外观",
                "评估修复方案",
                "进行必要的钣金和喷漆工作"
            ));
            estimatedCost = 1000.0;
        }
        
        return new DiagnosisResponse(
            "车身外观故障",
            possibleCause,
            recommendedActions,
            "低",
            estimatedCost,
            "BODY_WORK"
        );
    }
    
    /**
     * 空调系统故障诊断
     */
    private DiagnosisResponse diagnoseAirConditioning(String description, DiagnosisRequest request) {
        List<String> recommendedActions = new ArrayList<>();
        String possibleCause = "制冷剂不足、压缩机故障或空调滤芯堵塞";
        
        recommendedActions.addAll(Arrays.asList(
            "检查制冷剂压力",
            "检测空调压缩机工作状态",
            "更换空调滤芯",
            "清洁蒸发器和冷凝器",
            "必要时补充制冷剂"
        ));
        
        return new DiagnosisResponse(
            "空调系统故障",
            possibleCause,
            recommendedActions,
            "中",
            600.0,
            "MECHANIC"
        );
    }
    
    /**
     * 轮胎故障诊断
     */
    private DiagnosisResponse diagnoseTire(String description, DiagnosisRequest request) {
        List<String> recommendedActions = new ArrayList<>();
        String possibleCause;
        String severity;
        Double estimatedCost;
        
        if (containsKeywords(description, "爆胎", "漏气")) {
            possibleCause = "轮胎破损或气门嘴漏气";
            recommendedActions.addAll(Arrays.asList(
                "立即停车检查",
                "更换备胎",
                "检查轮胎是否可补",
                "必要时更换新轮胎"
            ));
            severity = "极高";
            estimatedCost = 400.0;
        } else if (containsKeywords(description, "磨损", "花纹")) {
            possibleCause = "轮胎自然磨损或四轮定位不准";
            recommendedActions.addAll(Arrays.asList(
                "检查轮胎磨损程度",
                "检查轮胎花纹深度",
                "进行四轮定位",
                "必要时更换轮胎"
            ));
            severity = "中";
            estimatedCost = 800.0;
        } else {
            possibleCause = "轮胎需要检查维护";
            recommendedActions.addAll(Arrays.asList(
                "检查轮胎气压",
                "检查轮胎磨损",
                "检查轮胎平衡",
                "进行轮胎换位"
            ));
            severity = "低";
            estimatedCost = 200.0;
        }
        
        return new DiagnosisResponse(
            "轮胎故障",
            possibleCause,
            recommendedActions,
            severity,
            estimatedCost,
            "MECHANIC"
        );
    }
    
    /**
     * 通用故障诊断
     */
    private DiagnosisResponse diagnoseGeneral(String description, DiagnosisRequest request) {
        List<String> recommendedActions = Arrays.asList(
            "详细描述故障现象和发生时间",
            "建议到店进行全面检测",
            "使用专业诊断设备进行故障诊断",
            "根据检测结果制定维修方案"
        );
        
        return new DiagnosisResponse(
            "需要详细诊断",
            "故障情况需要进一步专业检测才能确定",
            recommendedActions,
            "中",
            500.0,
            "DIAGNOSTIC"
        );
    }
    
    /**
     * 检查描述中是否包含关键词
     */
    private boolean containsKeywords(String description, String... keywords) {
        for (String keyword : keywords) {
            if (description.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
