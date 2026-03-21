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
