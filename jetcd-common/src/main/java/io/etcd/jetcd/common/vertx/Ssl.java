/*
 * Copyright 2016-2021 The jetcd authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.etcd.jetcd.common.vertx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;

/**
 * SSL/TLS configuration utilities for Vert.x HttpClientOptions.
 *
 * <p>
 * This class provides static factory methods that return {@link Consumer} instances for configuring
 * {@link HttpClientOptions} with SSL/TLS settings. The returned consumers can be composed
 * using {@link Consumer#andThen(Consumer)} for flexible configuration.
 * </p>
 *
 * <p>
 * All methods in this class enable SSL, ALPN (required for HTTP/2), and configure trust managers
 * and/or key managers with PEM-formatted certificates.
 * </p>
 *
 * <p>
 * Example usage with custom SSL configuration:
 * </p>
 *
 * <pre>
 * Client client = Client.builder("https://localhost:2379")
 *     .httpClientOptions(Ssl.withTrustManager(caFile)
 *         .andThen(options -> options.setVerifyHost(false)))
 *     .build();
 * </pre>
 *
 * <p>
 * Example usage with mutual TLS (mTLS):
 * </p>
 *
 * <pre>
 * Client client = Client.builder("https://localhost:2379")
 *     .httpClientOptions(Ssl.withTrustAndKeyManager(
 *         new File("/path/to/ca.pem"),
 *         new File("/path/to/client-cert.pem"),
 *         new File("/path/to/client-key.pem")))
 *     .build();
 * </pre>
 */
public final class Ssl {

    private Ssl() {
        // Utility class, prevent instantiation
    }

    /**
     * Creates a consumer that configures SSL/TLS with a CA certificate file.
     *
     * <p>
     * The returned consumer configures:
     * </p>
     * <ul>
     * <li>SSL enabled</li>
     * <li>ALPN enabled (required for HTTP/2)</li>
     * <li>Trust manager with the specified CA certificate</li>
     * </ul>
     *
     * <p>
     * Example:
     * </p>
     *
     * <pre>
     * Client client = Client.builder("https://localhost:2379")
     *     .httpClientOptions(Ssl.withTrustManager(new File("/path/to/ca.pem")))
     *     .build();
     * </pre>
     *
     * @param  caFile the CA certificate file in PEM format
     * @return        a consumer that configures HttpClientOptions with SSL/TLS
     */
    public static Consumer<HttpClientOptions> withTrustManager(File caFile) {
        return withTrustManager(caFile.getAbsolutePath());
    }

    /**
     * Creates a consumer that configures SSL/TLS with a CA certificate file path.
     *
     * <p>
     * The returned consumer configures:
     * </p>
     * <ul>
     * <li>SSL enabled</li>
     * <li>ALPN enabled (required for HTTP/2)</li>
     * <li>Trust manager with the specified CA certificate</li>
     * </ul>
     *
     * @param  caFilePath the path to the CA certificate file in PEM format
     * @return            a consumer that configures HttpClientOptions with SSL/TLS
     */
    public static Consumer<HttpClientOptions> withTrustManager(String caFilePath) {
        return options -> options
            .setSsl(true)
            .setUseAlpn(true)
            .setTrustOptions(new PemTrustOptions().addCertPath(caFilePath));
    }

    /**
     * Creates a consumer that configures SSL/TLS with a CA certificate from an InputStream.
     *
     * <p>
     * The returned consumer configures:
     * </p>
     * <ul>
     * <li>SSL enabled</li>
     * <li>ALPN enabled (required for HTTP/2)</li>
     * <li>Trust manager with the CA certificate from the stream</li>
     * </ul>
     *
     * <p>
     * Useful for loading certificates from classpath resources.
     * </p>
     *
     * <p>
     * Example:
     * </p>
     *
     * <pre>
     * try (InputStream is = getClass().getResourceAsStream("/ssl/ca.pem")) {
     *     Client client = Client.builder("https://localhost:2379")
     *         .httpClientOptions(Ssl.withTrustManager(is))
     *         .build();
     * }
     * </pre>
     *
     * @param  caInputStream the InputStream containing the CA certificate in PEM format
     * @return               a consumer that configures HttpClientOptions with SSL/TLS
     * @throws IOException   if reading from the InputStream fails
     */
    public static Consumer<HttpClientOptions> withTrustManager(InputStream caInputStream) throws IOException {
        byte[] certBytes = caInputStream.readAllBytes();
        return options -> options
            .setSsl(true)
            .setUseAlpn(true)
            .setTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(certBytes)));
    }

    /**
     * Creates a consumer that configures SSL/TLS with both CA certificate and client certificate/key for mutual TLS
     * (mTLS).
     *
     * <p>
     * The returned consumer configures:
     * </p>
     * <ul>
     * <li>SSL enabled</li>
     * <li>ALPN enabled (required for HTTP/2)</li>
     * <li>Trust manager with the CA certificate</li>
     * <li>Key manager with the client certificate and private key</li>
     * </ul>
     *
     * <p>
     * Example:
     * </p>
     *
     * <pre>
     * Client client = Client.builder("https://localhost:2379")
     *     .httpClientOptions(Ssl.withTrustAndKeyManager(
     *         new File("/path/to/ca.pem"),
     *         new File("/path/to/client-cert.pem"),
     *         new File("/path/to/client-key.pem")))
     *     .build();
     * </pre>
     *
     * @param  caFile         the CA certificate file in PEM format
     * @param  clientCertFile the client certificate file in PEM format
     * @param  clientKeyFile  the client private key file in PEM format
     * @return                a consumer that configures HttpClientOptions with mTLS
     */
    public static Consumer<HttpClientOptions> withTrustAndKeyManager(File caFile, File clientCertFile, File clientKeyFile) {
        return withTrustAndKeyManager(
            caFile.getAbsolutePath(),
            clientCertFile.getAbsolutePath(),
            clientKeyFile.getAbsolutePath());
    }

    /**
     * Creates a consumer that configures SSL/TLS with both CA certificate and client certificate/key for mutual TLS
     * (mTLS).
     *
     * <p>
     * The returned consumer configures:
     * </p>
     * <ul>
     * <li>SSL enabled</li>
     * <li>ALPN enabled (required for HTTP/2)</li>
     * <li>Trust manager with the CA certificate</li>
     * <li>Key manager with the client certificate and private key</li>
     * </ul>
     *
     * @param  caFilePath     the path to the CA certificate file in PEM format
     * @param  clientCertPath the path to the client certificate file in PEM format
     * @param  clientKeyPath  the path to the client private key file in PEM format
     * @return                a consumer that configures HttpClientOptions with mTLS
     */
    public static Consumer<HttpClientOptions> withTrustAndKeyManager(String caFilePath, String clientCertPath,
        String clientKeyPath) {
        return options -> options
            .setSsl(true)
            .setUseAlpn(true)
            .setTrustOptions(new PemTrustOptions().addCertPath(caFilePath))
            .setKeyCertOptions(new PemKeyCertOptions()
                .setCertPath(clientCertPath)
                .setKeyPath(clientKeyPath));
    }
}
