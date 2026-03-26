package org.com.repair.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "green_quiz")
@Schema(description = "环保问答题库")
public class GreenQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Column(nullable = false, length = 1000)
    @Schema(description = "题目内容", example = "以下哪种行为可以减少汽车碳排放？")
    private String question;

    @Column(name = "city_index")
    @Schema(description = "绑定城市节点索引", example = "2")
    private Integer cityIndex;

    @Column(name = "event_title", length = 255)
    @Schema(description = "情景事件标题", example = "遭遇沙尘暴")
    private String eventTitle;

    @Column(name = "event_description", length = 1000)
    @Schema(description = "情景事件描述", example = "前方沙尘暴预警，关于空调滤芯的知识你了解吗？")
    private String eventDescription;

    @Column(name = "event_theme", length = 50)
    @Schema(description = "情景主题样式", example = "sandstorm")
    private String eventTheme;

    @Column(name = "is_default_for_city", nullable = false)
    @Schema(description = "是否为城市默认题", example = "true")
    private Boolean isDefaultForCity;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Schema(description = "选项JSON字符串", example = "{\"A\":\"急加速\",\"B\":\"平稳驾驶\",\"C\":\"长时间怠速\",\"D\":\"频繁急刹\"}")
    private String options;

    @Column(name = "correct_answer", nullable = false, length = 100)
    @Schema(description = "正确答案", example = "B")
    private String correctAnswer;

    @Column(name = "energy_reward", nullable = false)
    @Schema(description = "答对后奖励能量", example = "10")
    private Integer energyReward;
}
