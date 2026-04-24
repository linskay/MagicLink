package com.magiclink.core.pipeline;

import com.magiclink.core.model.Node;
import java.util.List;

@FunctionalInterface
public interface PipelineStep {
    List<Node> process(List<Node> nodes);
}
