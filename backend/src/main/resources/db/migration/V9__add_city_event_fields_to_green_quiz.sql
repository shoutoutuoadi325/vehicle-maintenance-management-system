-- 为零碳公路题库增加城市情景事件能力
ALTER TABLE green_quiz
    ADD COLUMN city_index INT NULL COMMENT '绑定城市节点索引',
    ADD COLUMN event_title VARCHAR(255) NULL COMMENT '事件卡片标题',
    ADD COLUMN event_description VARCHAR(1000) NULL COMMENT '事件卡片描述',
    ADD COLUMN event_theme VARCHAR(50) NULL COMMENT '事件视觉主题，如 sandstorm/rain/snow/default',
    ADD COLUMN is_default_for_city TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为城市默认题';

CREATE INDEX idx_green_quiz_city_default ON green_quiz(city_index, is_default_for_city);

-- 城市默认专属题：成都
INSERT INTO green_quiz (question, options, correct_answer, energy_reward, city_index, event_title, event_description, event_theme, is_default_for_city)
SELECT
    '早高峰拥堵时，哪种做法更节能减排？',
    '{"A":"频繁变道抢行","B":"保持车距平稳行驶","C":"长时间原地怠速","D":"持续急加速急刹"}',
    'B',
    10,
    0,
    '遭遇早高峰拥堵',
    '成都城区车流缓慢，如何通过驾驶方式减少不必要的油耗与排放？',
    'traffic',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM green_quiz WHERE city_index = 0 AND is_default_for_city = 1
);

-- 城市默认专属题：康定
INSERT INTO green_quiz (question, options, correct_answer, energy_reward, city_index, event_title, event_description, event_theme, is_default_for_city)
SELECT
    '高海拔路段动力下降，哪项保养更关键？',
    '{"A":"忽略空气滤芯","B":"检查进气系统和空滤状态","C":"长期高转速硬撑","D":"减少胎压以提升抓地"}',
    'B',
    12,
    1,
    '翻越高海拔山口',
    '康定路段海拔上升，发动机进气效率受影响，提前维护可降低能耗。',
    'mountain',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM green_quiz WHERE city_index = 1 AND is_default_for_city = 1
);

-- 城市默认专属题：理塘
INSERT INTO green_quiz (question, options, correct_answer, energy_reward, city_index, event_title, event_description, event_theme, is_default_for_city)
SELECT
    '遭遇沙尘天气时，哪项操作更安全且环保？',
    '{"A":"关闭外循环并及时检查空调滤芯","B":"全程大风量外循环","C":"提高车速快速穿越","D":"长按喇叭提醒前车"}',
    'A',
    12,
    2,
    '遭遇沙尘暴',
    '前方沙尘暴预警，关于空调滤芯与车内空气管理的知识你了解吗？',
    'sandstorm',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM green_quiz WHERE city_index = 2 AND is_default_for_city = 1
);

-- 城市默认专属题：林芝
INSERT INTO green_quiz (question, options, correct_answer, energy_reward, city_index, event_title, event_description, event_theme, is_default_for_city)
SELECT
    '连续降雨路段，以下哪项最有助于降低能耗和事故风险？',
    '{"A":"高速通过积水区","B":"提前减速并保持胎纹良好","C":"频繁急刹测试路面","D":"关闭车灯节省电量"}',
    'B',
    10,
    3,
    '突遇强降雨',
    '林芝山区降雨频繁，轮胎与制动状态会直接影响行驶效率与安全。',
    'rain',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM green_quiz WHERE city_index = 3 AND is_default_for_city = 1
);

-- 城市默认专属题：拉萨
INSERT INTO green_quiz (question, options, correct_answer, energy_reward, city_index, event_title, event_description, event_theme, is_default_for_city)
SELECT
    '昼夜温差较大时，哪项用车习惯更能保护电池/油耗表现？',
    '{"A":"冷车立即高负载行驶","B":"预热后平稳起步并关注胎压","C":"长期不开启热管理系统","D":"忽略保养周期"}',
    'B',
    15,
    4,
    '高原寒潮来袭',
    '拉萨夜间气温骤降，合理热管理和轮胎维护能显著改善能效表现。',
    'coldwave',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM green_quiz WHERE city_index = 4 AND is_default_for_city = 1
);
