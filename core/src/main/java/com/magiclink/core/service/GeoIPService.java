package com.magiclink.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@RequiredArgsConstructor
public class GeoIPService {
    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private static final String API_URL = "http://ip-api.com/json/";

    public String getCountryCode(String ip) {
        if (ip == null || ip.isEmpty() || "127.0.0.1".equals(ip)) {
            return "UN";
        }
        
        Request request = new Request.Builder()
                .url(API_URL + ip)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode json = mapper.readTree(response.body().string());
                if ("success".equals(json.get("status").asText())) {
                    return json.get("countryCode").asText();
                }
            }
        } catch (Exception e) {
            log.error("Failed to lookup GeoIP for " + ip, e);
        }
        return "UN";
    }
}
