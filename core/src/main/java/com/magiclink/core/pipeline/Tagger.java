package com.magiclink.core.pipeline;

import com.magiclink.core.model.Node;
import java.util.List;

public class Tagger implements PipelineStep {
    @Override
    public List<Node> process(List<Node> nodes) {
        for (Node node : nodes) {
            // Placeholder for GeoIP lookup
            if (node.getCountry() == null || node.getCountry().isEmpty()) {
                node.setCountry("Unknown"); 
            }
            // Add other tags if needed
        }
        return nodes;
    }
}
