package com.magiclink.core.pipeline;

import com.magiclink.core.model.Node;
import com.magiclink.core.service.GeoIPService;
import java.util.List;

@RequiredArgsConstructor
public class Tagger implements PipelineStep {
    private final GeoIPService geoIPService;

    @Override
    public List<Node> process(List<Node> nodes) {
        for (Node node : nodes) {
            if (node.getCountry() == null || node.getCountry().isEmpty() || "Unknown".equals(node.getCountry())) {
                String country = geoIPService.getCountryCode(node.getHost());
                node.setCountry(country);
            }
        }
        return nodes;
    }
}
