# API Integration Rules

All API interactions must follow these rules:

1. Define API requests in **separate async functions**
2. Wrap API functions in **custom React Query hooks**
3. Components must **never call apiClient directly**
4. Feature-specific hooks are located in `features/<name>/api/services/`
   - Only hooks reused across multiple features go in `src/hooks`
5. API clients are located in `src/api/clients`
6. Query keys must be **stable and predictable** — use flattened primitives, never a plain object
7. Hooks must expose `QueryOptions` or `MutationOptions` for customization
8. Paginated endpoints must use `ApiRequestParams<TDto, TFilter>` as the param type and return `ApiPaginatedResponse<TDto>`
9. Query keys for paginated hooks must include all params: `[entity, page, size, sort, filter]`

Components must always use hooks instead of calling API functions directly.
