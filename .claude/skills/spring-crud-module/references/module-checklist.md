# Module Checklist

Use this checklist when creating or expanding a module.

## Mandatory files

- `models/<Entity>.java`
- `repository/<Entity>Repository.java`
- `service/<Entity>Service.java`
- `controller/<Entity>Controller.java`

## Add when the flow needs them

- `request/<Entity>Request.java` for create/update payloads
- `response/<Entity>Response.java` for complete response shaping
- `response/<Entity>BasicResponse.java` for lightweight nested or list response shaping
- `response/<Entity>ListResponse.java` only when the project already uses a dedicated list wrapper
- `records/...Request.java` for narrow PATCH endpoints
- `specification/<Entity>Specification.java` for dynamic filtering
- `response/...BasicResponse.java` for lightweight nested responses

## Patterns to identify in the target codebase

- Aggregate root with rich relationships
- Thin REST API with multipart, pagination, and PATCH support
- Repository with `JpaSpecificationExecutor` when filtered search exists
- Request object with Jakarta Validation
- Response enriched with external or computed data
- Service methods that preserve aggregate consistency during updates

## Common conventions

- Logged user may be stored as an id instead of a full user entity.
- External user data may be resolved by a dedicated integration service.
- Status may come from persisted tables or enums, depending on the target codebase.
- Input models should be named `EntityRequest`.
- Full output models should be named `EntityResponse`.
- Basic output models should be named `EntityBasicResponse`.
- Prefer `Entity(Request request)` constructors and `EntityResponse(Entity entity)` or `EntityBasicResponse(Entity entity)` constructors whenever possible.
- Repositories should stay as plain Spring Data interfaces, without `@Repository`.
- Services should use constructor injection.
- Variables and methods should use English names.
- Portuguese should stay restricted to Swagger/OpenAPI descriptions exposed to API consumers.
- Avoid `var` unless type inference is clearly the better choice.
- Do not use Lombok annotations.
- Avoid overusing helper methods when a direct implementation is cleaner.
- Swagger documentation should cover controller, request, response, and enum contracts.
- When touching springdoc config, ensure `springdoc.swagger-ui.tagsSorter=alpha` and `springdoc.swagger-ui.operationsSorter=alpha` exist.
- When OpenAPI metadata has no version, start with `1`.
- Use `snake_case` for table names and non-foreign-key column names.
- Foreign keys should follow `fk_Id_NomeDaColuna`.
- Foreign key names should follow `FK_FROM_TBTABELAORIGEM_FOR_TBTABELADESTINO`.
- Entities with update flow should use `created_at` and `updated_at` audit fields with lifecycle callbacks.
- Entities without update flow should use only `created_at` with `@PrePersist`.
- Markdown or long textual file content should use `@Lob private String content;`.
- Binary file storage should use the `Document` pattern with `@Table(name = "tbDocument")` and database-specific byte storage mapping.
- Most write endpoints return `204 No Content` or the saved entity/response.
- Standard retrieval by id should go through `service.findById(...)`, not direct repository calls from controllers.
