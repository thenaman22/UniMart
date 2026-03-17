package com.unimart.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MembershipRoleConstraintRepair {

    private static final Logger log = LoggerFactory.getLogger(MembershipRoleConstraintRepair.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public MembershipRoleConstraintRepair(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void repairConstraint() {
        if (!isPostgres()) {
            return;
        }

        String definition = jdbcTemplate.query(
            """
                select pg_get_constraintdef(constraint_row.oid)
                from pg_constraint constraint_row
                join pg_class table_row on table_row.oid = constraint_row.conrelid
                join pg_namespace namespace_row on namespace_row.oid = table_row.relnamespace
                where table_row.relname = 'memberships'
                  and namespace_row.nspname = current_schema()
                  and constraint_row.conname = 'memberships_role_check'
                """,
            resultSet -> resultSet.next() ? resultSet.getString(1) : null
        );

        if (definition != null && definition.contains("'SELLER'")) {
            return;
        }

        jdbcTemplate.execute("alter table memberships drop constraint if exists memberships_role_check");
        jdbcTemplate.execute(
            """
                alter table memberships
                add constraint memberships_role_check
                check (role in ('MEMBER', 'SELLER', 'MODERATOR', 'ADMIN'))
                """
        );
        log.info("Updated memberships_role_check to include SELLER");
    }

    private boolean isPostgres() {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to inspect the database product name", exception);
        }
    }
}
