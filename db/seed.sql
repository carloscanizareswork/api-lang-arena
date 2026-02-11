CREATE TABLE IF NOT EXISTS bill (
  id BIGSERIAL PRIMARY KEY,
  bill_number TEXT NOT NULL UNIQUE,
  issued_at DATE NOT NULL,
  customer_name TEXT NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'USD',
  subtotal NUMERIC(12,2) NOT NULL DEFAULT 0,
  tax NUMERIC(12,2) NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE bill DROP COLUMN IF EXISTS total;

CREATE TABLE IF NOT EXISTS bill_line (
  id BIGSERIAL PRIMARY KEY,
  bill_id BIGINT NOT NULL REFERENCES bill(id) ON DELETE CASCADE,
  line_no INTEGER NOT NULL,
  concept TEXT NOT NULL,
  quantity NUMERIC(10,2) NOT NULL,
  unit_amount NUMERIC(12,2) NOT NULL,
  line_amount NUMERIC(12,2) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (bill_id, line_no)
);

TRUNCATE TABLE bill_line, bill RESTART IDENTITY;

DO $$
DECLARE
  i INTEGER;
  j INTEGER;
  v_bill_id BIGINT;
  v_line_count INTEGER;
  v_quantity NUMERIC(10,2);
  v_unit_amount NUMERIC(12,2);
  v_line_amount NUMERIC(12,2);
  v_subtotal NUMERIC(12,2);
  v_tax_rate NUMERIC(6,4);
  v_tax NUMERIC(12,2);
  v_customers TEXT[] := ARRAY[
    'Northwind Traders', 'Acme Corp', 'Globex Inc', 'Soylent Labs',
    'Initech', 'Umbrella Group', 'Stark Industries', 'Wayne Enterprises',
    'Hooli', 'Wonka Industries', 'Pied Piper', 'Aperture Labs'
  ];
  v_concepts TEXT[] := ARRAY[
    'Cloud Hosting', 'API Requests', 'Data Processing', 'Consulting Hours',
    'Software License', 'Support Plan', 'Storage Usage', 'Bandwidth',
    'Database Backups', 'Security Monitoring', 'Email Service', 'SMS Service',
    'Training Session', 'Integration Fee', 'Premium Feature', 'Bug Fix Package'
  ];
BEGIN
  FOR i IN 1..100 LOOP
    INSERT INTO bill (bill_number, issued_at, customer_name)
    VALUES (
      format('BILL-%s', lpad(i::TEXT, 4, '0')),
      current_date - ((random() * 180)::INT),
      v_customers[1 + floor(random() * array_length(v_customers, 1))::INT]
    )
    RETURNING id INTO v_bill_id;

    v_line_count := 10 + floor(random() * 6)::INT;

    FOR j IN 1..v_line_count LOOP
      v_quantity := round((1 + random() * 4)::NUMERIC, 2);
      v_unit_amount := round((10 + random() * 490)::NUMERIC, 2);
      v_line_amount := round(v_quantity * v_unit_amount, 2);

      INSERT INTO bill_line (bill_id, line_no, concept, quantity, unit_amount, line_amount)
      VALUES (
        v_bill_id,
        j,
        v_concepts[1 + floor(random() * array_length(v_concepts, 1))::INT],
        v_quantity,
        v_unit_amount,
        v_line_amount
      );
    END LOOP;

    SELECT COALESCE(sum(line_amount), 0) INTO v_subtotal
    FROM bill_line
    WHERE bill_id = v_bill_id;

    v_tax_rate := round((0.05 + random() * 0.07)::NUMERIC, 4);
    v_tax := round(v_subtotal * v_tax_rate, 2);

    UPDATE bill
    SET subtotal = v_subtotal,
        tax = v_tax
    WHERE id = v_bill_id;
  END LOOP;
END $$;
