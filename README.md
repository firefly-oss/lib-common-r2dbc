# Firefly R2DBC Common Library

A reactive database connectivity library for the Firefly platform, providing utilities for filtering, pagination, and transaction management with R2DBC.

## Overview

The lib-common-r2dbc library is a core component of the Firefly platform that provides standardized utilities for working with reactive database connections using Spring Data R2DBC. It simplifies common database operations with a focus on:

- Reactive programming model using Project Reactor
- Standardized filtering capabilities
- Pagination utilities
- Transaction management
- PostgreSQL R2DBC support

## Features

- **Reactive Filtering**: Easily filter database entities with support for:
  - String fields (using LIKE queries)
  - Numeric fields (exact match)
  - ID fields (with special handling)
  - Range filters (for numeric values, dates, etc.)

- **Pagination**: Standardized pagination support with:
  - Page size and number
  - Sorting capabilities
  - Total count calculation
  - Consistent response format

- **Transaction Management**: R2DBC transaction configuration

- **Swagger Integration**: API documentation for filter and pagination models

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

### Filtering Example

1. Create a filter class for your entity:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFilter {
    private String name;
    private String email;
    private Boolean active;
}
```

2. Create a controller endpoint that uses the filtering utilities:

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

### Range Filtering Example

```java
// Create a filter request with range filters
FilterRequest<UserFilter> request = FilterRequest.<UserFilter>builder()
    .filters(new UserFilter("John", null, true))
    .rangeFilters(RangeFilter.builder()
        .ranges(Map.of(
            "createdDate", new RangeFilter.Range<>(
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now()
            )
        ))
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

## API Documentation

The library includes Swagger annotations for all filter and pagination models. When used in a Spring Boot application with SpringDoc OpenAPI, the models will be automatically documented.

To enable Swagger UI, add the following dependency to your project:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

## Contributing

Contributions to the lib-common-r2dbc library are welcome. Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Create a new Pull Request