-- 零碳公路之旅：游戏化模块基础表结构与题库初始化

CREATE TABLE IF NOT EXISTS green_energy_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    total_energy INT NOT NULL DEFAULT 0,
    current_mileage INT NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL,
    CONSTRAINT uk_green_energy_account_user UNIQUE (user_id),
    CONSTRAINT fk_green_energy_account_user FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE IF NOT EXISTS green_quiz (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question VARCHAR(1000) NOT NULL,
    options TEXT NOT NULL,
    correct_answer VARCHAR(100) NOT NULL,
    energy_reward INT NOT NULL,
    is_default_for_city TINYINT(1) NOT NULL DEFAULT 0
);

-- 初始化环保题库（防重复插入）
INSERT INTO green_quiz (question, options, correct_answer, energy_reward, is_default_for_city)
SELECT '下列哪种驾驶习惯更有助于降低碳排放？', '{"A":"频繁急加速","B":"平稳加速并保持匀速","C":"长时间原地怠速","D":"高速急刹"}', 'B', 10, 1
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE question = '下列哪种驾驶习惯更有助于降低碳排放？');

INSERT INTO green_quiz (question, options, correct_answer, energy_reward, is_default_for_city)
SELECT '车辆胎压长期偏低，通常会导致什么结果？', '{"A":"油耗上升","B":"油耗下降","C":"与油耗无关","D":"仅影响音响效果"}', 'A', 10, 1
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE question = '车辆胎压长期偏低，通常会导致什么结果？');

INSERT INTO green_quiz (question, options, correct_answer, energy_reward, is_default_for_city)
SELECT '以下哪项行为属于绿色出行中的“预防性维护”？', '{"A":"故障后再检修","B":"按周期更换机油和滤芯","C":"长期不做保养","D":"忽略仪表报警"}', 'B', 12, 1
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE question = '以下哪项行为属于绿色出行中的“预防性维护”？');

INSERT INTO green_quiz (question, options, correct_answer, energy_reward, is_default_for_city)
SELECT '如果短暂停车等待（超过1分钟），更推荐的做法是？', '{"A":"保持怠速等待","B":"熄火等待","C":"猛踩油门","D":"开启远光灯"}', 'B', 8, 1
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE question = '如果短暂停车等待（超过1分钟），更推荐的做法是？');

INSERT INTO green_quiz (question, options, correct_answer, energy_reward, is_default_for_city)
SELECT '哪种方式更能减少车辆不必要的能耗？', '{"A":"后备箱长期堆放重物","B":"按需清理多余负载","C":"长期开窗高速行驶","D":"频繁地板油起步"}', 'B', 10, 1
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE question = '哪种方式更能减少车辆不必要的能耗？');

INSERT INTO green_quiz (question, options, correct_answer, energy_reward, is_default_for_city)
SELECT '下列哪种轮胎使用习惯更环保？', '{"A":"花纹磨损严重仍继续高速使用","B":"按里程与磨损情况及时更换","C":"四轮胎压不一致长期行驶","D":"仅更换一个严重磨损轮胎后高速长途"}', 'B', 12, 1
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE question = '下列哪种轮胎使用习惯更环保？');

INSERT INTO green_quiz (question, options, correct_answer, energy_reward, is_default_for_city)
SELECT '合理规划路线（避开严重拥堵）通常会带来什么效果？', '{"A":"增加怠速时间和排放","B":"减少通行时间和油耗","C":"与能耗无关","D":"必然增加维修成本"}', 'B', 10, 1
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE question = '合理规划路线（避开严重拥堵）通常会带来什么效果？');

INSERT INTO green_quiz (question, options, correct_answer, energy_reward, is_default_for_city)
SELECT '对于新能源车辆，以下哪项做法更有利于提升综合效率？', '{"A":"长期满电满放","B":"保持合理充电区间并按建议保养","C":"忽视电池热管理","D":"长时间高负载急加速"}', 'B', 15, 1
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE question = '对于新能源车辆，以下哪项做法更有利于提升综合效率？');
