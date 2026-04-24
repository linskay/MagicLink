package com.magiclink.core.service;

import com.magiclink.core.model.Node;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Base64Parser implements ConfigParser {
    private static final Pattern VMESS_PATTERN = Pattern.compile("vmess://([a-zA-Z0-9+/=]+)");
    private static final Pattern VLESS_PATTERN = Pattern.compile("vless://([a-zA-Z0-9-@?#:%._+~#=]+)");

    @Override
    public List<Node> parse(String content, String sourceId) {
        List<Node> nodes = new ArrayList<>();
        try {
            String decoded = new String(Base64.getMimeDecoder().decode(content.trim()), StandardCharsets.UTF_8);
            String[] lines = decoded.split("\\r?\\n");
            for (String line : lines) {
                Node node = parseLine(line, sourceId);
                if (node != null) nodes.add(node);
            }
        } catch (Exception e) {
            log.error("Failed to decode base64 content", e);
        }
        return nodes;
    }

    @Override
    public boolean canParse(String content) {
        try {
            Base64.getMimeDecoder().decode(content.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Node parseLine(String line, String sourceId) {
        if (line.startsWith("vmess://")) {
            // Simplified VMess parsing (usually JSON inside base64)
            return Node.builder().type("vmess").source(sourceId).build(); 
        } else if (line.startsWith("vless://")) {
            // Simplified VLESS parsing
            return Node.builder().type("vless").source(sourceId).build();
        }
        return null;
    }
}
