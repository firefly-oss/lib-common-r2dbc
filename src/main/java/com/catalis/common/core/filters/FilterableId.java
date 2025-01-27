package com.catalis.common.core.filters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The FilterableId annotation is used to mark ID fields that should be included
 * in filtering operations. By default, fields that end with "Id" are excluded
 * from filter parameters, but when a field is annotated with @FilterableId, it will
 * be included in the filters with the following behavior:
 *
 * - In OpenAPI/Swagger documentation (FilterParameterCustomizer):
 *   - The field will appear as a query parameter
 *   - No range parameters will be generated for the field
 *
 * - In runtime filtering (FilterUtils):
 *   - The field will be included in filtering operations
 *   - Only exact matching will be used (no LIKE or range operations)
 *   - Range filters will be ignored for this field
 *
 * Example usage:
 * {@code
 *     public class UserFilterDTO {
 *         @FilterableId
 *         private Long customerId;  // Will be included in filters with exact matching
 *
 *         private Long accountId;   // Will be excluded from filters
 *
 *         private String name;      // Regular field with full filtering capabilities
 *     }
 * }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FilterableId {
}