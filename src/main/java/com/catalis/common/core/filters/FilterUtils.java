package com.catalis.common.core.filters;

import com.catalis.common.core.queries.PaginationRequest;
import com.catalis.common.core.queries.PaginationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * FilterUtils is a utility class designed to support filtering and pagination of database records
 * using Spring R2DBC. It provides a generic mechanism to dynamically query records using custom filters,
 * range filters, and pagination settings, while efficiently returning the results in a paginated response format.
 * <p>
 * Features:
 * - Dynamic filtering based on regular and range filters
 * - Special handling for ID fields:
 *   - Uses exact matching for all ID fields
 *   - Skips range filtering for ID fields (even if marked with @FilterableId)
 *   - Supports @FilterableId annotation to include specific ID fields in filtering
 * - Pagination and sorting support
 * - Uses Spring's reactive R2DBC to fetch data asynchronously
 * - Convert database entity results into custom DTOs using a mapper function
 * <p>
 * ID Field Handling:
 * - Fields with @Id annotation are always treated as ID fields
 * - Fields ending with "Id" are treated as ID fields
 * - Fields named exactly "id" are treated as ID fields
 * - By default, ID fields are excluded from filtering
 * - Adding @FilterableId to an ID field includes it in filtering with exact matching
 * - Range filtering is never applied to ID fields, even with @FilterableId
 */
@Slf4j
public class FilterUtils {
    /** Static instance of R2dbcEntityTemplate used for database operations */
    private static R2dbcEntityTemplate entityTemplate;

    /**
     * Initializes the static R2dbcEntityTemplate instance.
     * This method should be called during application startup.
     *
     * @param template The R2dbcEntityTemplate to be used for database operations
     */
    public static void initializeTemplate(R2dbcEntityTemplate template) {
        entityTemplate = template;
    }

    /**
     * Creates and returns a new GenericFilter instance for the specified entity and DTO types.
     *
     * @param entityClass The class of the entity to be queried
     * @param mapper Function to convert from entity to DTO
     * @return A new GenericFilter instance
     * @throws IllegalStateException if the entityTemplate hasn't been initialized
     */
    public static <F, E, D> GenericFilter<F, E, D> createFilter(Class<E> entityClass, Function<E, D> mapper) {
        if (entityTemplate == null) {
            throw new IllegalStateException("R2dbcEntityTemplate not initialized. Call FilterUtils.initializeTemplate first.");
        }
        return new GenericFilter<>(entityClass, mapper);
    }

    /**
     * GenericFilter class that handles the actual filtering, pagination, and mapping operations.
     * This inner class maintains the entity class and mapping function information.
     */
    public static class GenericFilter<F, E, D> {
        private final Class<E> entityClass;
        private final Function<E, D> mapper;

        /**
         * Constructs a new GenericFilter with the specified entity class and mapper function.
         */
        private GenericFilter(Class<E> entityClass, Function<E, D> mapper) {
            this.entityClass = entityClass;
            this.mapper = mapper;
        }

        /**
         * Main filtering method that processes the filter request and returns paginated results.
         * This method orchestrates the entire filtering process:
         * 1. Builds criteria from regular and range filters
         * 2. Creates a query with the criteria and pagination settings
         * 3. Executes the query and counts total matching elements
         * 4. Maps the results to DTOs and creates a paginated response
         *
         * @param filterRequest The filter request containing criteria and pagination settings
         * @return A Mono containing the paginated response with mapped DTOs
         */
        public Mono<PaginationResponse<D>> filter(FilterRequest<?> filterRequest) {
            Criteria criteria = buildCriteria(filterRequest);
            Query query = createQuery(criteria, filterRequest.getPagination());

            return Mono.zip(
                    fetchAndMapPagedData(query),
                    countTotalElements(criteria)
            ).map(tuple -> createPaginationResponse(tuple, filterRequest.getPagination()));
        }

        /**
         * Fetches and maps the paginated data based on the provided query.
         * The query includes both filtering criteria and pagination/sorting settings.
         */
        private Mono<List<D>> fetchAndMapPagedData(Query query) {
            return entityTemplate.select(entityClass)
                    .matching(query)
                    .all()
                    .map(mapper)
                    .collectList();
        }

        /**
         * Counts the total number of elements matching the filter criteria.
         * This is used to calculate pagination metadata.
         */
        private Mono<Long> countTotalElements(Criteria criteria) {
            return entityTemplate.select(entityClass)
                    .matching(Query.query(criteria))
                    .count();
        }

        /**
         * Creates a PaginationResponse object containing the fetched data and pagination metadata.
         * The response includes:
         * - The content (list of mapped DTOs)
         * - Total number of elements matching the criteria
         * - Total number of pages
         * - Current page number
         */
        private PaginationResponse<D> createPaginationResponse(
                Tuple2<List<D>, Long> tuple,
                PaginationRequest pagination) {
            List<D> content = tuple.getT1();
            long totalElements = tuple.getT2();
            int totalPages = calculateTotalPages(totalElements, pagination.getPageSize());

            return PaginationResponse.<D>builder()
                    .content(content)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .currentPage(pagination.getPageNumber())
                    .build();
        }

