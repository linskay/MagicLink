package com.magiclink.core.repository;

import com.magiclink.core.model.Node;
import java.util.List;

public interface NodeRepository {
    List<Node> getAll();
    List<Node> getByCountry(String country);
    void saveAll(List<Node> nodes);
    void clearAll();
}
