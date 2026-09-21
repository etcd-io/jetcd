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

package io.etcd.jetcd.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A loopback TCP server that closes every connection it accepts until {@link #startForwarding()} is called, after
 * which it forwards each accepted connection to the target address. It counts the connections it has accepted so a
 * test can observe the connection attempts made by a client.
 */
final class TcpProxy implements AutoCloseable {

    private static final int BUFFER_SIZE = 8192;

    private final ServerSocket serverSocket;
    private final InetSocketAddress target;
    private final AtomicInteger acceptedConnections;
    private final List<Socket> openSockets;
    private volatile boolean forwarding;
    private volatile boolean closed;

    TcpProxy(String targetHost, int targetPort) throws IOException {
        this.serverSocket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        this.target = new InetSocketAddress(targetHost, targetPort);
        this.acceptedConnections = new AtomicInteger();
        this.openSockets = Collections.synchronizedList(new ArrayList<>());

        final Thread acceptor = new Thread(this::acceptLoop, "tcp-proxy-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    /**
     * @return the endpoint clients should connect to.
     */
    String endpoint() {
        return "http://" + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort();
    }

    /**
     * @return the number of connections accepted so far, counted once the connection has been either closed or
     *         forwarded.
     */
    int acceptedConnections() {
        return acceptedConnections.get();
    }

    /**
     * Forwards every connection accepted from now on to the target address.
     */
    void startForwarding() {
        this.forwarding = true;
    }

    @Override
    public void close() {
        this.closed = true;

        TestUtil.closeQuietly(serverSocket);
        synchronized (openSockets) {
            openSockets.forEach(TestUtil::closeQuietly);
        }
    }

    private void acceptLoop() {
        while (!closed) {
            try {
                final Socket client = serverSocket.accept();
                if (forwarding) {
                    forward(client);
                } else {
                    client.close();
                }
                acceptedConnections.incrementAndGet();
            } catch (IOException ignored) {
                // the server socket has been closed or the connection could not be established
            }
        }
    }

    private void forward(Socket client) throws IOException {
        final Socket upstream = new Socket();
        upstream.connect(target);

        openSockets.add(client);
        openSockets.add(upstream);

        pump(client, upstream);
        pump(upstream, client);
    }

    private static void pump(Socket from, Socket to) {
        final Thread thread = new Thread(() -> {
            final byte[] buffer = new byte[BUFFER_SIZE];
            try {
                final InputStream in = from.getInputStream();
                final OutputStream out = to.getOutputStream();
                for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
                    out.write(buffer, 0, read);
                    out.flush();
                }
            } catch (IOException ignored) {
                // the connection has been closed
            } finally {
                TestUtil.closeQuietly(from);
                TestUtil.closeQuietly(to);
            }
        }, "tcp-proxy-pump");
        thread.setDaemon(true);
        thread.start();
    }
}
