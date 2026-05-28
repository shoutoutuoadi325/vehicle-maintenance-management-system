package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class RuleDiagnosisService {

    private static final double DIRECT_RETURN_THRESHOLD = 0.90;
    private static final String GAP_REGEX = "[\\u4e00-\\u9fa5A-Za-z0-9]{0,8}";

    private final List<RuleDefinition> rules = List.of(
            new RuleDefinition(
                    "制动异常",
                    Pattern.compile("刹车" + GAP_REGEX + "(变软|偏软|失灵|刹不住|距离变长|异响)|制动" + GAP_REGEX + "(异常|失灵|距离变长)"),
                    "制动系统故障",
                    List.of("制动液不足、含水率过高或管路进气", "刹车片/刹车盘磨损或表面异常", "制动总泵、分泵或 ABS 部件异常"),
                    List.of("立即降低车速，避免继续高速行驶，优先确认制动踏板行程和制动液液位。", "检查刹车片厚度、刹车盘沟槽、制动管路泄漏和轮端异常发热。", "读取 ABS/ESP 故障码，必要时进行制动压力测试和管路排气。"),
                    "CRITICAL",
                    800,
                    3000,
                    3,
                    8,
                    0.94
            ),
            new RuleDefinition(
                    "发动机运行异常",
                    Pattern.compile("发动机" + GAP_REGEX + "(抖动|异响|怠速不稳|动力不足)|故障灯" + GAP_REGEX + "(亮|点亮)|动力" + GAP_REGEX + "(不足|下降)"),
                    "发动机运行异常/动力系统异常",
                    List.of("点火系统、火花塞或点火线圈异常", "燃油供给、喷油或燃油压力异常", "进气系统漏气、节气门积碳或传感器信号异常"),
                    List.of("读取 OBD 故障码和冻结帧，记录转速、水温、车速、进气量和燃油修正值。", "按低成本顺序检查火花塞、点火线圈、空气滤清器、节气门和进气漏气。", "如故障灯闪烁、抖动明显或动力显著下降，应限制继续行驶并尽快到店检测。"),
                    "HIGH",
                    800,
                    3000,
                    3,
                    8,
                    0.91
            ),
            new RuleDefinition(
                    "电气/高温风险",
                    Pattern.compile("冒烟|焦糊味|烧焦味|短路|线束" + GAP_REGEX + "(过热|冒烟)|电气" + GAP_REGEX + "(异常|短路)"),
                    "高危电气或高温风险",
                    List.of("线束短路、插头接触不良或保险丝异常发热", "油液滴落到排气等高温部件产生烟雾或异味", "电机、高压部件或发动机舱局部过热"),
                    List.of("立即停车熄火，优先断电并远离高温、燃油和高压部件。", "待车辆降温后检查线束、保险丝盒、插头、油液泄漏点和排气高温区域。", "未确认风险源前不要继续行驶，必要时安排拖车到店检测。"),
                    "CRITICAL",
                    1000,
                    6000,
                    4,
                    12,
                    0.93
            ),
            new RuleDefinition(
                    "冷却系统高温",
                    Pattern.compile("高温|水温" + GAP_REGEX + "(高|报警|异常)|开锅|冷却液" + GAP_REGEX + "(不足|泄漏)|防冻液" + GAP_REGEX + "(不足|泄漏)"),
                    "冷却系统过热故障",
                    List.of("冷却液不足、管路泄漏或水箱散热不良", "节温器、水泵或电子风扇工作异常", "缸垫密封异常导致冷却系统压力异常"),
                    List.of("停车等待降温，禁止热车直接打开水箱盖。", "检查冷却液液位、泄漏点、电子风扇和水箱表面堵塞情况。", "观察上下水管温差，必要时检测节温器、水泵循环和冷却系统压力。"),
                    "CRITICAL",
                    1000,
                    5000,
                    4,
                    10,
                    0.90
            ),
            new RuleDefinition(
                    "启动/熄火异常",
                    Pattern.compile("无法启动|启动困难|打不着火|点不着|熄火|行驶中" + GAP_REGEX + "熄火|怠速" + GAP_REGEX + "熄火|自动" + GAP_REGEX + "熄火"),
                    "启动系统/发动机熄火异常",
                    List.of("蓄电池电量不足、接线柱松动或供电不稳定", "点火线圈、火花塞、燃油泵或燃油压力异常", "曲轴/凸轮轴位置传感器、节气门、怠速控制或相关保险丝/继电器异常"),
                    List.of("先确认熄火发生场景：冷车启动、怠速、低速行驶、急加速或涉水后。", "读取 OBD 故障码和冻结帧，重点关注点火、燃油压力、节气门、曲轴/凸轮轴传感器相关报码。", "检查电瓶电压、搭铁线、火花塞点火状态、燃油泵工作声和油压；若行驶中反复熄火，应暂停继续行驶。"),
                    "HIGH",
                    500,
                    2500,
                    2,
                    6,
                    0.91
            ),
            new RuleDefinition(
                    "空调异常",
                    Pattern.compile("空调" + GAP_REGEX + "(不制冷|不制热|异味|不出风|风小)"),
                    "空调系统异常",
                    List.of("冷媒不足或冷媒压力异常", "压缩机、冷凝器、鼓风机或风门执行器异常", "空调滤芯堵塞或蒸发箱污染"),
                    List.of("检查冷媒压力、压缩机吸合状态和冷凝器散热情况。", "检查鼓风机风量、空调滤芯和出风口温度。", "如伴随异味，检查蒸发箱污染和排水状态。"),
                    "MEDIUM",
                    300,
                    1800,
                    1,
                    4,
                    0.84
            ),
            new RuleDefinition(
                    "油液泄漏",
                    Pattern.compile("漏油|漏液|渗油|汽油味|机油" + GAP_REGEX + "(减少|泄漏)|变速箱油" + GAP_REGEX + "(泄漏|渗漏)"),
                    "油液泄漏/安全风险故障",
                    List.of("发动机油封、油底壳、变速箱或助力系统存在泄漏", "燃油管路、冷却管路或制动管路密封异常", "油液滴落到排气高温部件产生异味"),
                    List.of("优先确认泄漏液体颜色和位置，存在汽油味或大量漏液时停止行驶。", "举升车辆检查底盘、发动机舱和管路接头，清洁后复查渗漏源。", "补足对应油液后做压力或路试检查，确认不再泄漏再交车。"),
                    "CRITICAL",
                    600,
                    4000,
                    2,
                    8,
                    0.89
            )
    );

    public RuleDiagnosisResult diagnose(String problemDescription) {
        String normalized = normalize(problemDescription);
        if (normalized.isBlank()) {
            return RuleDiagnosisResult.noHit();
        }

        RuleDefinition best = null;
        for (RuleDefinition rule : rules) {
            if (rule.matches(normalized) && (best == null || rule.confidence() > best.confidence())) {
                best = rule;
            }
        }

        if (best == null) {
            return RuleDiagnosisResult.noHit();
        }

        AIDiagnosisResponse response = new AIDiagnosisResponse(best.faultType(), buildSuggestion(best));
        response.setPossibleCauses(new ArrayList<>(best.possibleCauses()));
        response.setSeverityLevel(best.severityLevel());
        response.setEstimatedCostMin(best.estimatedCostMin());
        response.setEstimatedCostMax(best.estimatedCostMax());
        response.setEstimatedHoursMin(best.estimatedHoursMin());
        response.setEstimatedHoursMax(best.estimatedHoursMax());

        return new RuleDiagnosisResult(
                true,
                best.confidence() >= DIRECT_RETURN_THRESHOLD,
                best.label(),
                best.confidence(),
                response
        );
    }

    private String normalize(String problemDescription) {
        return problemDescription == null ? "" : problemDescription.trim().toLowerCase(Locale.ROOT);
    }

    private String buildSuggestion(RuleDefinition rule) {
        return "已根据技师端规则库完成初步判断，命中规则：" + rule.label() + "。\n\n"
                + "## 可能原因\n" + toMarkdownList(rule.possibleCauses())
                + "\n## 建议排查\n" + toMarkdownList(rule.steps());
    }

    private String toMarkdownList(List<String> items) {
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            builder.append("- ").append(item).append("\n");
        }
        return builder.toString();
    }

    public record RuleDiagnosisResult(boolean matched,
                                      boolean directReturn,
                                      String ruleHit,
                                      double confidence,
                                      AIDiagnosisResponse response) {
        public static RuleDiagnosisResult noHit() {
            return new RuleDiagnosisResult(false, false, "", 0.0, null);
        }
    }

    private record RuleDefinition(String label,
                                  Pattern pattern,
                                  String faultType,
                                  List<String> possibleCauses,
                                  List<String> steps,
                                  String severityLevel,
                                  Integer estimatedCostMin,
                                  Integer estimatedCostMax,
                                  Integer estimatedHoursMin,
                                  Integer estimatedHoursMax,
                                  double confidence) {
        private boolean matches(String text) {
            return pattern.matcher(text).find();
        }
    }
}
