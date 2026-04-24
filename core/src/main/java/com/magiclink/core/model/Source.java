package com.magiclink.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Source {
    private String id;
    private String name;
    private String url;
    private SourceType type;
    private String pattern; // file mask or specific filter
    private String lastETag;
    private LocalDateTime lastUpdate;
    private int ttlHours; // default update interval
}
