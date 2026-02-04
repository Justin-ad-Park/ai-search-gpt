package com.example.aisearch.support;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class ElasticsearchDirectExecutionSetup {

    private ElasticsearchDirectExecutionSetup() {
    }

    public static SetupResult setup() {
        try {
            // 로컬 테스트용으로 HTTPS 검증을 완화 (운영 환경에서는 사용 금지)
            trustAllHttpsForLocalOnly();

            // 기본 사용자명 설정
            setIfMissing("AI_SEARCH_ES_USERNAME", "elastic");

            // 비밀번호가 없으면 k8s Secret에서 읽어 설정
            if (isBlank(System.getProperty("AI_SEARCH_ES_PASSWORD")) && isBlank(System.getenv("AI_SEARCH_ES_PASSWORD"))) {
                String password = runCommand(
                        "kubectl", "get", "secret", "ai-search-es-es-elastic-user",
                        "-n", "ai-search", "-o", "go-template={{.data.elastic | base64decode}}"
                ).trim();
                setIfMissing("AI_SEARCH_ES_PASSWORD", password);
            }

            // ES URL이 없으면 로컬 포트포워딩을 열고 설정
            if (isBlank(System.getProperty("AI_SEARCH_ES_URL")) && isBlank(System.getenv("AI_SEARCH_ES_URL"))) {
                String service = findElasticsearchHttpService();
                Process portForwardProcess = new ProcessBuilder(
                        "kubectl", "port-forward", "-n", "ai-search", "service/" + service, "9200:9200"
                ).redirectErrorStream(true).start();
                Thread.sleep(3000L);
                setIfMissing("AI_SEARCH_ES_URL", "http://localhost:9200");
                return new SetupResult(portForwardProcess);
            }

            return new SetupResult(null);
        } catch (Exception e) {
            throw new RuntimeException("Elasticsearch 직접 실행을 위한 사전 설정 실패", e);
        }
    }

    public static void cleanup(SetupResult result) {
        if (result == null) {
            return;
        }
        Process process = result.portForwardProcess();
        if (process != null && process.isAlive()) {
            // 테스트 종료 시 포트포워딩 프로세스 종료
            process.destroy();
        }
    }

    private static String findElasticsearchHttpService() throws IOException, InterruptedException {
        // 네임스페이스 내 -es-http 서비스 자동 탐지
        String output = runCommand(
                "kubectl", "get", "svc", "-n", "ai-search",
                "-o", "jsonpath={range .items[*]}{.metadata.name}{\"\\n\"}{end}"
        );
        for (String line : output.split("\\R")) {
            if (line.endsWith("-es-http")) {
                return line.trim();
            }
        }
        return "ai-search-es-es-http";
    }

    private static void trustAllHttpsForLocalOnly() throws Exception {
        // 모든 인증서를 신뢰하도록 SSLContext 설정 (로컬 테스트용)
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
    }

    private static void setIfMissing(String key, String value) {
        // 시스템 속성/환경변수에 값이 없을 때만 세팅
        if (isBlank(System.getProperty(key)) && isBlank(System.getenv(key))) {
            System.setProperty(key, value);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String runCommand(String... command) throws IOException, InterruptedException {
        // 외부 커맨드를 실행해 결과를 문자열로 반환
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
            throw new IllegalStateException("command failed: " + String.join(" ", command) + "\n" + out);
        }
        return out.toString();
    }

    public record SetupResult(Process portForwardProcess) {
    }
}
