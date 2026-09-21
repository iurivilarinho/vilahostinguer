# Filter Patterns

## Typical implementation map

- Entry point service
- Predicate library in a `Specification` class
- Repository support through `JpaSpecificationExecutor`
- HTTP exposure through a listing controller

## Composition location

- Combine specifications directly in the service layer.
- Keep parameter null checks inside each static specification method.
- Prefer returning `Specification.unrestricted()` when a filter input is absent.

## Preferred service composition style

```java
@Transactional(readOnly = true)
public Page<Entity> findAllPage(FilterA filterA, FilterB filterB, String search, Pageable page) {
    return repository.findAll(
        EntitySpecification.filterAIgual(filterA)
            .and(EntitySpecification.filterBIgual(filterB))
            .and(EntitySpecification.searchAllFields(search, entityManager)),
        page
    );
}
```

Use this same style for non-pageable searches, removing only the `Pageable` argument.

## Reusable predicate patterns

- Equality by id: `idEqual`
- Equality by creator/responsible: `usuarioIgual`, `usuarioResponsavelIgual`
- Date range: `dataAberturaEntre`, `dataEsperadaIncioEntre`, `dataAEsperadaFimEntre`
- Boolean flag: `isCritical`
- List membership: `statusIn`, `sectorInId`
- Join to nested enum: `statusDiferente`, `statusIn`
- Generic text search: `searchAllFields`

## Design notes

- The repository stays thin; filtering lives outside.
- Reporting reuses the same predicates through a non-pageable method.
- Null checks happen inside each `Specification` method, not in controller branches.
- Complex `searchAllFields` implementations may inspect root fields, associations, and basic string collections when needed.
