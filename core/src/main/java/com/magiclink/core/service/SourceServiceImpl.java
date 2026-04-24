package com.magiclink.core.service;

import com.magiclink.core.model.Node;
import com.magiclink.core.model.Source;
import com.magiclink.core.model.SourceType;
import com.magiclink.core.pipeline.PipelineProcessor;
import com.magiclink.core.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SourceServiceImpl implements SourceService {
    private final List<SourceFetcher> fetchers;
    private final List<ConfigParser> parsers;
    private final PipelineProcessor pipelineProcessor;
    private final NodeRepository nodeRepository;

    private final List<Source> mvpSources = new ArrayList<>();

    public void initMvpSources() {
        mvpSources.add(Source.builder()
                .id("github-v2ray-configs")
                .name("V2Ray Configs Repo")
                .url("https://github.com/v2ray/config-repo") // Example URL
                .type(SourceType.GITHUB_REPO)
                .pattern("*.json")
                .ttlHours(12)
                .build());

        mvpSources.add(Source.builder()
                .id("pub-proxy-list")
                .name("Public Proxy List")
                .url("https://raw.githubusercontent.com/proxies/list/main/raw.txt")
                .type(SourceType.GITHUB_RAW)
                .ttlHours(6)
                .build());

        mvpSources.add(Source.builder()
                .id("singbox-compat")
                .name("Sing-box Compatible Configs")
                .url("https://example.com/sub/sing-box")
                .type(SourceType.SUBSCRIPTION)
                .ttlHours(6)
                .build());
    }

    @Override
    public void updateAll() {
        log.info("Starting global update of all sources...");
        List<Node> allFoundNodes = new ArrayList<>();

        for (Source source : mvpSources) {
            try {
                String content = fetchFromSource(source);
                if (content != null) {
                    List<Node> parsedNodes = parseContent(content, source.getId());
                    allFoundNodes.addAll(parsedNodes);
                }
            } catch (Exception e) {
                log.error("Failed to update source: " + source.getName(), e);
            }
        }

        if (!allFoundNodes.isEmpty()) {
            pipelineProcessor.process(allFoundNodes);
        } else {
            log.warn("No nodes found during update.");
        }
    }

    private String fetchFromSource(Source source) throws Exception {
        for (SourceFetcher fetcher : fetchers) {
            if (fetcher.supports(source)) {
                return fetcher.fetch(source);
            }
        }
        log.warn("No fetcher found for source type: {}", source.getType());
        return null;
    }

    private List<Node> parseContent(String content, String sourceId) {
        for (ConfigParser parser : parsers) {
            if (parser.canParse(content)) {
                return parser.parse(content, sourceId);
            }
        }
        log.warn("No parser found for content from source: {}", sourceId);
        return new ArrayList<>();
    }
}
