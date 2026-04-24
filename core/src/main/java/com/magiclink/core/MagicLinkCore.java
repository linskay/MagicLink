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
        boolean refresh = false;
        String exportPath = null;

        for (int i = 0; i < args.length; i++) {
            if ("--refresh".equals(args[i])) refresh = true;
            if ("--export".equals(args[i]) && i + 1 < args.length) exportPath = args[i+1];
        }

        if (refresh || exportPath != null) {
            if (refresh) {
                log.info("CLI: Running manual source update...");
                sourceService.updateAll();
            }
            if (exportPath != null) {
                log.info("CLI: Exporting nodes to {}...", exportPath);
                try {
                    java.util.List<com.magiclink.core.model.Node> nodes = nodeRepo.getAll();
                    // Filter: latency < 500ms and not null
                    java.util.List<com.magiclink.core.model.Node> filtered = nodes.stream()
                            .filter(n -> n.getLatency() > 0 && n.getLatency() < 500)
                            .toList();
                    mapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(exportPath), filtered);
                    log.info("Successfully exported {} filtered nodes (from total {})", filtered.size(), nodes.size());
                } catch (Exception e) {
                    log.error("Failed to export nodes", e);
                    System.exit(1);
                }
            }
            System.exit(0);
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
