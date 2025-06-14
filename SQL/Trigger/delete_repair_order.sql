DELIMITER $$

CREATE TRIGGER delete_user
BEFORE DELETE ON repair_order
FOR EACH ROW
BEGIN
    DELETE FROM order_technician
    WHERE order_id = OLD.id;

END$$

DELIMITER ;