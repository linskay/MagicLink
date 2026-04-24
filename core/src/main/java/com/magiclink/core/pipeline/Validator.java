package com.magiclink.core.pipeline;

import com.magiclink.core.model.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Validator implements PipelineStep {
    @Override
    public List<Node> process(List<Node> nodes) {
        return nodes.stream()
                .filter(this::isValid)
                .collect(Collectors.toList());
    }

    private boolean isValid(Node node) {
        if (node.getHost() == null || node.getHost().isEmpty()) return false;
        if (node.getPort() <= 0 || node.getPort() > 65535) return false;
        if (node.getType() == null || node.getType().isEmpty()) return false;
        
        // Basic protocol validation
        String type = node.getType().toLowerCase();
        return type.equals("vless") || type.equals("vmess") || type.equals("trojan") || 
               type.equals("ss") || type.equals("wireguard") || type.equals("socks") || type.equals("http");
    }
}
