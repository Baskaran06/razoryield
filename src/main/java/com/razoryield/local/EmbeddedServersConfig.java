package com.razoryield.local;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import redis.embedded.RedisServer;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * Runs PostgreSQL and Redis in-process so the app starts on a machine with neither installed and no
 * Docker. Active only under the {@code local} profile; production points at real servers through
 * DB_URL and REDIS_HOST as normal.
 *
 * <p>Both servers here are the genuine article, not fakes: the binaries ship inside the Maven
 * artifacts and are launched as child processes, so Flyway, the CHECK constraints, the unique
 * constraint and the atomic INCRBY all behave exactly as they will in production.
 */
@Configuration
@Profile("local")
public class EmbeddedServersConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedServersConfig.class);

    private EmbeddedPostgres postgres;
    private RedisServer redisServer;

    @Bean
    @Primary
    public DataSource embeddedPostgresDataSource() throws IOException {
        postgres = EmbeddedPostgres.builder().start();
        log.info("Embedded PostgreSQL listening on port {}", postgres.getPort());
        return postgres.getPostgresDatabase();
    }

    @Bean
    @Primary
    public LettuceConnectionFactory embeddedRedisConnectionFactory() throws IOException {
        // A redis-server child process outlives a force-killed JVM and keeps its port bound, so a
        // fixed port makes the next run fail to start. Claim a free one instead.
        int port = findFreePort();
        redisServer = RedisServer.newRedisServer()
                .port(port)
                .setting("maxmemory 64M")
                .onShutdownForceStop(true)
                .build();
        redisServer.start();
        log.info("Embedded Redis listening on port {}", port);
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", port));
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    @PreDestroy
    void shutdown() {
        if (redisServer != null) {
            try {
                redisServer.stop();
            } catch (Exception e) {
                log.warn("Embedded Redis did not stop cleanly: {}", e.toString());
            }
        }
        if (postgres != null) {
            try {
                postgres.close();
            } catch (Exception e) {
                log.warn("Embedded PostgreSQL did not stop cleanly: {}", e.toString());
            }
        }
    }
}
