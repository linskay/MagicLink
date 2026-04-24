package com.magiclink.core.pipeline;

import com.magiclink.core.model.Node;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Deduplicator implements PipelineStep {
    @Override
    public List<Node> process(List<Node> nodes) {
        return nodes.stream()
                .filter(distinctByKey(n -> n.getHost() + ":" + n.getPort() + ":" + n.getType()))
                .collect(Collectors.toList());
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
