package com.community.assist.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 幂等结构迁移：Hibernate 会为枚举列生成 CHECK 约束，枚举值演进后老库插入新值会被拒绝。
 * 每次启动删除 public schema 下所有 *_check 约束（本库中均为枚举约束），避免此类问题。
 */
@Component
public class EnumCheckConstraintMigration implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public EnumCheckConstraintMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        jdbc.execute("""
                DO $$
                DECLARE r RECORD;
                BEGIN
                  FOR r IN (
                    SELECT con.conname AS name, con.conrelid::regclass::text AS tbl
                    FROM pg_constraint con
                    JOIN pg_namespace n ON n.oid = con.connamespace
                    WHERE con.contype = 'c' AND n.nspname = 'public'
                      AND con.conrelid <> 0 AND con.conname LIKE '%\\_check'
                  ) LOOP
                    EXECUTE format('ALTER TABLE %s DROP CONSTRAINT IF EXISTS %I', r.tbl, r.name);
                  END LOOP;
                END $$;
                """);
    }
}
