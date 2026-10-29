package com.example.meetingscheduler.config;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Configures JVM-wide trust store settings before the Google GenAI HTTP client is created.
 */
final class JvmTrustStoreConfigurer {

    private JvmTrustStoreConfigurer() {
    }

    static void ensureConfigured(Environment environment) {
        if (StringUtils.hasText(System.getProperty("javax.net.ssl.trustStore"))) {
            return;
        }
        String trustStore = firstNonBlank(
                environment.getProperty("meeting.ssl.trust-store"),
                environment.getProperty("SSL_TRUST_STORE"),
                environment.getProperty("JAVAX_NET_SSL_TRUSTSTORE"));
        if (StringUtils.hasText(trustStore)) {
            String password = firstNonBlank(
                    environment.getProperty("meeting.ssl.trust-store-password"),
                    environment.getProperty("SSL_TRUST_STORE_PASSWORD"),
                    "changeit");
            System.setProperty("javax.net.ssl.trustStore", trustStore.trim());
            System.setProperty("javax.net.ssl.trustStorePassword", password);
            String type = environment.getProperty("meeting.ssl.trust-store-type", "JKS");
            System.setProperty("javax.net.ssl.trustStoreType", type);
            return;
        }
        JvmTrustStoreMerger.applyIfEnabled(environment.getProperty("meeting.ssl.merge-windows-root-certificates"));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
