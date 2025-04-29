# Firefly R2DBC Common Library

A comprehensive reactive database connectivity library for the Firefly platform, providing advanced filtering, pagination, and transaction management capabilities for Spring Data R2DBC applications.

## Overview

The lib-common-r2dbc library is a core component of the Firefly platform that provides powerful utilities for working with reactive database connections using Spring Data R2DBC. It simplifies common database operations with a focus on:

- **Reactive Programming**: Built on Project Reactor for non-blocking database operations
- **Advanced Filtering**: Flexible and type-safe filtering capabilities
- **Pagination**: Standardized pagination with sorting support
- **Transaction Management**: Simplified R2DBC transaction handling
- **PostgreSQL Support**: Optimized for PostgreSQL databases
- **OpenAPI Integration**: Automatic API documentation for filter and pagination models

## Features

### Reactive Filtering

The library provides a powerful filtering system that supports:

- **String Filtering**: Case-sensitive or case-insensitive LIKE queries
- **Numeric/Boolean Filtering**: Exact match filtering for numeric and boolean values
- **ID Field Handling**: Special handling for ID fields with @FilterableId annotation support
- **Range Filtering**: Between, greater-than, and less-than operations for numeric and date fields
- **Collection/Array Filtering**: Support for filtering on collection and array fields
- **Null/Not-Null Filtering**: Explicit filtering for NULL or NOT NULL values
- **Performance Optimization**: Field reflection caching for better performance

### Pagination

Standardized pagination support with:

- **Page Size and Number**: Control the number of results per page
- **Sorting**: Sort by any field with ascending or descending direction
- **Total Count**: Automatic calculation of total results
- **Consistent Response Format**: Standardized response structure

### Transaction Management

Simplified transaction management for R2DBC operations.

### OpenAPI Documentation

Automatic generation of OpenAPI documentation for filter and pagination models. The library includes a `FilterParameterCustomizer` that enhances Swagger/OpenAPI documentation with detailed query parameters for all filtering capabilities.

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-r2dbc</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Repository Configuration

Ensure you have access to the GitHub Packages repository:

```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/firefly-oss/lib-common-r2dbc</url>
    </repository>
</repositories>
```

## Usage

### Basic Configuration

The library auto-configures itself when included in a Spring Boot application. Make sure your application has the necessary R2DBC connection properties:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/your_database
    username: your_username
    password: your_password
```

### Creating a Filter

1. Create an entity class:

```java
@Data
@Table("users")
public class User {
    @Id
    private Long id;

    private String name;
    private String email;
    private Boolean active;
    private LocalDateTime createdDate;
}
```

2. Create a filter class for your entity:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFilter {
    // Use @FilterableId to make ID fields filterable
    @FilterableId
    private Long id;

    private String name;
    private String email;
    private Boolean active;
}
```

3. Create a controller endpoint that uses the filtering utilities:

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @PostMapping("/filter")
    public Mono<PaginationResponse<UserDto>> filterUsers(@RequestBody FilterRequest<UserFilter> request) {
        // Create a filter for the User entity
        GenericFilter<UserFilter, User, UserDto> filter =
            FilterUtils.createFilter(User.class, userMapper::toDto);

        // Apply the filter and return paginated results
        return filter.filter(request);
    }
}
```

### Filter Request Examples

#### Basic Filtering

```java
// Create a filter request with basic filters
FilterRequest<UserFilter> request = FilterRequest.<UserFilter>builder()
    .filters(new UserFilter(null, "John", "john@example.com", true))
    .pagination(new PaginationRequest(0, 10, "name", "ASC"))
    .build();
```

#### Range Filtering

```java
// Create a filter request with range filters
FilterRequest<UserFilter> request = FilterRequest.<UserFilter>builder()
    .filters(new UserFilter("John", null, true))
    .rangeFilters(RangeFilter.builder()
        .ranges(Map.of(
            "createdDate", new RangeFilter.Range<>(
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now()
            ),
            "balance", new RangeFilter.Range<>(
                1000.0,
                5000.0
            )
        ))
        .build())
    .pagination(new PaginationRequest(0, 10, "name", "ASC"))
    .build();
```

#### Case-Insensitive String Filtering

```java
// Create a filter request with case-insensitive string filtering
FilterRequest<UserFilter> request = FilterRequest.<UserFilter>builder()
    .filters(new UserFilter("john", null, true))
    .options(FilterUtils.FilterOptions.builder()
        .caseInsensitiveStrings(true)
        .build())
    .pagination(new PaginationRequest(0, 10, "name", "ASC"))
    .build();
```

#### Collection/Array Filtering

```java
// Create a filter with collection values
UserFilter filter = new UserFilter();
filter.setRoles(Arrays.asList("ADMIN", "MANAGER"));
filter.setTags(new String[]{"important", "urgent"});

FilterRequest<UserFilter> request = FilterRequest.<UserFilter>builder()
    .filters(filter)
    .pagination(new PaginationRequest(0, 10, "name", "ASC"))
    .build();
```

#### Null/Not Null Filtering

```java
// Create a filter to find users with null email
UserFilter filter = new UserFilter();
filter.setEmail(FilterRequest.NULL_VALUE);

// Create a filter to find users with non-null phone
UserFilter anotherFilter = new UserFilter();
anotherFilter.setPhone(FilterRequest.NOT_NULL_VALUE);

