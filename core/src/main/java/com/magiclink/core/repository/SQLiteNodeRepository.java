package com.magiclink.core.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiclink.core.model.Node;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class SQLiteNodeRepository implements NodeRepository {
    private final DatabaseManager dbManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Node> getAll() {
        List<Node> nodes = new ArrayList<>();
        String sql = "SELECT * FROM nodes";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                nodes.add(mapNode(rs));
            }
        } catch (Exception e) {
            log.error("Error fetching all nodes", e);
        }
        return nodes;
    }

    @Override
    public List<Node> getByCountry(String country) {
        List<Node> nodes = new ArrayList<>();
        String sql = "SELECT * FROM nodes WHERE country = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, country);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    nodes.add(mapNode(rs));
                }
            }
        } catch (Exception e) {
            log.error("Error fetching nodes by country: " + country, e);
        }
        return nodes;
    }

    @Override
    public void saveAll(List<Node> nodes) {
        String sql = "INSERT OR REPLACE INTO nodes (id, type, host, port, country, source, latency, params) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (Node node : nodes) {
                pstmt.setString(1, node.getId());
                pstmt.setString(2, node.getType());
                pstmt.setString(3, node.getHost());
                pstmt.setInt(4, node.getPort());
                pstmt.setString(5, node.getCountry());
                pstmt.setString(6, node.getSource());
                pstmt.setLong(7, node.getLatency());
                pstmt.setString(8, objectMapper.writeValueAsString(node.getParams()));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (Exception e) {
            log.error("Error saving nodes batch", e);
        }
    }

    @Override
    public void clearAll() {
        String sql = "DELETE FROM nodes";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (Exception e) {
            log.error("Error clearing nodes", e);
        }
    }

    private Node mapNode(ResultSet rs) throws Exception {
        return Node.builder()
                .id(rs.getString("id"))
                .type(rs.getString("type"))
                .host(rs.getString("host"))
                .port(rs.getInt("port"))
                .country(rs.getString("country"))
                .source(rs.getString("source"))
                .latency(rs.getLong("latency"))
                .params(objectMapper.readValue(rs.getString("params"), new TypeReference<Map<String, Object>>() {}))
                .build();
    }
}
