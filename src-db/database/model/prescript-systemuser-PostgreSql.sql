-- In previous versions of Openbravo uuid_generate_v4 was created manually not enabling the uuid-ossp
-- extension. If that is the case we need to drop it before creating the extension to avoid errors.
DO $$DECLARE
  v_exists NUMERIC;

BEGIN
  SELECT count(1) INTO v_exists FROM pg_extension WHERE extname = 'uuid-ossp';
  IF (v_exists = 0) THEN
    DROP FUNCTION IF EXISTS uuid_generate_v4();
  END IF;
END$$;
/-- END

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
/-- END

CREATE EXTENSION IF NOT EXISTS "pg_trgm";
/-- END
