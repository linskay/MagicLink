package com.magiclink.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Node {
    private String id;
    private String type; // vless, trojan, vmess, ss, wireguard, socks, http
    private String host;
    private int port;
    private String country;
    private String source;
    private long latency; // in ms
    private Map<String, Object> params;
}
