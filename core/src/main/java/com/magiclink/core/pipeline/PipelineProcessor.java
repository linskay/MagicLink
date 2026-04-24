package com.magiclink.core.pipeline;

import com.magiclink.core.model.Node;
import com.magiclink.core.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PipelineProcessor {
    private final List<PipelineStep> steps = new ArrayList<>();
    private final NodeRepository nodeRepository;

    public void addStep(PipelineStep step) {
        steps.add(step);
    }

    public void process(List<Node> initialNodes) {
        log.info("Starting pipeline processing for {} nodes", initialNodes.size());
        List<Node> currentNodes = initialNodes;
        
        for (PipelineStep step : steps) {
            currentNodes = step.process(currentNodes);
            log.debug("After step: {}, nodes remaining: {}", step.getClass().getSimpleName(), currentNodes.size());
        }

        log.info("Pipeline processing complete. Saving {} nodes to repository.", currentNodes.size());
        nodeRepository.saveAll(currentNodes);
    }
}