        /**
         * Calculates the total number of pages based on total elements and page size.
         * Handles the case where page size is 0 to avoid division by zero.
         */
        private int calculateTotalPages(long totalElements, int pageSize) {
            return pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
        }

        /**
         * Creates a Query object with filtering criteria, pagination, and sorting settings.
         * The query combines:
         * - The filtering criteria
         * - Sort settings (if specified)
         * - Pagination settings (limit and offset)
         */
        private Query createQuery(Criteria criteria, PaginationRequest pagination) {
            Query query = Query.query(criteria);

            if (pagination != null) {
                // Apply sorting if sortBy field is specified
                if (pagination.getSortBy() != null && !pagination.getSortBy().isEmpty()) {
                    Sort sort = Sort.by(
                            Sort.Direction.fromString(pagination.getSortDirection()),
                            pagination.getSortBy()
                    );
                    query = query.sort(sort);
                }

                // Apply pagination
                query = query.limit(pagination.getPageSize())
                        .offset((long) pagination.getPageNumber() * pagination.getPageSize());
            }

            return query;
        }

        /**
         * Builds the complete filtering criteria from both regular and range filters.
         * Combines criteria from:
         * - Regular filters (exact matches for IDs, LIKE for strings, etc.)
         * - Range filters (for numeric and date fields, excluding ID fields)
         */
        private Criteria buildCriteria(FilterRequest<?> filterRequest) {
            List<Criteria> criteriaList = new ArrayList<>();

            if (filterRequest.getFilters() != null) {
                criteriaList.addAll(processRegularFilters(filterRequest.getFilters()));
            }

            if (filterRequest.getRangeFilters() != null) {
                criteriaList.addAll(processRangeFilters(filterRequest.getRangeFilters()));
            }

            return criteriaList.isEmpty() ?
                    Criteria.empty() :
                    Criteria.from(criteriaList);
        }

        /**
         * Processes regular (non-range) filters and converts them to criteria.
         * Handles different field types and special cases:
         * - ID fields (with or without @FilterableId): Use exact matching
         * - String fields: Use LIKE operator for partial matching
         * - Other fields: Use exact matching
         */
        private List<Criteria> processRegularFilters(Object filters) {
            List<Criteria> criteriaList = new ArrayList<>();
            Field[] fields = filters.getClass().getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(filters);
                    if (value != null) {
                        // Check if field should be excluded
                        if (isExcludableIdField(field)) {
                            continue;
                        }

                        // All ID fields use exact matching
                        if (isAnyIdField(field)) {
                            criteriaList.add(Criteria.where(field.getName()).is(value));
                        }
                        // For non-ID fields, use appropriate matching
                        else if (value instanceof String && !((String) value).isEmpty()) {
                            criteriaList.add(Criteria.where(field.getName()).like("%" + value + "%"));
                        } else if (!(value instanceof String)) {
                            criteriaList.add(Criteria.where(field.getName()).is(value));
                        }
                    }
                } catch (IllegalAccessException e) {
                    log.error("Error accessing field: {}", field.getName(), e);
                }
            }

            return criteriaList;
        }

        /**
         * Processes range filters and converts them to criteria.
         * Range filters are applied only to non-ID fields and can include:
         * - Between: When both 'from' and 'to' values are provided
         * - Greater than or equal: When only 'from' value is provided
         * - Less than or equal: When only 'to' value is provided
         */
        private List<Criteria> processRangeFilters(RangeFilter rangeFilters) {
            List<Criteria> criteriaList = new ArrayList<>();

            rangeFilters.getRanges().forEach((fieldName, range) -> {
                // Skip range filters for any ID field
                if (isAnyIdField(fieldName, entityClass)) {
                    log.debug("Skipping range filter for ID field: {}", fieldName);
                    return;
                }

                if (range.getFrom() != null && range.getTo() != null) {
                    criteriaList.add(
                            Criteria.where(fieldName)
                                    .between(range.getFrom(), range.getTo())
                    );
                } else if (range.getFrom() != null) {
                    criteriaList.add(
                            Criteria.where(fieldName)
                                    .greaterThanOrEquals(range.getFrom())
                    );
                } else if (range.getTo() != null) {
                    criteriaList.add(
                            Criteria.where(fieldName)
                                    .lessThanOrEquals(range.getTo())
                    );
                }
            });

            return criteriaList;
        }

        /**
         * Checks if a field is any type of ID field (whether excludable or filterable).
         * This is used to determine the type of matching to use and whether to allow range filtering.
         */
        private boolean isAnyIdField(Field field) {
            return field.isAnnotationPresent(Id.class) ||
                    field.getName().endsWith("Id") ||
                    "id".equals(field.getName());
        }

        /**
         * Checks if a field is any type of ID field by name.
         * Used when processing range filters where we only have the field name.
         */
        private boolean isAnyIdField(String fieldName, Class<?> entityClass) {
            try {
                Field field = entityClass.getDeclaredField(fieldName);
                return isAnyIdField(field);
            } catch (NoSuchFieldException e) {
                return false;
            }
        }

        /**
         * Checks if a field is an ID field that should be excluded from filtering.
         * A field is excludable if it's an ID field but doesn't have @FilterableId annotation.
         */
        private boolean isExcludableIdField(Field field) {
            return isAnyIdField(field) && !field.isAnnotationPresent(FilterableId.class);
        }
    }
}