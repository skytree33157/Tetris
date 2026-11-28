package network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 두 PC 사이의 네트워크 왕복 지연을 측정해 화면 표시 지연 예상치가 200ms 이하인지 확인하는 도구.
 *
 * <pre>
 * 상대 PC: java -cp build\classes\java\test network.LatencyProbe server 7777
 * 내 PC:   java -cp build\classes\java\test network.LatencyProbe client 192.168.0.10 7777 30
 * </pre>
 *
 * 화면 표시 지연 예상치 = 편도 지연 p99 + 상태 송신 주기(50ms) + 렌더 프레임(16ms).
 * 키 입력은 다음 송신 틱에 실려 나가고, 수신 측은 다음 프레임에 그리기 때문이다.
 */
public final class LatencyProbe {
    private static final int SEND_INTERVAL_MS = 50;
    private static final int FRAME_BUDGET_MS = 16;
    private static final double LIMIT_MS = 200.0;

    private LatencyProbe() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("server")) {
            runServer(Integer.parseInt(args[1]));
        } else if (args.length >= 3 && args[0].equals("client")) {
            int seconds = args.length >= 4 ? Integer.parseInt(args[3]) : 30;
            runClient(args[1], Integer.parseInt(args[2]), seconds);
        } else {
            System.out.println("사용법:");
            System.out.println("  server <포트>");
            System.out.println("  client <호스트> <포트> [측정 초=30]");
        }
    }

    private static void runServer(int port) throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("대기 중: 포트 " + port);
            while (true) {
                Socket socket = server.accept();
                socket.setTcpNoDelay(true);
                Thread echo = new Thread(() -> echo(socket), "latency-echo");
                echo.setDaemon(true);
                echo.start();
            }
        }
    }

    private static void echo(Socket socket) {
        try (socket;
                DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {
            while (true) {
                long token = input.readLong();
                output.writeLong(token);
                output.flush();
            }
        } catch (IOException ignored) {
            // 상대가 연결을 끊으면 이 연결의 에코만 종료한다.
        }
    }

    private static void runClient(String host, int port, int seconds) throws IOException, InterruptedException {
        try (Socket socket = new Socket(host, port)) {
            socket.setTcpNoDelay(true);
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            ConcurrentHashMap<Long, Long> sentAt = new ConcurrentHashMap<>();
            List<Double> oneWayMs = Collections.synchronizedList(new ArrayList<>());

            Thread reader = new Thread(() -> {
                try {
                    while (true) {
                        long token = input.readLong();
                        Long start = sentAt.remove(token);
                        if (start != null) {
                            oneWayMs.add((System.nanoTime() - start) / 2_000_000.0);
                        }
                    }
                } catch (IOException ignored) {
                    // 측정 종료 시 소켓을 닫으면 여기서 빠져나온다.
                }
            }, "latency-reader");
            reader.setDaemon(true);
            reader.start();

            System.out.printf(Locale.ROOT, "%s:%d 에 %d초 동안 %dms 간격으로 측정합니다.%n",
                    host, port, seconds, SEND_INTERVAL_MS);
            long end = System.nanoTime() + seconds * 1_000_000_000L;
            long token = 0;
            while (System.nanoTime() < end) {
                token++;
                sentAt.put(token, System.nanoTime());
                output.writeLong(token);
                output.flush();
                Thread.sleep(SEND_INTERVAL_MS);
            }
            Thread.sleep(500);
            socket.close();
            report(new ArrayList<>(oneWayMs));
        }
    }

    private static void report(List<Double> samples) {
        if (samples.isEmpty()) {
            System.out.println("응답이 하나도 없습니다. 방화벽, IP, 포트를 확인하세요.");
            return;
        }
        Collections.sort(samples);
        double min = samples.get(0);
        double max = samples.get(samples.size() - 1);
        double mean = samples.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double p95 = percentile(samples, 0.95);
        double p99 = percentile(samples, 0.99);
        double estimate = p99 + SEND_INTERVAL_MS + FRAME_BUDGET_MS;

        System.out.printf(Locale.ROOT, "샘플 %d개 (편도 지연 추정, RTT/2)%n", samples.size());
        System.out.printf(Locale.ROOT, "  최소 %.2f ms / 평균 %.2f ms / p95 %.2f ms / p99 %.2f ms / 최대 %.2f ms%n",
                min, mean, p95, p99, max);
        System.out.printf(Locale.ROOT, "화면 표시 지연 예상 (p99 기준): %.1f ms%n", estimate);
        System.out.println(estimate <= LIMIT_MS
                ? "결과: 통과 (200ms 이하)"
                : "결과: 실패 (200ms 초과)");
    }

    private static double percentile(List<Double> sorted, double p) {
        int index = (int) Math.ceil(p * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
