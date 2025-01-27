package com.catalis.common.core.filters;

import com.catalis.common.core.queries.PaginationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Generic filter request that includes both filter criteria and pagination")
public class FilterRequest<T> {

    @Schema(description = "Filter criteria")
    private T filters;

    @Schema(description = "Range filters for numeric fields")
    private RangeFilter rangeFilters;

    @Schema(description = "Pagination and sorting parameters", required = true)
    private PaginationRequest pagination;
}