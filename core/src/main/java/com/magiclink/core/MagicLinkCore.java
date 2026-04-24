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
        GeoIPService geoIPService = new GeoIPService(client, mapper);

        // Check for CLI commands
        if (args.length >= 1) {
            String command = args[0];
            if ("--export".equals(command)) {
                String fileName = args.length > 1 ? args[1] : "../desktop/data/nodes.json";
                log.info("Exporting nodes to {}...", fileName);
                try {
                    java.util.List<com.magiclink.core.model.Node> nodes = nodeRepo.getAll();
                    mapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(fileName), nodes);
                    log.info("Successfully exported {} nodes to {}", nodes.size(), fileName);
                    System.exit(0);
                } catch (Exception e) {
                    log.error("Failed to export nodes", e);
                    System.exit(1);
                }
            } else if ("--update".equals(command)) {
                log.info("CLI: Running manual source update...");
                // Note: This would need a full setup call which is done below, 
                // but for a quick CLI we might want to just trigger the update.
            }
        }

        // 2. Setup Pipeline
        PipelineProcessor processor = new PipelineProcessor(nodeRepo);
        processor.addStep(new Validator());
        processor.addStep(new Deduplicator());
        processor.addStep(new LatencyTester());
        processor.addStep(new Tagger(geoIPService));

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
        log.info("MagicLink Core is active and running.");

        // 6. Schedule periodic updates (every hour)
        java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                log.info("Cron: Running scheduled source update...");
                sourceService.updateAll();
                log.info("Cron: Update completed.");
            } catch (Exception e) {
                log.error("Cron: Scheduled update failed", e);
            }
        }, 1, 1, java.util.concurrent.TimeUnit.HOURS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down MagicLink Core...");
            scheduler.shutdown();
        }));
    }
}
