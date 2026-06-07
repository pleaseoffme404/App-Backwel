CREATE OR REPLACE FUNCTION fn_process_product_discount(
    category_path UUID[], product_uuid UUID
) RETURNS NUMERIC(5,4)
LANGUAGE plpgsql
AS $$
DECLARE
    v_discount_decimal NUMERIC(5,4);
BEGIN
    WITH
        available_discounts AS (
            SELECT
                d.id,
                d.stackable,
                d.decimal_value
            FROM discount d
            WHERE d.start_date <= NOW()
            AND d.end_date > NOW()
            AND d.active
        ),
        discounts AS (
            SELECT
                dt.discount_id AS discount_id,
                d.stackable,
                d.decimal_value
            FROM discount_target dt
            INNER JOIN available_discounts d ON dt.discount_id = d.id
            WHERE dt.category_id = ANY(category_path)

            UNION

            SELECT
                dt.discount_id as discount_id,
                d.stackable,
                d.decimal_value
            FROM discount_target dt
            INNER JOIN available_discounts d ON dt.discount_id = d.id
            WHERE dt.product_id = product_uuid
        ),
        ranked_discounts AS (
            SELECT
                discount_id,
                stackable,
                decimal_value,
                ROW_NUMBER() OVER (PARTITION BY stackable ORDER BY decimal_value DESC) AS rn
            FROM discounts
        ),
        all_strategies AS (
            SELECT
                COALESCE(SUM(decimal_value), 0) AS potential_discount
            FROM ranked_discounts
            WHERE stackable = true AND rn <= 3

            UNION ALL

            SELECT
                COALESCE(SUM(decimal_value), 0) AS potential_discount
            FROM ranked_discounts
            WHERE stackable = false AND rn = 1
        )

    SELECT
        CAST(ROUND(MAX(potential_discount),4) AS NUMERIC(5,4))
    INTO v_discount_decimal
    FROM all_strategies;

    RETURN v_discount_decimal;
END;
$$;