package com.example.aisearch.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public final class ElasticsearchK8sHelper {

    private ElasticsearchK8sHelper() {
    }

    public static void requireKubectl() {
        try {
            Process process = new ProcessBuilder("kubectl", "version", "--client=true")
                    .redirectErrorStream(true)
                    .start();
            int code = process.waitFor();
            if (code != 0) {
                throw new IllegalStateException("kubectl not available");
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("kubectl not available", e);
        }
    }

    public static String findEsHttpService(String namespace) throws IOException, InterruptedException {
        String output = runCommand(
                "kubectl", "get", "svc", "-n", namespace,
                "-o", "jsonpath={range .items[*]}{.metadata.name}{\"\\n\"}{end}"
        );
        // 이름이 "-es-http"로 끝나는 서비스를 우선 사용
        for (String line : output.split("\\R")) {
            if (line.endsWith("-es-http")) {
                return line.trim();
            }
        }
        return "ai-search-es-es-http";
    }

    public static String readElasticPassword(String namespace, String secretName)
            throws IOException, InterruptedException {
        // Secret에 base64로 저장된 elastic 비밀번호를 복호화해서 가져온다
        return runCommand(
                "kubectl", "get", "secret", secretName,
                "-n", namespace, "-o", "go-template={{.data.elastic | base64decode}}"
        ).trim();
    }

    public static PortForwardHandle startPortForward(
            String namespace,
            String serviceName,
            int localPort,
            int remotePort
    ) throws IOException {
        // kubectl port-forward 프로세스를 시작하고 핸들로 관리한다
        Process process = new ProcessBuilder(
                "kubectl", "port-forward",
                "-n", namespace,
                "service/" + Objects.requireNonNull(serviceName),
                localPort + ":" + remotePort
        ).redirectErrorStream(true).start();
        return new PortForwardHandle(process);
    }

    private static String runCommand(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            // 실패 시 출력 전체를 포함해 예외로 전달
            throw new IllegalStateException("command failed: " + String.join(" ", command) + "\n" + out);
        }
        return out.toString();
    }

    public record PortForwardHandle(Process process) implements AutoCloseable {
        @Override
        public void close() {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
    }
}
