SET NAMES utf8mb4;
SET SQL_SAFE_UPDATES = 0;

/*
  零碳旅程题库演示数据

  仅维护 green_quiz 中 event_title 以 COMP- 开头的演示题。
  可重复执行，用于在不重置用户、订单、旅程进度和券包数据的情况下补齐问答演示数据。
*/

START TRANSACTION;

DELETE FROM green_quiz WHERE event_title LIKE 'COMP-%';

UPDATE green_quiz
SET
  event_title = 'COMP-上海启程低碳检查',
  event_description = '出发前完成车辆轻量化与胎压检查，开启本次零碳服务线。',
  event_theme = 'city',
  question = '从上海城区启程前，哪项准备最能降低长途能耗？',
  options = '{"A":"后备箱长期堆满重物","B":"清理多余载物并检查胎压","C":"出发前长时间原地怠速","D":"故意降低胎压提升舒适性"}',
  correct_answer = 'B',
  energy_reward = 20
WHERE city_index = 0 AND is_default_for_city = 1;

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 0, 1, 'COMP-上海启程低碳检查', '出发前完成车辆轻量化与胎压检查，开启本次零碳服务线。', 'city',
       '从上海城区启程前，哪项准备最能降低长途能耗？',
       '{"A":"后备箱长期堆满重物","B":"清理多余载物并检查胎压","C":"出发前长时间原地怠速","D":"故意降低胎压提升舒适性"}',
       'B', 20
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE city_index = 0 AND is_default_for_city = 1);

UPDATE green_quiz
SET
  event_title = 'COMP-湖州服务区补能选择',
  event_description = '抵达湖州服务区，选择更合理的补能与休整策略。',
  event_theme = 'charging',
  question = '在服务区短暂停留时，新能源车辆更推荐哪种补能方式？',
  options = '{"A":"电量很低才临时急充到满","B":"结合路线在合理区间补能并同步休息","C":"关闭热管理系统继续高速行驶","D":"为省时间跳过所有检查"}',
  correct_answer = 'B',
  energy_reward = 22
WHERE city_index = 1 AND is_default_for_city = 1;

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 1, 1, 'COMP-湖州服务区补能选择', '抵达湖州服务区，选择更合理的补能与休整策略。', 'charging',
       '在服务区短暂停留时，新能源车辆更推荐哪种补能方式？',
       '{"A":"电量很低才临时急充到满","B":"结合路线在合理区间补能并同步休息","C":"关闭热管理系统继续高速行驶","D":"为省时间跳过所有检查"}',
       'B', 22
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE city_index = 1 AND is_default_for_city = 1);

UPDATE green_quiz
SET
  event_title = 'COMP-武汉充电港热管理',
  event_description = '长江中游气温较高，考察电池热管理与空调能耗控制。',
  event_theme = 'battery',
  question = '高温天气长途行驶时，哪种做法更有利于电池效率与安全？',
  options = '{"A":"忽视电池温控提示","B":"保持热管理正常工作并避免连续激烈驾驶","C":"长期满载急加速","D":"关闭所有冷却功能省电"}',
  correct_answer = 'B',
  energy_reward = 22
WHERE city_index = 2 AND is_default_for_city = 1;

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 2, 1, 'COMP-武汉充电港热管理', '长江中游气温较高，考察电池热管理与空调能耗控制。', 'battery',
       '高温天气长途行驶时，哪种做法更有利于电池效率与安全？',
       '{"A":"忽视电池温控提示","B":"保持热管理正常工作并避免连续激烈驾驶","C":"长期满载急加速","D":"关闭所有冷却功能省电"}',
       'B', 22
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE city_index = 2 AND is_default_for_city = 1);

UPDATE green_quiz
SET
  event_title = 'COMP-成都低碳站山路策略',
  event_description = '进入西部山地路段前，完成制动、胎压与能量回收策略判断。',
  event_theme = 'mountain',
  question = '进入连续坡道前，哪项操作更符合安全低碳驾驶？',
  options = '{"A":"下坡全程空挡滑行","B":"提前检查制动并合理使用能量回收","C":"频繁急刹测试制动","D":"关闭仪表告警避免干扰"}',
  correct_answer = 'B',
  energy_reward = 24
