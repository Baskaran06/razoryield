package com.razoryield.migration;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the real Flyway migrations against a real PostgreSQL, started in-process so the build does
 * not need Docker. This is the automated form of the Phase 0 verification queries.
 */
class FlywayMigrationTest {

    private static EmbeddedPostgres postgres;
    private static DataSource dataSource;

    @BeforeAll
    static void migrate() throws Exception {
        postgres = EmbeddedPostgres.builder().start();
        dataSource = postgres.getPostgresDatabase();

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @AfterAll
    static void shutdown() throws Exception {
        if (postgres != null) {
            postgres.close();
        }
    }

    private static long scalar(String sql) throws SQLException {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    @Test
    @DisplayName("both migrations apply cleanly and create all four tables")
    void migrationsApply() throws Exception {
        long tables = scalar("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('products', 'customer_cohorts', 'campaigns', 'campaign_audit_log')
                """);
        assertThat(tables).isEqualTo(4);
    }

    @Test
    @DisplayName("every money column is BIGINT, never a floating-point or decimal type")
    void moneyColumnsAreBigint() throws Exception {
        long wrongTypes = scalar("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND column_name LIKE '%_paise'
                  AND data_type <> 'bigint'
                """);
        assertThat(wrongTypes).isZero();

        // products (cost, base) + campaigns (offer) + campaign_audit_log (cost, base, offer)
        long paiseColumns = scalar("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'public' AND column_name LIKE '%_paise'
                """);
        assertThat(paiseColumns).isEqualTo(6);
    }

    @Test
    @DisplayName("the margin-breach fixture is present and genuinely unservable at a 15% floor")
    void marginBreachFixtureIsCorrect() throws Exception {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("""
                     SELECT sku, cost_price_paise, base_price_paise,
                            (cost_price_paise * 115) / 100 AS floor_price_paise
                     FROM products
                     WHERE (cost_price_paise * 115) / 100 > base_price_paise
                     """)) {

            assertThat(rs.next()).as("exactly one product must be an unservable fixture").isTrue();
            assertThat(rs.getString("sku")).isEqualTo("SKU-LEGACY-PRINTER");
            assertThat(rs.getLong("cost_price_paise")).isEqualTo(95_000L);
            assertThat(rs.getLong("base_price_paise")).isEqualTo(100_000L);
            assertThat(rs.getLong("floor_price_paise")).isEqualTo(109_250L);
            assertThat(rs.next()).as("no other product should be unservable").isFalse();
        }
    }

    @Test
    @DisplayName("ten products are seeded")
    void tenProductsSeeded() throws Exception {
        assertThat(scalar("SELECT COUNT(*) FROM products")).isEqualTo(10);
    }

    @Test
    @DisplayName("all five cohort rows meet the targeting criteria")
    void fiveTargetableCohorts() throws Exception {
        assertThat(scalar("SELECT COUNT(*) FROM customer_cohorts")).isEqualTo(5);
        assertThat(scalar("""
                SELECT COUNT(*) FROM customer_cohorts
                WHERE days_since_last_purchase >= 45 AND total_orders >= 2
                """)).isEqualTo(5);
    }

    @Test
    @DisplayName("the status CHECK constraint refuses a status outside the state machine")
    void statusCheckConstraintIsEnforced() {
        assertThatThrownBy(() -> {
            try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
                s.executeUpdate("""
                        INSERT INTO campaigns (sku, status, discount_pct, offer_price_paise)
                        VALUES ('SKU-TEA-250G', 'TOTALLY_MADE_UP', 5.00, 20000)
                        """);
            }
        }).isInstanceOf(SQLException.class).hasMessageContaining("chk_campaign_status");
    }

    @Test
    @DisplayName("the unique constraint stops a duplicate payment id from being recorded twice")
    void duplicatePaymentIdIsRejected() throws Exception {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                    INSERT INTO campaigns (id, sku, status, discount_pct, offer_price_paise)
                    VALUES ('11111111-1111-1111-1111-111111111111', 'SKU-TEA-250G',
                            'PENDING_MERCHANT_APPROVAL', 12.50, 21000)
                    """);
            String insertAudit = """
                    INSERT INTO campaign_audit_log
                        (campaign_id, sku, cost_price_paise, base_price_paise, discount_pct,
                         offer_price_paise, llm_reasoning, gate_verdict, razorpay_payment_id, settlement_status)
                    VALUES ('11111111-1111-1111-1111-111111111111', 'SKU-TEA-250G', 12000, 24000, 12.50,
                            21000, 'seeded by test', 'WEBHOOK_SETTLED', 'pay_duplicate_1', 'PAID')
                    """;
            s.executeUpdate(insertAudit);

            assertThatThrownBy(() -> {
                try (Connection c2 = dataSource.getConnection(); Statement s2 = c2.createStatement()) {
                    s2.executeUpdate(insertAudit);
                }
            }).isInstanceOf(SQLException.class).hasMessageContaining("unique_payment_id");
        }
    }

    @Test
    @DisplayName("multiple audit rows may carry a null payment id, so the constraint does not block proposals")
    void nullPaymentIdsAreNotConstrained() throws Exception {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                    INSERT INTO campaigns (id, sku, status, discount_pct, offer_price_paise)
                    VALUES ('22222222-2222-2222-2222-222222222222', 'SKU-SOAP-4PK',
                            'AUTO_DISPATCHED', 5.00, 17000)
                    """);
            String insertProposal = """
                    INSERT INTO campaign_audit_log
                        (campaign_id, sku, cost_price_paise, base_price_paise, discount_pct,
                         offer_price_paise, llm_reasoning, gate_verdict)
                    VALUES ('22222222-2222-2222-2222-222222222222', 'SKU-SOAP-4PK', 8800, 18000, 5.00,
                            17000, 'proposal row', 'PASSED_AUTO_DISPATCH')
                    """;
            s.executeUpdate(insertProposal);
            s.executeUpdate(insertProposal);
        }

        assertThat(scalar("""
                SELECT COUNT(*) FROM campaign_audit_log
                WHERE campaign_id = '22222222-2222-2222-2222-222222222222'
                """)).isEqualTo(2);
    }
}
