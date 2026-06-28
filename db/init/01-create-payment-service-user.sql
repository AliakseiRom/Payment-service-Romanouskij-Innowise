DO $$
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_catalog.pg_roles
        WHERE rolname = 'payment_service_app'
    ) THEN
        CREATE USER payment_service_app WITH PASSWORD 'payment_service_pass';
ELSE
        ALTER USER payment_service_app WITH PASSWORD 'payment_service_pass';
END IF;
END
$$;

GRANT CONNECT ON DATABASE payment_service TO payment_service_app;

GRANT USAGE, CREATE ON SCHEMA public TO payment_service_app;