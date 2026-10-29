package com.example.meetingscheduler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Locale;

/**
 * Builds a JVM trust store that includes the JDK cacerts plus Windows ROOT CAs
 * (corporate TLS inspection). Required for HTTPS clients that do not use the Windows cert store.
 */
final class JvmTrustStoreMerger {

    private static final Logger log = LoggerFactory.getLogger(JvmTrustStoreMerger.class);
    private static final char[] STORE_PASSWORD = "changeit".toCharArray();

    private JvmTrustStoreMerger() {
    }

    static void applyIfEnabled(String mergeProperty) {
        if (!isEnabled(mergeProperty)) {
            return;
        }
        try {
            Path trustStorePath = buildMergedTrustStore();
            System.setProperty("javax.net.ssl.trustStore", trustStorePath.toAbsolutePath().toString());
            System.setProperty("javax.net.ssl.trustStorePassword", new String(STORE_PASSWORD));
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");
            log.info("Using merged JVM trust store (JDK cacerts + Windows ROOT): {}", trustStorePath);
        } catch (Exception ex) {
            log.warn("Could not build merged JVM trust store; Gemini HTTPS may fail with PKIX errors: {}", ex.toString());
        }
    }

    private static boolean isEnabled(String mergeProperty) {
        if (StringUtils.hasText(mergeProperty)) {
            return Boolean.parseBoolean(mergeProperty.trim());
        }
        return isWindows();
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase(Locale.ROOT).contains("win");
    }

    private static Path buildMergedTrustStore() throws Exception {
        KeyStore merged = KeyStore.getInstance("JKS");
        merged.load(null, null);

        importJdkCacerts(merged);
        if (isWindows()) {
            importWindowsRootCertificates(merged);
        }

        Path output = Files.createTempFile("meeting-scheduler-truststore-", ".jks");
        output.toFile().deleteOnExit();
        try (OutputStream out = Files.newOutputStream(output)) {
            merged.store(out, STORE_PASSWORD);
        }
        return output;
    }

    private static void importJdkCacerts(KeyStore merged) throws Exception {
        Path cacerts = Path.of(System.getProperty("java.home"), "lib", "security", "cacerts");
        if (!Files.isRegularFile(cacerts)) {
            log.debug("JDK cacerts not found at {}", cacerts);
            return;
        }
        KeyStore jdk = KeyStore.getInstance("JKS");
        try (InputStream in = Files.newInputStream(cacerts)) {
            jdk.load(in, STORE_PASSWORD);
        }
        copyCertificateEntries(jdk, merged, "jdk-");
    }

    private static void importWindowsRootCertificates(KeyStore merged) {
        try {
            KeyStore windowsRoot = KeyStore.getInstance("Windows-ROOT", "SunMSCAPI");
            windowsRoot.load(null, null);
            copyCertificateEntries(windowsRoot, merged, "win-root-");
        } catch (Exception ex) {
            log.warn("Windows-ROOT certificates could not be loaded: {}", ex.toString());
        }
    }

    private static void copyCertificateEntries(KeyStore source, KeyStore target, String aliasPrefix)
            throws KeyStoreException {
        Enumeration<String> aliases = source.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (!source.isCertificateEntry(alias)) {
                continue;
            }
            Certificate certificate = source.getCertificate(alias);
            if (certificate == null) {
                continue;
            }
            String targetAlias = uniqueAlias(target, aliasPrefix + sanitizeAlias(alias));
            target.setCertificateEntry(targetAlias, certificate);
        }
    }

    private static String uniqueAlias(KeyStore target, String baseAlias) throws KeyStoreException {
        String candidate = baseAlias;
        int suffix = 1;
        while (target.containsAlias(candidate)) {
            candidate = baseAlias + "-" + suffix++;
        }
        return candidate;
    }

    private static String sanitizeAlias(String alias) {
        return alias.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
