package dev.gmpark.cors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;

@SpringBootApplication
public class CorsApplication {

    public static int AI_SERVER_PORT = 8000; // 기본값

    public static void main(String[] args) {
        SpringApplication.run(CorsApplication.class, args);
    }

    @Bean
    public CommandLineRunner runAiServer() {
        return args -> {
            new Thread(() -> {
                try {
                    // 1. 빈 포트 찾기
                    AI_SERVER_PORT = findAvailablePort(8000, 8100);
                    System.out.println("🚀 [AI Server] Starting Python AI Server on port " + AI_SERVER_PORT + "...");

                    // 2. Python 서버 실행 (포트 번호를 인자로 전달)
                    ProcessBuilder builder = new ProcessBuilder("python", "ai-server/main.py", String.valueOf(AI_SERVER_PORT));
                    builder.redirectErrorStream(true);
                    Process process = builder.start();

                    // Python 서버 로그 출력
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("[AI Server Log] " + line);
                        }
                    }

                    int exitCode = process.waitFor();
                    System.out.println("🛑 [AI Server] Exited with code: " + exitCode);

                } catch (Exception e) {
                    System.out.println("⚠ [AI Server] 자동 실행 실패. 수동으로 'python ai-server/main.py " + AI_SERVER_PORT + "'를 실행해주세요.");
                    System.out.println("Reason: " + e.getMessage());
                }
            }).start();
        };
    }

    private int findAvailablePort(int start, int end) {
        for (int port = start; port <= end; port++) {
            try (ServerSocket socket = new ServerSocket(port)) {
                return port;
            } catch (Exception ignored) {
                // 포트 사용 중
            }
        }
        throw new RuntimeException("No available port found for AI Server");
    }
}