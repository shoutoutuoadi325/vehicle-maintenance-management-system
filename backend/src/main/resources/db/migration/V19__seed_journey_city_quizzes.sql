-- Seed city quizzes for journey nodes and random event quiz pool.
-- Ensures /api/gamification/journey/quiz?cityIndex={n} has available questions.

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 0, 1, '成都绿色启程', '出发前做一题环保知识，完成后可获得启程能量。', 'city',
       '城市道路低速跟车时，哪种驾驶方式更节能？',
       '{"A":"急加速急刹车","B":"平稳加减速并预判路况","C":"长时间怠速等待","D":"频繁并线抢道"}',
       'B', 20
WHERE NOT EXISTS (
    SELECT 1 FROM green_quiz WHERE city_index = 0 AND is_default_for_city = 1
);

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 0, 0, '成都拥堵应对', '晚高峰拥堵场景，选择更低碳的驾驶策略。', 'traffic',
       '在拥堵路段走走停停时，以下哪种行为更能降低油耗与排放？',
       '{"A":"持续深踩油门抢位","B":"保持车距并轻踩油门","C":"长鸣喇叭催行","D":"频繁急刹"}',
       'B', 18
WHERE NOT EXISTS (
    SELECT 1 FROM green_quiz WHERE city_index = 0 AND is_default_for_city = 0 AND event_title = '成都拥堵应对'
);

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 1, 1, '康定高原路段', '进入高原道路前，先检查低碳与安全要点。', 'mountain',
       '高原长坡路段行驶前，哪项检查最有助于安全与节能？',
       '{"A":"仅检查音响","B":"检查胎压与制动系统","C":"关闭所有仪表灯","D":"忽略保养提示"}',
       'B', 22
WHERE NOT EXISTS (
    SELECT 1 FROM green_quiz WHERE city_index = 1 AND is_default_for_city = 1
);

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 1, 0, '康定雨雾天气', '山路雨雾环境，考验你的绿色驾驶习惯。', 'weather',
       '在雨雾山路环境中，哪种操作更合理？',
       '{"A":"紧跟前车减少空隙","B":"保持更长跟车距离并平稳制动","C":"高速超车尽快通过","D":"急刹测试附着力"}',
       'B', 20
WHERE NOT EXISTS (
    SELECT 1 FROM green_quiz WHERE city_index = 1 AND is_default_for_city = 0 AND event_title = '康定雨雾天气'
);

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 2, 1, '理塘高寒路段', '高寒环境续航与能耗管理知识题。', 'cold',
       '寒冷环境下，哪种做法更有助于降低综合能耗？',
       '{"A":"冷车立即急加速","B":"平稳热车并减少无效用电","C":"长期怠速取暖","D":"高档低转硬拖"}',
       'B', 22
WHERE NOT EXISTS (
    SELECT 1 FROM green_quiz WHERE city_index = 2 AND is_default_for_city = 1
);

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 3, 1, '林芝雨林路段', '湿滑路面驾驶行为影响能耗与安全。', 'forest',
       '湿滑路面下，哪种驾驶方式更低碳且安全？',
       '{"A":"急踩刹车短距离停车","B":"提前松油并平顺制动","C":"频繁地板油起步","D":"高速压弯"}',
       'B', 22
WHERE NOT EXISTS (
    SELECT 1 FROM green_quiz WHERE city_index = 3 AND is_default_for_city = 1
);

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 4, 1, '拉萨终点冲刺', '终点前最后一道低碳知识题。', 'final',
       '长途旅程结束后，哪项保养习惯更符合绿色理念？',
       '{"A":"忽略保养周期","B":"按周期更换机油与滤芯","C":"故障灯亮后再说","D":"超期使用磨损件"}',
       'B', 25
WHERE NOT EXISTS (
    SELECT 1 FROM green_quiz WHERE city_index = 4 AND is_default_for_city = 1
);

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT NULL, 0, '路途突发事件：轮胎预警', '路途中遇到胎压告警，选择最优应对。', 'random',
       '收到胎压预警后，优先采取哪项措施？',
       '{"A":"继续高速行驶","B":"尽快靠边检查胎压并低速前往维修点","C":"关闭预警提示","D":"立即急打方向"}',
       'B', 15
WHERE NOT EXISTS (
    SELECT 1 FROM green_quiz WHERE city_index IS NULL AND event_title = '路途突发事件：轮胎预警'
);

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT NULL, 0, '路途突发事件：电量焦虑', '新能源车辆电量下降，考察补能策略。', 'random',
       '当剩余电量偏低时，哪种做法更合理？',
       '{"A":"继续激烈驾驶","B":"就近导航补能并开启节能模式","C":"关闭导航盲开","D":"无视续航提示"}',
       'B', 15
WHERE NOT EXISTS (
    SELECT 1 FROM green_quiz WHERE city_index IS NULL AND event_title = '路途突发事件：电量焦虑'
);
