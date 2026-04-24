package com.magiclink.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiclink.core.pipeline.*;
import com.magiclink.core.repository.DatabaseManager;
import com.magiclink.core.repository.SQLiteNodeRepository;
import com.magiclink.core.service.*;
import okhttp3.OkHttpClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class MagicLinkCore {
    public static void main(String[] args) {
        log.info("Starting MagicLink Core Subsystem...");

        // 1. Initialize Infrastructure
        ObjectMapper mapper = new ObjectMapper();
        OkHttpClient client = new OkHttpClient();
        DatabaseManager dbManager = new DatabaseManager();
        dbManager.initialize();

        SQLiteNodeRepository nodeRepo = new SQLiteNodeRepository(dbManager);

        // 2. Setup Pipeline
        PipelineProcessor processor = new PipelineProcessor(nodeRepo);
        processor.addStep(new Validator());
        processor.addStep(new Deduplicator());
        processor.addStep(new LatencyTester());
        processor.addStep(new Tagger());

        // 3. Setup Fetchers and Parsers
        SourceFetcher githubRaw = new GitHubRawFetcher(client);
        SourceFetcher githubRepo = new GitHubRepoFetcher(client, mapper);
        SourceFetcher githubRelease = new GitHubReleaseFetcher(client, mapper);
        SourceFetcher subFetcher = new SubscriptionFetcher(client);

        ConfigParser b64Parser = new Base64Parser();
        // Add more parsers as needed

        // 4. Setup Service
        SourceServiceImpl sourceService = new SourceServiceImpl(
                Arrays.asList(githubRaw, githubRepo, githubRelease, subFetcher),
                Arrays.asList(b64Parser),
                processor,
                nodeRepo
        );

        sourceService.initMvpSources();

        // 5. Run initial update
        log.info("Running initial configuration update...");
        sourceService.updateAll();

        log.info("MagicLink Core is active and running.");
    }
}
