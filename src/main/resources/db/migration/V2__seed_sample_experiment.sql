INSERT INTO experiments (id, experiment_key, name, enabled, salt, traffic_allocation_bp)
VALUES ('11111111-1111-1111-1111-111111111111', 'checkout_button_color', 'Checkout Button Color', true, 'checkout-button-color-v1', 10000)
ON CONFLICT (experiment_key) DO NOTHING;

INSERT INTO variants (id, experiment_id, variant_key, weight, payload_json, position)
VALUES
    ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'control', 5000, '{"color":"blue","label":"Pay now"}'::jsonb, 0),
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'green', 5000, '{"color":"green","label":"Complete purchase"}'::jsonb, 1)
ON CONFLICT (experiment_id, variant_key) DO NOTHING;
