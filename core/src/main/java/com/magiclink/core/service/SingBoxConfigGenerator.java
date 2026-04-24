package com.magiclink.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.magiclink.core.model.Node;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class SingBoxConfigGenerator {
    private final ObjectMapper objectMapper;

    public String generateConfig(List<Node> nodes) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        
        // Outbounds
        ArrayNode outbounds = root.putArray("outbounds");
        
        for (Node node : nodes) {
            ObjectNode outbound = outbounds.addObject();
            outbound.put("type", node.getType());
            outbound.put("tag", node.getId());
            outbound.put("server", node.getHost());
            outbound.put("server_port", node.getPort());
            
            // Add specific params
            if (node.getParams() != null) {
                node.getParams().forEach((k, v) -> {
                    if (v instanceof String) outbound.put(k, (String) v);
                    else if (v instanceof Integer) outbound.put(k, (Integer) v);
                    else if (v instanceof Boolean) outbound.put(k, (Boolean) v);
                });
            }
        }

        // Add default direct/block outbounds
        ObjectNode direct = outbounds.addObject();
        direct.put("type", "direct");
        direct.put("tag", "direct");

        ObjectNode dns = root.putObject("dns");
        ArrayNode dnsServers = dns.putArray("servers");
        ObjectNode googleDns = dnsServers.addObject();
        googleDns.put("address", "8.8.8.8");

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }
}
