-- Etapa 8: trazabilidad de saldos iniciales.
-- Los productos creados antes de esta etapa pudieron fijar existencias sin
-- dejar movimiento en el historial. Esta migracion registra un saldo de
-- apertura para cada producto que tenga existencias pero ningun movimiento,
-- de modo que todo saldo quede explicado por el libro mayor.
-- El NOT EXISTS evita duplicar productos que ya tienen historial.

INSERT INTO stock_movement (product_id, movement_type, quantity, previous_stock, new_stock,
                            note, created_at)
SELECT p.id, 'IN', p.stock, 0, p.stock,
       'Saldo de apertura (migracion de saldos existentes)', now()
FROM product p
WHERE p.stock > 0
  AND NOT EXISTS (SELECT 1 FROM stock_movement m WHERE m.product_id = p.id);
