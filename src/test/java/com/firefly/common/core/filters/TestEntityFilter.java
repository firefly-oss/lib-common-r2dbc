package com.firefly.common.core.filters;

import com.firefly.core.utils.annotations.FilterableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Filter class for TestEntity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestEntityFilter {
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
}