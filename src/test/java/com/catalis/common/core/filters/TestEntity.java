package com.catalis.common.core.filters;

import com.catalis.core.utils.annotations.FilterableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Test entity with various field types for testing filtering capabilities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestEntity {
    @Id
    private Long id;
    
    @FilterableId
    private Long filterableId;
    
    private String name;
    
    private Integer count;
    
    private Boolean active;
    
    private LocalDateTime createdDate;
    
    private List<String> tags;
    
    private Set<Long> relatedIds;
    
    private String[] stringArray;
    
    private NestedObject nestedObject;
    
    /**
     * Nested object for testing nested object filtering.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NestedObject {
        private String nestedName;
        private Integer nestedCount;
    }
}