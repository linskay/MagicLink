package com.magiclink.core.service;

import com.magiclink.core.model.Source;
import com.magiclink.core.model.SourceType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SubscriptionFetcher implements SourceFetcher {
    private final OkHttpClient httpClient;

    @Override
    public String fetch(Source source) throws Exception {
        log.info("Fetching subscription from: {}", source.getUrl());
        Request request = new Request.Builder()
                .url(source.getUrl())
                .header("User-Agent", "v2rayNG/1.8.5") // Common UA for subscription endpoints
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("Unexpected code " + response);
            return response.body().string();
        }
    }

    @Override
    public boolean supports(Source source) {
        return source.getType() == SourceType.SUBSCRIPTION;
    }
}
