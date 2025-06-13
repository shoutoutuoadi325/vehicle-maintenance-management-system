
-- 删除旧触发器
DROP TRIGGER IF EXISTS delete_user;

DELIMITER $$

CREATE TRIGGER delete_user
BEFORE DELETE ON `user`
FOR EACH ROW
BEGIN
    /* 1. 先删反馈 --------------- */
    DELETE FROM feedback
    WHERE repair_order_id IN (
        SELECT id FROM repair_order WHERE user_id = OLD.id
    );

    /* 2. 再删 order_technician --- */
    DELETE FROM order_technician
    WHERE order_id IN (
        SELECT id FROM repair_order WHERE user_id = OLD.id
    );

    /* 3. 删除维修单 -------------- */
    DELETE FROM repair_order
    WHERE user_id = OLD.id;

    /* 4. 删除车辆 ---------------- */
    DELETE FROM vehicle
    WHERE user_id = OLD.id;
END$$

DELIMITER ;