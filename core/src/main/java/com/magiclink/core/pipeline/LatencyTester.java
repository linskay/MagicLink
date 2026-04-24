package com.magiclink.core.pipeline;

import com.magiclink.core.model.Node;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class LatencyTester implements PipelineStep {
    private static final int TIMEOUT_MS = 1000;
    private static final int MAX_LATENCY_MS = 500;

    @Override
    public List<Node> process(List<Node> nodes) {
        log.info("Starting latency test for {} nodes", nodes.size());
        return nodes.parallelStream()
                .peek(node -> node.setLatency(measureLatency(node.getHost(), node.getPort())))
                .filter(node -> node.getLatency() > 0 && node.getLatency() <= MAX_LATENCY_MS)
                .collect(Collectors.toList());
    }

    private long measureLatency(String host, int port) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            return -1; // Timeout or error
        }
    }
}
