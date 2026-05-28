package org.com.repair.service.green;

/**
 * 工单碳排放评估对应的 S/A/B/C 绿色指数等级。
 */
public enum GreenIndexGrade {
    // S/A/B/C 由低排放到高排放逐级递增，便于后台筛选和前端呈现。
    S("S", "Excellent low-carbon repair"),
    A("A", "Good low-carbon repair"),
    B("B", "Standard repair"),
    C("C", "High-emission repair");

    private final String code;
    private final String description;

    GreenIndexGrade(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 返回等级编码，用于数据库或接口字段。
     */
    public String code() {
        return code;
    }

    /**
     * 返回等级说明，用于说明绿色维修表现。
     */
    public String description() {
        return description;
    }
}
