package com.magiclink.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiclink.core.model.Source;
import com.magiclink.core.model.SourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@RequiredArgsConstructor
public class GitHubReleaseFetcher implements SourceFetcher {
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Override
    public String fetch(Source source) throws Exception {
        // api.github.com/repos/{owner}/{repo}/releases/latest
        String apiUrl = source.getUrl().replace("github.com", "api.github.com/repos") + "/releases/latest";
        log.info("Fetching latest release from: {}", apiUrl);

        Request request = new Request.Builder().url(apiUrl).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("GitHub API error: " + response.code());
            JsonNode root = objectMapper.readTree(response.body().string());
            JsonNode assets = root.get("assets");
            
            StringBuilder combinedContent = new StringBuilder();
            if (assets != null && assets.isArray()) {
                for (JsonNode asset : assets) {
                    String name = asset.get("name").asText();
                    if (matchesPattern(name, source.getPattern())) {
                        String downloadUrl = asset.get("browser_download_url").asText();
                        combinedContent.append(fetchRaw(downloadUrl)).append("\n");
                    }
                }
            }
            return combinedContent.toString();
        }
    }

    private String fetchRaw(String url) throws Exception {
        Request request = new Request.Builder().url(url).build();
        try (Response response = httpClient.newCall(request).execute()) {
            return response.body().string();
        }
    }

    private boolean matchesPattern(String name, String pattern) {
        if (pattern == null || pattern.isEmpty()) return true;
        return name.matches(pattern.replace("*", ".*"));
    }

    @Override
    public boolean supports(Source source) {
        return source.getType() == SourceType.GITHUB_RELEASE;
    }
}
