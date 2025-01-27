package com.catalis.common.core.filters;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.data.annotation.Id;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.HandlerMethod;
import org.springdoc.core.annotations.ParameterObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FilterParameterCustomizer is responsible for customizing OpenAPI/Swagger documentation
 * for endpoints that use FilterRequest parameters. It automatically generates documentation
 * for filter fields, pagination parameters, and range filters while properly handling
 * special cases like ID fields.
 *
 * The customization process follows these steps:
 * 1. Preserves existing path parameters from the operation
 * 2. Identifies endpoints using FilterRequest parameters with @ParameterObject or @ModelAttribute
 * 3. Extracts the DTO class used for filtering
 * 4. Adds standard pagination parameters
 * 5. Processes DTO fields to create filter parameters:
 *    - Skips ID fields (fields with @Id annotation or ending with "Id") unless marked with @FilterableId
 *    - Creates basic filter parameters for regular fields
 *    - Creates range parameters (from/to) for numeric and date fields
 *    - Handles ID fields marked with @FilterableId as basic filter parameters without range
 * 6. Combines preserved parameters with new filter parameters
 *
 * Features:
 * - Preserves PathVariable parameters in documentation
 * - Automatic detection and handling of ID fields
 * - Support for range-based filtering on numeric and date fields
 * - Pagination parameter documentation
 * - Sorting parameter documentation
 * - Camel case to words conversion for readable descriptions
 * - Support for selectively including ID fields in filters via @FilterableId annotation
 */
@Component
public class FilterParameterCustomizer implements OperationCustomizer {

    /**
     * Customizes the OpenAPI operation by adding filter parameters while preserving path variables.
     * This is called by SpringDoc for each endpoint that uses FilterRequest.
     *
     * @param operation The OpenAPI operation to customize
     * @param handlerMethod The Spring handler method being documented
     * @return The customized operation
     */
    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        // Get existing parameters and preserve path parameters
        List<Parameter> existingParameters = operation.getParameters() != null
                ? operation.getParameters()
                : new ArrayList<>();

        // Keep path parameters and other non-query parameters
        List<Parameter> preservedParameters = existingParameters.stream()
                .filter(param -> "path".equals(param.getIn()) || !"query".equals(param.getIn()))
                .collect(Collectors.toList());

        // Process only FilterRequest parameters with @ParameterObject or @ModelAttribute
        Arrays.stream(handlerMethod.getMethodParameters())
                .filter(param -> FilterRequest.class.isAssignableFrom(param.getParameterType()) &&
                        (param.hasParameterAnnotation(ParameterObject.class) ||
                                param.hasParameterAnnotation(ModelAttribute.class)))
                .findFirst()
                .ifPresent(param -> {
                    Class<?> dtoClass = extractDtoClass(param.getGenericParameterType());
                    if (dtoClass != null) {
                        // Make a copy of the preserved parameters so we can add new filter params
                        List<Parameter> filterParameters = new ArrayList<>(preservedParameters);

                        // Add pagination & filter params from the DTO class
                        addFilterParameters(filterParameters, dtoClass);

                        // Update the operation parameters with the combined list
                        operation.setParameters(filterParameters);
                    }
                });

