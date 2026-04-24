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
        
        // Log
        ObjectNode log = root.putObject("log");
        log.put("level", "info");

        // DNS
        ObjectNode dns = root.putObject("dns");
        ArrayNode dnsServers = dns.putArray("servers");
        ObjectNode googleDns = dnsServers.addObject();
        googleDns.put("address", "8.8.8.8");

        // Inbounds
        ArrayNode inbounds = root.putArray("inbounds");
        
        if ("FULL".equalsIgnoreCase(mode) || "TG".equalsIgnoreCase(mode)) {
            ObjectNode tun = inbounds.addObject();
            tun.put("type", "tun");
            tun.put("tag", "tun-in");
            tun.put("interface_name", "tun0");
            tun.put("inet4_address", "172.19.0.1/30");
            tun.put("auto_route", true);
            tun.put("strict_route", true);
            tun.put("stack", "system");
        }
        
        // Always provide a local proxy inbound
        ObjectNode socks = inbounds.addObject();
        socks.put("type", "socks");
        socks.put("tag", "socks-in");
        socks.put("listen", "127.0.0.1");
        socks.put("listen_port", 1080);

        // Outbounds
        ArrayNode outbounds = root.putArray("outbounds");
        
        String bestNodeTag = "proxy-direct";
        if (nodes != null && !nodes.isEmpty()) {
            Node node = nodes.get(0);
            bestNodeTag = "proxy-" + node.getId();
            ObjectNode outbound = outbounds.addObject();
            outbound.put("type", node.getType());
            outbound.put("tag", bestNodeTag);
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

        ObjectNode direct = outbounds.addObject();
        direct.put("type", "direct");
        direct.put("tag", "direct");

        // Routing
        ObjectNode route = root.putObject("route");
        ArrayNode rules = route.putArray("rules");

        if ("TG".equalsIgnoreCase(mode)) {
            ObjectNode tgRule = rules.addObject();
            ArrayNode domains = tgRule.putArray("domain");
            domains.add("telegram.org").add("t.me").add("telegram.me").add("tdesktop.com").add("telegra.ph");
            
            ArrayNode ipRanges = tgRule.putArray("ip_cidr");
            ipRanges.add("91.108.4.0/22").add("91.108.8.0/22").add("91.108.12.0/22")
                    .add("91.108.16.0/22").add("91.108.20.0/22").add("91.108.56.0/22")
                    .add("149.154.160.0/20");
            
            tgRule.put("outbound", bestNodeTag);
            
            ObjectNode defaultRule = rules.addObject();
            defaultRule.put("outbound", "direct");
        } else if ("PROXY".equalsIgnoreCase(mode)) {
            ObjectNode allRule = rules.addObject();
            allRule.put("outbound", bestNodeTag);
        } else {
            // FULL Magic - handled by auto_route in tun, but we can add an explicit fallback rule
            ObjectNode defaultRule = rules.addObject();
            defaultRule.put("outbound", bestNodeTag);
        }

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }
}
