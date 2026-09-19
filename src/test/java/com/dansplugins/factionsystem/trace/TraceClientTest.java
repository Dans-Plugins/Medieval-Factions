package com.dansplugins.factionsystem.trace;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
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

    // What the client sees as its environment. Empty unless a test says
    // otherwise, so a DO_NOT_TRACK on the machine running the suite cannot
    // fail the tests that expect an enabled client.
    private final Map<String, String> environment = new HashMap<>();
    private Function<String, String> realEnvironment;

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
    void isolateEnvironment() {
        realEnvironment = TraceClient.environment;
        TraceClient.environment = environment::get;
    }

    @AfterEach
    void restoreEnvironment() {
        TraceClient.environment = realEnvironment;
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

    @Test
    void close_sendsWhatWasJustQueuedBeforeStopping() throws Exception {
        // A CLI reports once and exits at once. Without draining, the event
        // races the sender thread and is lost a good fraction of the time; 30
        // back-to-back report()+close() pairs make that fraction visible.
        for (int i = 0; i < 30; i++) {
            TraceClient client = TraceClient.builder(baseUrl(), "MyCli").key("k").build();
            client.report("startup", null, Collections.singletonMap("run", String.valueOf(i)));
            client.close();
        }
        assertEquals(30, received.size(), "every report()+close() pair must deliver");
    }

    @Test
    void close_stillReturnsWithinTheTimeoutWhenTheServerHangs() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        HttpServer slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slow.createContext("/", exchange -> {
            try { release.await(15, TimeUnit.SECONDS); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        slow.start();
        TraceClient client = TraceClient.builder("http://127.0.0.1:" + slow.getAddress().getPort(), "MyCli").key("k").build();
        client.report("startup");

        long before = System.nanoTime();
        client.close();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);

        assertTrue(elapsedMs < 7_000, "close() took " + elapsedMs + " ms; draining must be bounded by the timeout");
        release.countDown();
        slow.stop(0);
    }

    @Test
    void disabledClient_saysWhy() {
        assertEquals(TraceClient.REASON_CONFIG, TraceClient.builder(baseUrl(), "MyPlugin").key("k").enabled(false).build().disabledReason());
        assertEquals(TraceClient.REASON_NO_KEY, TraceClient.builder(baseUrl(), "MyPlugin").build().disabledReason());
        assertEquals(TraceClient.REASON_NO_KEY, TraceClient.builder(baseUrl(), "MyPlugin").key("  ").build().disabledReason());
        assertNull(TraceClient.builder(baseUrl(), "MyPlugin").key("k").build().disabledReason(), "an enabled client has no reason");
    }

    @Test
    void serverWideConfig_isCreatedWithTheExactContentWhenMissing(@TempDir Path plugins) throws Exception {
        // Arrange
        File pluginsDirectory = plugins.toFile();
        Path file = plugins.resolve("trace").resolve("config.yml");
        assertFalse(Files.exists(file));

        // Act
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin").key("k")
                .serverWideConfig(pluginsDirectory).build();

        // Assert
        assertTrue(Files.exists(file), "plugins/trace/config.yml should have been created");
        String expected = "# Server-wide switch for usage reporting by plugins that report to trace\n"
                + "# (https://github.com/Stephenson-Software/trace#usage-reporting).\n"
                + "# Set enabled to false and every such plugin on this server stops reporting,\n"
                + "# regardless of its own usage-reporting.enabled setting. Plugins never turn\n"
                + "# this back on.\n"
                + "enabled: true\n";
        assertEquals(expected, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        assertTrue(client.isEnabled(), "a freshly created switch file means enabled");
        assertNull(client.disabledReason());
        client.close();
    }

    @Test
    void serverWideConfig_enabledFalseDisablesWithTheServerWideReason(@TempDir Path plugins) throws Exception {
        // Arrange
        Path file = plugins.resolve("trace").resolve("config.yml");
        Files.createDirectories(file.getParent());
        String operatorsFile = "# my notes\n  enabled :   False   # turned off by the operator\nother: true\n";
        Files.write(file, operatorsFile.getBytes(StandardCharsets.UTF_8));

        // Act
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin").key("k").enabled(true)
                .serverWideConfig(plugins.toFile()).build();
        client.report("startup");
        client.close();

        // Assert
        assertFalse(client.isEnabled());
        assertEquals("server-wide config: plugins/trace/config.yml", client.disabledReason());
        assertFalse(arrived.await(300, TimeUnit.MILLISECONDS), "nothing should have been sent");
        assertEquals(operatorsFile, new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
                "an existing switch file is never rewritten");
    }

    @Test
    void serverWideConfig_acceptsEverySpellingOfOff(@TempDir Path plugins) throws Exception {
        Path file = plugins.resolve("trace").resolve("config.yml");
        Files.createDirectories(file.getParent());
        for (String off : new String[] {"false", "no", "0", "off", "OFF", "No"}) {
            Files.write(file, ("enabled: " + off + "\n").getBytes(StandardCharsets.UTF_8));
            assertEquals(TraceClient.REASON_SERVER_WIDE,
                    TraceClient.builder(baseUrl(), "MyPlugin").key("k").serverWideConfig(plugins.toFile()).build().disabledReason(),
                    "enabled: " + off + " should disable");
        }
        for (String on : new String[] {"true", "yes", "1", "on", "anything-else"}) {
            Files.write(file, ("enabled: " + on + "\n").getBytes(StandardCharsets.UTF_8));
            assertNull(TraceClient.builder(baseUrl(), "MyPlugin").key("k").serverWideConfig(plugins.toFile()).build().disabledReason(),
                    "enabled: " + on + " should not disable");
        }
        Files.write(file, "# nothing here\n".getBytes(StandardCharsets.UTF_8));
        assertNull(TraceClient.builder(baseUrl(), "MyPlugin").key("k").serverWideConfig(plugins.toFile()).build().disabledReason(),
                "a file without an enabled: line means enabled");
    }

    @Test
    void environment_disablesAndWinsOverTheServerWideFile(@TempDir Path plugins) throws Exception {
        // Arrange
        // The file says on; the environment says off. The environment wins,
        // and is the reason given.
        TraceClient.builder(baseUrl(), "MyPlugin").key("k").serverWideConfig(plugins.toFile()).build().close();
        assertEquals("enabled: true\n", lastLine(plugins.resolve("trace").resolve("config.yml")));

        for (String off : new String[] {"off", "OFF", "false", "0", "no", " No "}) {
            environment.clear();
            environment.put("TRACE_USAGE_REPORTING", off);
            TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin").key("k").serverWideConfig(plugins.toFile()).build();
            assertFalse(client.isEnabled(), "TRACE_USAGE_REPORTING=" + off + " should disable");
            assertEquals("environment", client.disabledReason());
            client.report("startup");
            client.close();
        }
        for (String yes : new String[] {"1", "true", "TRUE", "yes"}) {
            environment.clear();
            environment.put("DO_NOT_TRACK", yes);
            TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin").key("k").serverWideConfig(plugins.toFile()).build();
            assertFalse(client.isEnabled(), "DO_NOT_TRACK=" + yes + " should disable");
            assertEquals("environment", client.disabledReason());
            client.report("startup");
            client.close();
        }
        assertFalse(arrived.await(300, TimeUnit.MILLISECONDS), "nothing should have been sent");

        // Values that are not an opt-out leave the client alone.
        environment.clear();
        environment.put("TRACE_USAGE_REPORTING", "on");
        environment.put("DO_NOT_TRACK", "0");
        assertNull(TraceClient.builder(baseUrl(), "MyPlugin").key("k").serverWideConfig(plugins.toFile()).build().disabledReason());
    }

    @Test
    void disabledReason_followsThePrecedenceEnvironmentThenServerWideThenConfigThenKey(@TempDir Path plugins) throws Exception {
        // Arrange: everything says off at once.
        Path file = plugins.resolve("trace").resolve("config.yml");
        Files.createDirectories(file.getParent());
        Files.write(file, "enabled: false\n".getBytes(StandardCharsets.UTF_8));
        environment.put("DO_NOT_TRACK", "1");
        File pluginsDirectory = plugins.toFile();

        // Act + Assert: peel the reasons off one at a time, in order.
        assertEquals("environment",
                TraceClient.builder(baseUrl(), "MyPlugin").enabled(false).serverWideConfig(pluginsDirectory).build().disabledReason());
        environment.clear();
        assertEquals("server-wide config: plugins/trace/config.yml",
                TraceClient.builder(baseUrl(), "MyPlugin").enabled(false).serverWideConfig(pluginsDirectory).build().disabledReason());
        Files.write(file, "enabled: true\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("config.yml",
                TraceClient.builder(baseUrl(), "MyPlugin").enabled(false).serverWideConfig(pluginsDirectory).build().disabledReason());
        assertEquals("no key",
                TraceClient.builder(baseUrl(), "MyPlugin").enabled(true).serverWideConfig(pluginsDirectory).build().disabledReason());
        TraceClient enabled = TraceClient.builder(baseUrl(), "MyPlugin").key("k").enabled(true).serverWideConfig(pluginsDirectory).build();
        assertNull(enabled.disabledReason());
        assertTrue(enabled.isEnabled());
        enabled.close();
    }

    @Test
    void serverWideConfig_ioFailureIsLoggedFineAndTreatedAsEnabled(@TempDir Path scratch) throws Exception {
        // Arrange
        // A "plugins directory" that is a regular file: trace/config.yml can
        // be neither created nor read under it. (Permission bits are no use
        // here -- the suite may run as root.)
        Path notADirectory = scratch.resolve("plugins");
        Files.write(notADirectory, "not a directory".getBytes(StandardCharsets.UTF_8));
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.serverWideIo");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);

        // Act
        TraceClient client = assertDoesNotThrow(() -> TraceClient.builder(baseUrl(), "MyPlugin").key("k")
                .serverWideConfig(notADirectory.toFile()).logger(logger).build());

        // Assert
        assertTrue(client.isEnabled(), "a switch file that cannot be handled must not turn reporting off");
        assertNull(client.disabledReason());
        assertTrue(log.await(1, TimeUnit.SECONDS), "the failure should be mentioned at FINE");
        assertEquals(Level.FINE, log.records.get(0).getLevel());
        assertTrue(log.records.get(0).getMessage().contains("server-wide config"), log.records.get(0).getMessage());
        client.report("startup");
        assertTrue(arrived.await(5, TimeUnit.SECONDS), "and it still reports");
        client.close();
    }

    private static String lastLine(Path file) throws java.io.IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        return lines.get(lines.size() - 1) + "\n";
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
