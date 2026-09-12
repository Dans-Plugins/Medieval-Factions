package com.dansplugins.factionsystem.trace;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the client against a real HTTP server on a loopback port -- the
 * JDK's own, so the test has no more dependencies than the client does.
 */
class TraceClientTest {

    private HttpServer server;
    private final List<Received> received = new CopyOnWriteArrayList<>();
    private volatile int replyStatus = 201;
    private volatile CountDownLatch arrived = new CountDownLatch(1);

    private static final class Received {
        final String method;
        final String path;
        final String authorization;
        final String contentType;
        final String body;

        Received(String method, String path, String authorization, String contentType, String body) {
            this.method = method;
            this.path = path;
            this.authorization = authorization;
            this.contentType = contentType;
            this.body = body;
        }
    }

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = readAll(exchange.getRequestBody());
            received.add(new Received(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    new String(body, StandardCharsets.UTF_8)));
            exchange.sendResponseHeaders(replyStatus, -1);
            exchange.close();
            arrived.countDown();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void report_postsTheEventToTheMetricsEndpointWithTheKey() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl() + "/", "MyPlugin").key("k-123").build();

        // Act
        client.report("startup");

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS), "the report should reach the server");
        Received request = received.get(0);
        assertEquals("POST", request.method);
        assertEquals("/api/metrics", request.path, "a trailing slash on the base URL must not double up");
        assertEquals("Bearer k-123", request.authorization);
        assertTrue(request.contentType.startsWith("application/json"), request.contentType);
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"startup\"}", request.body);
        client.close();
    }

    @Test
    void report_carriesValueAndTagsWhenGiven() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin").key("k").build();
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("command", "home");
        tags.put("world", "the \"end\"");

        // Act
        client.report("command", 2.5, tags);

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        assertEquals(
                "{\"application\":\"MyPlugin\",\"name\":\"command\",\"value\":2.5,"
                        + "\"tags\":{\"command\":\"home\",\"world\":\"the \\\"end\\\"\"}}",
                received.get(0).body);
        client.close();
    }

    @Test
    void report_returnsBeforeTheServerAnswers() throws Exception {
        // Arrange
        // A server that never replies. If report() waited on the network the
        // caller -- a game server's main thread, in the case that matters --
        // would wait with it.
        CountDownLatch release = new CountDownLatch(1);
        HttpServer slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slow.createContext("/", exchange -> {
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        slow.start();
        TraceClient client = TraceClient.builder("http://127.0.0.1:" + slow.getAddress().getPort(), "MyPlugin")
                .key("k").build();

        // Act
        long before = System.nanoTime();
        client.report("startup");
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);

        // Assert
        assertTrue(elapsedMs < 1_000, "report() took " + elapsedMs + " ms; it must not wait on the network");
        release.countDown();
        client.close();
        slow.stop(0);
    }

    @Test
    void report_doesNotThrowWhenNothingIsListening() throws Exception {
        // Arrange
        // Pick a port by binding and releasing it, so nothing answers there.
        HttpServer probe = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int deadPort = probe.getAddress().getPort();
        probe.stop(0);
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.dead");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);
        TraceClient client = TraceClient.builder("http://127.0.0.1:" + deadPort, "MyPlugin")
                .key("k").logger(logger).build();

        // Act
        assertDoesNotThrow(() -> client.report("startup"));
        client.close(); // waits for the in-flight attempt to fail

        // Assert
        assertTrue(log.await(5, TimeUnit.SECONDS), "the failure should be mentioned at FINE");
        assertEquals(Level.FINE, log.records.get(0).getLevel());
        assertTrue(log.records.get(0).getMessage().contains("could not deliver"), log.records.get(0).getMessage());
    }

    @Test
    void report_doesNotThrowWhenTheServerRejectsTheKey() throws Exception {
        // Arrange
        replyStatus = 401;
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.rejected");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin").key("revoked").logger(logger).build();

        // Act
        assertDoesNotThrow(() -> client.report("startup"));

        // Assert
        assertTrue(log.await(5, TimeUnit.SECONDS));
        assertTrue(log.records.get(0).getMessage().contains("answered 401"), log.records.get(0).getMessage());
        client.close();
    }

    @Test
    void disabledClient_sendsNothing() throws Exception {
        // Arrange
        TraceClient byFlag = TraceClient.builder(baseUrl(), "MyPlugin").key("k").enabled(false).build();
        TraceClient byMissingKey = TraceClient.builder(baseUrl(), "MyPlugin").build();
        TraceClient byBlankKey = TraceClient.builder(baseUrl(), "MyPlugin").key("  ").build();
        TraceClient explicit = TraceClient.disabled();

        // Act
        for (TraceClient client : new TraceClient[] {byFlag, byMissingKey, byBlankKey, explicit}) {
            assertFalse(client.isEnabled());
            client.report("startup");
            client.close();
        }

        // Assert
        assertFalse(arrived.await(300, TimeUnit.MILLISECONDS), "nothing should have been sent");
        assertTrue(received.isEmpty());
    }

    @Test
    void report_ignoresABlankName() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin").key("k").build();

        // Act
        client.report(null);
        client.report("   ");
        client.close();

        // Assert
        assertFalse(arrived.await(300, TimeUnit.MILLISECONDS));
        assertTrue(received.isEmpty());
    }

    @Test
    void builder_rejectsAMissingBaseUrlOrApplication() {
        assertThrows(IllegalArgumentException.class, () -> TraceClient.builder(null, "MyPlugin"));
        assertThrows(IllegalArgumentException.class, () -> TraceClient.builder(" ", "MyPlugin"));
        assertThrows(IllegalArgumentException.class, () -> TraceClient.builder("http://x", null));
        assertThrows(IllegalArgumentException.class, () -> TraceClient.builder("http://x", ""));
    }

    @Test
    void json_escapesControlCharactersAndSkipsNullTags() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("ok", "line\nbreak\ttab\\slash");
        tags.put("nullValue", null);
        tags.put(null, "nullKey");

        String json = TraceClient.json("App", "n", Double.NaN, tags);

        assertEquals("{\"application\":\"App\",\"name\":\"n\",\"tags\":{\"ok\":\"line\\nbreak\\ttab\\\\slash\"}}", json,
                "NaN is not JSON and is dropped; null keys or values are skipped");
        assertEquals("\"\\u0001\"", TraceClient.quote("\u0001"));
    }

    @Test
    void queue_isBoundedAndDropsRatherThanGrows() throws Exception {
        // Arrange
        // Hold the sending thread on the first report so everything behind it
        // queues, overfill the queue, then let the server drain and count.
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger delivered = new AtomicInteger();
        HttpServer slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slow.createContext("/", exchange -> {
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            delivered.incrementAndGet();
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        slow.start();
        TraceClient client = TraceClient.builder("http://127.0.0.1:" + slow.getAddress().getPort(), "MyPlugin")
                .key("k").build();
        int flood = TraceClient.QUEUE_CAPACITY * 3;

        // Act
        for (int i = 0; i < flood; i++) {
            client.report("flood");
        }
        release.countDown();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        int seen = -1;
        while (System.nanoTime() < deadline) {
            Thread.sleep(200);
            int now = delivered.get();
            if (now == seen) {
                break; // nothing arrived in the last 200 ms: the queue is drained
            }
            seen = now;
        }

        // Assert
        assertTrue(delivered.get() >= 1, "the first report was in flight and must land");
        assertTrue(delivered.get() <= TraceClient.QUEUE_CAPACITY + 1,
                "delivered " + delivered.get() + " of " + flood + "; at most the in-flight one plus a full queue may survive");
        assertTrue(delivered.get() < flood, "an unbounded queue would have delivered all " + flood);
        client.close();
        slow.stop(0);
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private static final class RecordingHandler extends Handler {
        final List<LogRecord> records = new CopyOnWriteArrayList<>();
        private final CountDownLatch first = new CountDownLatch(1);

        @Override
        public void publish(LogRecord record) {
            records.add(record);
            first.countDown();
        }

        boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return first.await(timeout, unit);
        }

        @Override public void flush() { }
        @Override public void close() { }
    }
}