        return operation;
    }

    /**
     * Extracts the DTO class from a parameterized FilterRequest type.
     * For example, from FilterRequest<UserDTO> it extracts UserDTO.class.
     *
     * @param type The generic parameter type
     * @return The class of the DTO used in the FilterRequest
     */
    private Class<?> extractDtoClass(java.lang.reflect.Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            return (Class<?>) paramType.getActualTypeArguments()[0];
        }
        return null;
    }

    /**
     * Determines if a field should be included in the filter parameters.
     * Excludes static, transient, and specific system fields.
     *
     * @param field The field to check
     * @return true if the field should be included
     */
    private boolean shouldIncludeField(Field field) {
        int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers)
                && !Modifier.isTransient(modifiers)
                && !"serialVersionUID".equals(field.getName());
    }

    /**
     * Checks if a field is an ID field that should be excluded from filtering.
     * A field is considered an excludable ID field if:
     * - It has the @Id annotation, or
     * - Its name ends with "Id" and it doesn't have @FilterableId annotation, or
     * - Its name is exactly "id"
     *
     * Fields marked with @FilterableId will not be considered as excludable ID fields,
     * allowing them to be used as basic filter parameters.
     *
     * @param field The field to check
     * @return true if the field is an ID field that should be excluded
     */
    private boolean isIdField(Field field) {
        return field.isAnnotationPresent(Id.class) ||
                (field.getName().endsWith("Id") && !field.isAnnotationPresent(FilterableId.class)) ||
                "id".equals(field.getName());
    }

    /**
     * Adds all necessary filter parameters to the parameter list.
     * This includes pagination parameters and parameters derived from the DTO fields.
     * Handles three types of fields differently:
     * 1. Regular fields: Gets normal parameter and range parameters if applicable
     * 2. ID fields with @FilterableId: Gets only normal parameter without range
     * 3. Other ID fields: Skipped entirely
     *
     * @param parameters The list of parameters to add to
     * @param dtoClass The DTO class to extract fields from
     */
    private void addFilterParameters(List<Parameter> parameters, Class<?> dtoClass) {
        // Add standard pagination parameters
        parameters.add(createParameter("pageNumber", integerSchema(), "Page number (0-based)", "0"));
        parameters.add(createParameter("pageSize", integerSchema(), "Number of items per page", "10"));
        parameters.add(createParameter("sortBy", stringSchema(), "Field to sort by", null));
        parameters.add(createParameter("sortDirection", stringSchema(), "Sort direction (ASC or DESC)", "DESC"));

        // Process each field in the DTO class
        for (Field field : dtoClass.getDeclaredFields()) {
            if (shouldIncludeField(field)) {
                if (!isIdField(field)) {
                    // Regular field - add normal parameter and range if applicable
                    parameters.add(createParameterFromField(field));
                    if (isRangeableField(field)) {
                        parameters.add(createRangeStartParameter(field));
                        parameters.add(createRangeEndParameter(field));
                    }
                } else if (field.isAnnotationPresent(FilterableId.class)) {
                    // ID field marked as filterable - add only the basic parameter, no range
                    parameters.add(createParameterFromField(field));
                }
            }
        }
    }

    /**
     * Determines if a field should support range-based filtering.
     * Applies to numeric and date/time fields, but never to ID fields
     * (even if they're marked with @FilterableId).
     *
     * @param field The field to check
     * @return true if the field should support range filtering
     */
    private boolean isRangeableField(Field field) {
        // ID fields should never have range parameters, even if they're filterable
        if (field.getName().endsWith("Id") || "id".equals(field.getName()) ||
                field.isAnnotationPresent(Id.class)) {
            return false;
        }

        Class<?> type = field.getType();
        return Number.class.isAssignableFrom(type)
                || type.equals(LocalDateTime.class)
                || type.equals(java.util.Date.class);
    }

    /**
     * Creates a basic OpenAPI parameter with the specified properties.
     * We now accept a Schema<?> directly instead of just a type string,
     * so we can properly handle enums and other types.
     *
     * @param name Parameter name
     * @param schema OpenAPI Schema
     * @param description Parameter description
     * @param defaultValue Default value (can be null)
     * @return Created Parameter object
     */
    private Parameter createParameter(String name, Schema<?> schema, String description, String defaultValue) {
        if (defaultValue != null) {
            schema.setDefault(defaultValue);
        }

        return new Parameter()
                .name(name)
                .in("query")
                .description(description)
                .required(false)
                .schema(schema);
    }

    /**
     * Creates a filter parameter for a specific field using a schema derived from the field type.
     *
     * @param field The field to create a parameter for
     * @return Created Parameter object
     */
    private Parameter createParameterFromField(Field field) {
        Schema<?> schema = createSchemaForType(field.getType());
        return createParameter(
                field.getName(),
                schema,
                "Filter by " + camelCaseToWords(field.getName()),
                null
        );
    }

    /**
     * Creates a "from" range parameter for a field.
     *
     * @param field The field to create a range parameter for
     * @return Created Parameter object
     */
    private Parameter createRangeStartParameter(Field field) {
        return createParameter(
                field.getName() + "From",
                createSchemaForType(field.getType()),
                "Filter " + camelCaseToWords(field.getName()) + " from value",
                null
        );
    }

    /**
     * Creates a "to" range parameter for a field.
     *
     * @param field The field to create a range parameter for
     * @return Created Parameter object
     */
    private Parameter createRangeEndParameter(Field field) {
        return createParameter(
                field.getName() + "To",
                createSchemaForType(field.getType()),
                "Filter " + camelCaseToWords(field.getName()) + " to value",
                null
        );
    }

    /**
     * Creates an OpenAPI Schema object based on the Java type.
     * - If it's an enum, we populate the schema with all possible enum values.
     * - If it's a known primitive/wrapper/string/time type, we choose the proper schema.
     * - Otherwise, we default to string schema.
     *
     * @param type Java type to convert into an OpenAPI Schema
     * @return Schema<?> representing the type
     */
    private Schema<?> createSchemaForType(Class<?> type) {
        if (type.isEnum()) {
            // Create a string schema and add enum values
            StringSchema enumSchema = new StringSchema();
            Object[] constants = type.getEnumConstants();
            for (Object constant : constants) {
                enumSchema.addEnumItem(constant.toString());
            }
            return enumSchema;
        } else if (type == String.class) {
            return stringSchema();
        } else if (type == Integer.class || type == int.class) {
            return integerSchema();
        } else if (type == Long.class || type == long.class) {
            return integerSchema();
        } else if (type == Double.class || type == double.class ||
                type == Float.class || type == float.class) {
            return numberSchema();
        } else if (type == Boolean.class || type == boolean.class) {
            return booleanSchema();
        } else if (type == LocalDateTime.class) {
            return stringSchema().format("date-time");
        } else {
            // For other object types, default to string schema
            return stringSchema();
        }
    }

    /**
     * Helper method to create a simple StringSchema.
     *
     * @return A new StringSchema
     */
    private StringSchema stringSchema() {
        return new StringSchema();
    }

    /**
     * Helper method to create a simple IntegerSchema.
     *
     * @return A new IntegerSchema
     */
    private IntegerSchema integerSchema() {
        return new IntegerSchema();
    }

    /**
     * Helper method to create a simple NumberSchema.
     *
     * @return A new NumberSchema
     */
    private NumberSchema numberSchema() {
        return new NumberSchema();
    }

    /**
     * Helper method to create a simple BooleanSchema.
     *
     * @return A new BooleanSchema
     */
    private BooleanSchema booleanSchema() {
        return new BooleanSchema();
    }

    /**
     * Converts camelCase strings to space-separated words.
     * Example: "firstName" becomes "first name"
     *
     * @param camelCase The camelCase string to convert
     * @return The converted string with spaces
     */
    private String camelCaseToWords(String camelCase) {
        String[] words = camelCase.split("(?=[A-Z])");
        return String.join(" ", words).toLowerCase();
    }
}