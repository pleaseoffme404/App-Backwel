CREATE OR REPLACE PROCEDURE pr_process_price_recalculation(
    p_transaction_id UUID
)
    LANGUAGE plpgsql
AS $$
BEGIN
    CREATE TEMP TABLE IF NOT EXISTS tmp_applied_price_changes AS
    WITH filtered_pending_changes AS (
        SELECT
            item_id,
            new_price,
            created_at
        FROM item_base_price_change_track
        WHERE checked_by_transaction IS NULL
    ),
         ranked_base_price_changed AS (
             SELECT
                 item_id,
                 new_price,
                 ROW_NUMBER() OVER (
                     PARTITION BY item_id
                     ORDER BY created_at DESC
                     ) AS rn
             FROM filtered_pending_changes
         )
    SELECT item_id, new_price
    FROM ranked_base_price_changed
    WHERE rn = 1;

    -- 1. Actualizar la tabla maestra de ítems
    UPDATE item AS i
    SET base_price = s.new_price
    FROM tmp_applied_price_changes AS s
    WHERE i.id = s.item_id;

    -- 2. Flag cambios pendientes como revisados (Checked)
    UPDATE item_base_price_change_track
    SET checked_by_transaction = p_transaction_id
    WHERE checked_by_transaction IS NULL;

    -- 3. Proceder al procesamiento de descuentos
    WITH
        created_at AS (SELECT NOW() AS now),
        unique_values AS (
            -- Part 1: Discounts by category
            SELECT
                ipcp.item_id,
                d.id as discount_id,
                d.stackable,
                d.decimal_value as discount_value
            FROM item i
                     INNER JOIN item_product_category_path ipcp ON i.id = ipcp.item_id
                     INNER JOIN discount_target dt ON ipcp.category_path @> ARRAY[dt.category_id]
                     INNER JOIN discount d ON dt.discount_id = d.id
                AND d.start_date <= NOW()
                AND d.end_date > NOW()
                AND d.active = true
            WHERE i.visible = true

            UNION ALL

            -- Part 2: Discounts by product
            SELECT
                ipcp.item_id,
                d.id as discount_id,
                d.stackable,
                d.decimal_value as discount_value
            FROM item i
                     INNER JOIN item_product_category_path ipcp ON i.id = ipcp.item_id
                     INNER JOIN discount_target dt ON dt.product_id = ipcp.product_id
                     INNER JOIN discount d ON dt.discount_id = d.id
                AND d.start_date <= NOW()
                AND d.end_date > NOW()
                AND d.active = true
            WHERE i.visible = true

            UNION ALL

            -- Part 3: Discounts by item
            SELECT
                ipcp.item_id,
                d.id as discount_id,
                d.stackable,
                d.decimal_value as discount_value
            FROM item i
                     INNER JOIN item_product_category_path ipcp ON i.id = ipcp.item_id
                     INNER JOIN discount_target dt ON dt.item_id = ipcp.item_id
                     INNER JOIN discount d ON dt.discount_id = d.id
                AND d.start_date <= NOW()
                AND d.end_date > NOW()
                AND d.active = true
            WHERE i.visible = true
        ),
        deduplicated_discounts AS (
            SELECT item_id, discount_id, stackable, discount_value
            FROM unique_values
            GROUP BY item_id, discount_id, stackable, discount_value
        ),
        ranked_discounts AS (
            SELECT
                item_id,
                stackable,
                discount_value,
                ROW_NUMBER() OVER (
                    PARTITION BY item_id, stackable
                    ORDER BY discount_value DESC
                    ) AS rank_desc
            FROM deduplicated_discounts
        ),
        all_strategies AS (
            SELECT
                item_id,
                COALESCE(SUM(discount_value), 0) AS potential_discount
            FROM ranked_discounts
            WHERE stackable = true AND rank_desc <= 3
            GROUP BY item_id

            UNION ALL

            SELECT
                item_id,
                COALESCE(MAX(discount_value), 0) AS potential_discount
            FROM ranked_discounts
            WHERE stackable = false AND rank_desc = 1
            GROUP BY item_id
        ),
        new_calculated_discounts AS (
            SELECT
                item_id,
                CAST(ROUND(MAX(potential_discount), 4) AS NUMERIC(5,4)) AS discount_decimal
            FROM all_strategies
            GROUP BY item_id
        ),

        -- Obtener ítems que tenían descuento en el evento previo
        last_transaction AS (
            SELECT transaction_id
            FROM price_calculation_history
            ORDER BY created_at DESC
            LIMIT 1
        ),
        had_discount_items AS (
            SELECT
                h.item_id
            FROM price_calculation_history h
                     INNER JOIN last_transaction lt
                                ON h.transaction_id = lt.transaction_id AND h.discount_decimal != 0.00
        ),

        -- PARCHE CORREGIDO: Leemos de la tabla temporal limpia con los ids únicos reales de este ciclo
        base_price_changed AS (
            SELECT item_id FROM tmp_applied_price_changes
        ),

        set_b AS (
            SELECT item_id FROM had_discount_items
            UNION
            SELECT item_id FROM base_price_changed
        ),

        extra_payload_ids AS (
            SELECT
                b.item_id
            FROM set_b b
                     LEFT JOIN new_calculated_discounts ncd ON b.item_id = ncd.item_id
            WHERE ncd.item_id IS NULL
        ),
        extra_payload AS (
            SELECT
                ei.item_id,
                i.base_price as base_price,
                i.base_price as final_price,
                0.00 AS discount_decimal
            FROM extra_payload_ids ei
                     JOIN item i ON ei.item_id = i.id
        ),
        base_payload AS (
            SELECT
                ncd.item_id,
                i.base_price AS base_price,
                CAST(ROUND((1 - ncd.discount_decimal) * i.base_price, 2)AS NUMERIC(12,2)) AS final_price,
                ncd.discount_decimal
            FROM new_calculated_discounts ncd
                     JOIN item i ON ncd.item_id = i.id
        ),
        final_payload_to_insert AS (
            SELECT * FROM base_payload
            UNION ALL
            SELECT * FROM extra_payload
        )

    INSERT INTO price_calculation_history(id, transaction_id, item_id, base_price, final_price, discount_decimal, created_at)
    SELECT
        DEFAULT,
        p_transaction_id,
        item_id,
        base_price,
        final_price,
        discount_decimal,
        NOW() AS created_at
    FROM final_payload_to_insert;

    DROP TABLE tmp_applied_price_changes;
END;
$$;