FilterRequest<UserFilter> request = FilterRequest.<UserFilter>builder()
    .filters(filter)
    .pagination(new PaginationRequest(0, 10, "name", "ASC"))
    .build();
```

#### Combining Multiple Filter Types

```java
// Create a complex filter combining different filter types
UserFilter filter = new UserFilter();
filter.setName("john");
filter.setActive(true);
filter.setRoles(Arrays.asList("ADMIN", "MANAGER"));
filter.setEmail(FilterRequest.NOT_NULL_VALUE);

Map<String, RangeFilter.Range<?>> ranges = new HashMap<>();
ranges.put("createdDate", new RangeFilter.Range<>(
    LocalDateTime.now().minusDays(30),
    LocalDateTime.now()
));
ranges.put("lastLoginDate", new RangeFilter.Range<>(
    LocalDateTime.now().minusDays(7),
    null  // Only specify lower bound
));

FilterRequest<UserFilter> request = FilterRequest.<UserFilter>builder()
    .filters(filter)
    .rangeFilters(RangeFilter.builder().ranges(ranges).build())
    .options(FilterUtils.FilterOptions.builder()
        .caseInsensitiveStrings(true)
        .includeInheritedFields(true)
        .build())
    .pagination(new PaginationRequest(0, 10, "name", "ASC"))
    .build();
```

### Pagination Example

```java
// Create a pagination request
PaginationRequest paginationRequest = new PaginationRequest();
paginationRequest.setPageNumber(0);
paginationRequest.setPageSize(20);
paginationRequest.setSortBy("lastName");
paginationRequest.setSortDirection("ASC");

// Use PaginationUtils to paginate a query
Mono<PaginationResponse<UserDto>> result = PaginationUtils.paginateQuery(
    paginationRequest,
    userMapper::toDto,
    pageable -> userRepository.findAll(pageable),
    () -> userRepository.count()
);
```

## OpenAPI Documentation

The library includes comprehensive Swagger/OpenAPI support for all filter and pagination models. When used in a Spring Boot application with SpringDoc OpenAPI, the models and query parameters will be automatically documented.

### Setup

To enable Swagger UI, add the following dependency to your project:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

### Automatic Query Parameter Documentation

The `FilterParameterCustomizer` class automatically enhances your OpenAPI documentation with detailed query parameters for all filtering capabilities:

- **Pagination Parameters**: `pagination.pageNumber`, `pagination.pageSize`, `pagination.sortBy`, `pagination.sortDirection`
- **Filter Parameters**: `filters.<fieldName>` for each field in your filter class
- **Range Filter Parameters**: `rangeFilters.ranges[<fieldName>].from` and `rangeFilters.ranges[<fieldName>].to` for numeric and date fields
- **Filter Options**: `options.caseInsensitiveStrings`, `options.includeInheritedFields`

### Example

When you annotate your controller method with `@ParameterObject` or `@ModelAttribute`:

```java
@GetMapping("/filter")
public Mono<PaginationResponse<UserDto>> filterUsers(
        @ParameterObject FilterRequest<UserFilter> request) {
    // Implementation
}
```

The OpenAPI documentation will automatically include all the filter parameters:

```
GET /api/users/filter?
    pagination.pageNumber=0&
    pagination.pageSize=10&
    filters.name=John&
    rangeFilters.ranges[createdDate].from=2023-01-01T00:00:00&
    options.caseInsensitiveStrings=true
```

## Troubleshooting

### Common Issues and Solutions

#### No Results When Filtering

**Issue**: Filter is applied but no results are returned when you expect matches.

**Possible causes and solutions**:
- **Case sensitivity**: String filters are case-sensitive by default. Use the `caseInsensitiveStrings` option if you need case-insensitive matching.
- **ID fields**: ID fields are excluded from filtering by default unless annotated with `@FilterableId`.
- **Null values**: Regular filters skip null values. Use `FilterRequest.NULL_VALUE` to explicitly filter for null values.

#### Performance Issues

**Issue**: Filtering operations are slow, especially with large datasets.

**Possible solutions**:
- Ensure database indexes are created for commonly filtered fields
- Use pagination with reasonable page sizes
- Consider using range filters instead of collection filters for large datasets
- The library uses field reflection caching to improve performance, but complex filter objects might still impact performance

#### Error: "R2dbcEntityTemplate not initialized"

**Issue**: `IllegalStateException: R2dbcEntityTemplate not initialized` when using FilterUtils.

**Solution**: Call `FilterUtils.initializeTemplate(entityTemplate)` in your application configuration before using any filtering functionality.

```java
@Configuration
public class AppConfig {
    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @PostConstruct
    public void init() {
        FilterUtils.initializeTemplate(entityTemplate);
    }
}
```

#### Issues with Inherited Fields

**Issue**: Fields from parent classes are not included in filtering.

**Solution**: Use the `includeInheritedFields` option to include fields from parent classes.

```java
FilterRequest<UserFilter> request = FilterRequest.<UserFilter>builder()
    .filters(filter)
    .options(FilterUtils.FilterOptions.builder()
        .includeInheritedFields(true)
        .build())
    .build();
```

## Contributing

Contributions to the lib-common-r2dbc library are welcome. Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Create a new Pull Request
