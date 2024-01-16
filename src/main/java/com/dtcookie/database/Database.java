package com.dtcookie.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;

public class Database {

    public static final Logger log = LogManager.getLogger(Database.class);

    private static com.dtcookie.database.internal.Driver DRIVER = new com.dtcookie.database.internal.Driver().setConnectionListener(Database::onConnectionClosed);
    private static final OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
    private static final Meter meter = openTelemetry.meterBuilder("manual-instrumentation").setInstrumentationVersion("1.0.0").build();
    private static final LongUpDownCounter activeConnectionsCounter = meter.upDownCounterBuilder("shop.database.connections.active").setDescription("Number of active Database Connections").build();

    private static final AtomicInteger activeConnections = new AtomicInteger(20);

    public static AtomicBoolean Debug = new AtomicBoolean(false);

    private static final long DELAY_PER_ACTIVE_CONNECTION = 56;

    private static AtomicLong lastLogTS = new AtomicLong(0);

    static {
        Attributes attributes = Attributes.of(AttributeKey.stringKey("tier"), System.getenv("DEMO_PURPOSE"));
        activeConnectionsCounter.add(activeConnections.get(), attributes);
    }

    private Database() {        
        // prevent instantiation
    }
    
    private static Connection newConnection() {
        try {
            return DRIVER.connect("jdbc:datadirect:oracle://localhost:1521;ServiceName=ShopDatabase", new Properties());
        } catch (SQLException e) {
            throw new InternalError(e);
        }        
    }
    
    public static Connection getConnection(long timeout, TimeUnit unit) throws InterruptedException {
        int numActiveConnections = activeConnections.get();
        long timeoutMillis = TimeUnit.MILLISECONDS.convert(timeout, unit);
        long expectedDelay = 1500 + numActiveConnections * DELAY_PER_ACTIVE_CONNECTION;
        Thread.sleep(Math.min(expectedDelay, timeoutMillis));
        if (timeoutMillis < expectedDelay) {
            return null;
        }
        numActiveConnections = activeConnections.incrementAndGet();
        log("Active Connection: " + numActiveConnections);
        
        Attributes attributes = Attributes.of(AttributeKey.stringKey("tier"), System.getenv("DEMO_PURPOSE"));
        activeConnectionsCounter.add(1, attributes);
        return newConnection();
    }

    private static void log(String s) {
        if (!Debug.get()) {
            return;
        }
        if (s == null || s.isBlank() || s.isEmpty()) {
            return;
        }
        synchronized (lastLogTS) {
            long last = lastLogTS.get();
            long now = System.currentTimeMillis();
            if (now - last > 10 * 1000) {
                lastLogTS.set(now);
                log.info(s);
            }
        }
    }
    
    private static void onConnectionClosed(Connection con) {
    	if (con == null) {
    		return;
    	}
        activeConnections.decrementAndGet();        
        Attributes attributes = Attributes.of(AttributeKey.stringKey("tier"), System.getenv("DEMO_PURPOSE"));
        activeConnectionsCounter.add(-1, attributes);
    }

    public static void execute(String sql) throws SQLException, InterruptedException {
        long start = System.currentTimeMillis();
        try (Connection con = Database.getConnection(4, TimeUnit.SECONDS)) {
            Thread.sleep(System.currentTimeMillis() - start);
            try (Statement stmt = con.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    public static void executeUpdate(String sql) throws SQLException, InterruptedException {
        long start = System.currentTimeMillis();
        try (Connection con = Database.getConnection(4, TimeUnit.SECONDS)) {
            Thread.sleep(System.currentTimeMillis() - start);
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate(sql);
            }
        }
    }

}
