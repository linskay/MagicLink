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

    public String generateConfig(List<Node> nodes, String mode) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        
        // Outbounds
        ArrayNode outbounds = root.putArray("outbounds");
        
        for (Node node : nodes) {
            ObjectNode outbound = outbounds.addObject();
            outbound.put("type", node.getType());
            outbound.put("tag", "proxy-" + node.getId());
            outbound.put("server", node.getHost());
            outbound.put("server_port", node.getPort());
            
            if (node.getParams() != null) {
                node.getParams().forEach((k, v) -> {
                    if (v instanceof String) outbound.put(k, (String) v);
                    else if (v instanceof Integer) outbound.put(k, (Integer) v);
                    else if (v instanceof Boolean) outbound.put(k, (Boolean) v);
                });
            }
        }

        // Add direct and block outbounds
        ObjectNode direct = outbounds.addObject();
        direct.put("type", "direct");
        direct.put("tag", "direct");

        ObjectNode dns = root.putObject("dns");
        ArrayNode dnsServers = dns.putArray("servers");
        ObjectNode googleDns = dnsServers.addObject();
        googleDns.put("address", "8.8.8.8");

        // Routing
        ObjectNode route = root.putObject("route");
        ArrayNode rules = route.putArray("rules");

        if ("TG".equalsIgnoreCase(mode)) {
            ObjectNode tgRule = rules.addObject();
            ArrayNode domains = tgRule.putArray("domain");
            domains.add("telegram.org").add("t.me").add("telegram.me").add("tdesktop.com");
            tgRule.put("outbound", "proxy-" + nodes.get(0).getId());
            
            ObjectNode defaultRule = rules.addObject();
            defaultRule.put("outbound", "direct");
        }

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }
}
