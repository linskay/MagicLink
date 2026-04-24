package com.magiclink.core.service;

import com.magiclink.core.model.Node;
import java.util.List;

public interface ConfigParser {
    List<Node> parse(String content, String sourceId);
    boolean canParse(String content);
}
