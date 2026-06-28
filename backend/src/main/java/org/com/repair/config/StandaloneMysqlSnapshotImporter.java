package org.com.repair.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@Profile("standalone")
@ConditionalOnProperty(prefix = "standalone.mysql-snapshot", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StandaloneMysqlSnapshotImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StandaloneMysqlSnapshotImporter.class);
    private static final String MARKER_TABLE = "standalone_seed_metadata";
    private static final String MARKER_KEY = "mysql_snapshot_imported";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final Path snapshotPath;

    public StandaloneMysqlSnapshotImporter(
            DataSource dataSource,
            @Value("${standalone.mysql-snapshot.path:}") String snapshotPath) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.snapshotPath = snapshotPath == null || snapshotPath.isBlank() ? null : Path.of(snapshotPath);
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        if (snapshotPath == null || Files.notExists(snapshotPath)) {
            return;
        }

        ensureMarkerTable();
        if (alreadyImported()) {
            log.info("Standalone MySQL snapshot has already been imported, skipping: {}", snapshotPath);
            return;
        }
        if (hasExistingBusinessData()) {
            markImported("skipped_existing_data");
            log.info("Standalone database already has business data, skipping MySQL snapshot import: {}", snapshotPath);
            return;
        }

        log.info("Importing standalone MySQL snapshot: {}", snapshotPath);
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new EncodedResource(new FileSystemResource(snapshotPath), StandardCharsets.UTF_8));
        }
        resetIdentityColumns();
        markImported("imported");
    }

    private void ensureMarkerTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS standalone_seed_metadata (
                    metadata_key VARCHAR(128) PRIMARY KEY,
                    metadata_value VARCHAR(255),
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    private boolean alreadyImported() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + MARKER_TABLE + " WHERE metadata_key = ?",
                Integer.class,
                MARKER_KEY);
        return count != null && count > 0;
    }

    private boolean hasExistingBusinessData() {
        return countRows("admin") > 0
                || countRows("user") > 0
                || countRows("technician") > 0
                || countRows("material") > 0
                || countRows("repair_order") > 0;
    }

    private int countRows(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + tableName + "`", Integer.class);
            return count == null ? 0 : count;
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private void resetIdentityColumns() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT TABLE_NAME
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND COLUMN_NAME = 'id'
                """, String.class);

        for (String table : tables) {
            String normalizedTable = table.toLowerCase(Locale.ROOT);
            if (!normalizedTable.matches("[a-z0-9_]+")) {
                continue;
            }
            Long nextId = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(id), 0) + 1 FROM `" + normalizedTable + "`",
                    Long.class);
            long restartWith = nextId == null ? 1L : Math.max(1L, nextId);
            jdbcTemplate.execute("ALTER TABLE `" + normalizedTable + "` ALTER COLUMN id RESTART WITH " + restartWith);
        }
    }

    private void markImported(String status) {
        jdbcTemplate.update("""
                MERGE INTO standalone_seed_metadata (metadata_key, metadata_value, updated_at)
                KEY(metadata_key)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """, MARKER_KEY, status);
    }
}