WHERE city_index = 3 AND is_default_for_city = 1;

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 3, 1, 'COMP-成都低碳站山路策略', '进入西部山地路段前，完成制动、胎压与能量回收策略判断。', 'mountain',
       '进入连续坡道前，哪项操作更符合安全低碳驾驶？',
       '{"A":"下坡全程空挡滑行","B":"提前检查制动并合理使用能量回收","C":"频繁急刹测试制动","D":"关闭仪表告警避免干扰"}',
       'B', 24
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE city_index = 3 AND is_default_for_city = 1);

UPDATE green_quiz
SET
  event_title = 'COMP-拉萨终点绿色保养',
  event_description = '终点前完成最后一道绿色养护题，衔接大奖与权益闭环。',
  event_theme = 'final',
  question = '长途旅程结束后，哪项养护动作最符合绿色维修理念？',
  options = '{"A":"忽略异常噪声继续用车","B":"按周期检查轮胎、制动和滤芯状态","C":"等故障扩大后一次性维修","D":"只清洗外观不检查关键系统"}',
  correct_answer = 'B',
  energy_reward = 28
WHERE city_index = 4 AND is_default_for_city = 1;

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 4, 1, 'COMP-拉萨终点绿色保养', '终点前完成最后一道绿色养护题，衔接大奖与权益闭环。', 'final',
       '长途旅程结束后，哪项养护动作最符合绿色维修理念？',
       '{"A":"忽略异常噪声继续用车","B":"按周期检查轮胎、制动和滤芯状态","C":"等故障扩大后一次性维修","D":"只清洗外观不检查关键系统"}',
       'B', 28
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE city_index = 4 AND is_default_for_city = 1);

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
VALUES
  (0, 0, 'COMP-上海早高峰能耗挑战', '城市早高峰车流密集，选择更稳的起步和跟车方式。', 'traffic',
   '拥堵路段走走停停时，哪种驾驶方式更节能？',
   '{"A":"频繁地板油抢位","B":"保持车距并平顺加减速","C":"长时间原地轰油门","D":"急刹后立刻急加速"}',
   'B', 18),
  (1, 0, 'COMP-湖州胎压巡检', '服务区休整时发现胎压偏低，判断正确处理方式。', 'tire',
   '胎压长期偏低通常会带来什么影响？',
   '{"A":"滚阻增大、能耗上升","B":"一定降低能耗","C":"只影响车漆颜色","D":"完全不影响轮胎寿命"}',
   'A', 18),
  (2, 0, 'COMP-武汉雨天制动回收', '雨天经过武汉城区，考察制动能量回收与安全距离。', 'rain',
   '湿滑道路上使用能量回收时，更合理的做法是？',
   '{"A":"突然切到最高回收并紧跟前车","B":"结合路况平顺减速并保持安全距离","C":"关闭车灯节省电量","D":"高速压过积水区"}',
   'B', 20),
  (3, 0, 'COMP-成都拥堵绕行', '进入成都低碳站前遇到拥堵，选择路线规划策略。', 'route',
   '合理避开严重拥堵通常会带来什么效果？',
   '{"A":"减少怠速与频繁启停","B":"必然增加碳排放","C":"完全不影响能耗","D":"一定增加维修成本"}',
   'A', 20),
  (4, 0, 'COMP-拉萨高原温差', '终点附近昼夜温差大，关注胎压和热管理。', 'cold',
   '昼夜温差较大时，哪种用车习惯更稳妥？',
   '{"A":"冷车立即高负载行驶","B":"关注胎压变化并平稳起步","C":"故意超载提高续航","D":"忽略热管理告警"}',
   'B', 22),
  (NULL, 0, 'COMP-路途突发：空调能耗', '夏季服务区外温较高，选择更低碳的空调使用方式。', 'random',
   '高温行车时，哪种空调使用方式更兼顾舒适与节能？',
   '{"A":"长期开窗高速行驶","B":"先通风再合理设定空调温度","C":"空调温度越低越省电","D":"停车长时间怠速开空调"}',
   'B', 15),
  (NULL, 0, 'COMP-路途突发：制动异响', '长下坡后出现轻微制动异响，需要判断处理方式。', 'random',
   '发现制动异响后，哪种处理更安全也更绿色？',
   '{"A":"继续高速行驶直到完全失效","B":"降低车速并尽快到店检查制动片磨损","C":"用急刹反复磨合","D":"关闭音乐以免听到异响"}',
   'B', 15);

COMMIT;
