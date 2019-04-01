-- Every time the application starts, reset pg_stat_statements so we can track slow queries only for the current version
-- Won't do anything if pg_stat_statements isn't enabled. If it is, the connecting user will need permission to run it, e.g.
-- tabula=# grant execute on function pg_stat_statements_reset() to tabula_dev;
DO $$
  BEGIN
    IF EXISTS
      (select * from pg_proc where proname = 'pg_stat_statements_reset')
    THEN
      perform pg_stat_statements_reset();
    END IF;
  END
  $$;