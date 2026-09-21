---
name: spring-tests-junit-mockito
description: Create or update typed tests for Java Spring backends using JUnit 5 and Mockito. Use when Codex needs unit tests for services, controller tests with mocked dependencies, validation of exception flows, interaction verification, argument capture, or regression coverage for business rules while keeping test code explicit, readable, and free from `var` unless strictly necessary.
---

# Spring Tests JUnit Mockito

Create focused automated tests that follow the backend's current style without inflating the test suite with unnecessary abstractions.

## Read first

- The production class under test
- The existing tests for sibling modules
- The project's test dependencies and shared base classes, if any
- The exception handler behavior when testing controller or service error flows

## Core rules

- Use JUnit 5 and Mockito annotations already adopted by the project.
- Prefer `@ExtendWith(MockitoExtension.class)` for pure unit tests.
- Keep tests strongly typed. Do not use `var` unless the explicit type is genuinely harmful to readability.
- Do not use `any` as a type shortcut. Use the real generic type, request type, response type, or captor type.
- Keep variable names and method names in English.
- Use Portuguese only in Swagger/OpenAPI descriptions from production code, not as a reason to mix test identifiers.
- Avoid excessive helper methods in test classes. Extract helpers only when they remove meaningful duplication and improve readability.
- Prefer straightforward arrange-act-assert flow over deep private helper trees.

## Service test pattern

- Mock repositories, gateways, and collaborators.
- Instantiate the real service with `@InjectMocks`.
- When the service exposes `findById(Long id)`, test both the success path and the `EntityNotFoundException` path.
- Verify business rules with explicit assertions on returned entities, saved entities, and thrown exceptions.
- Use `ArgumentCaptor<ConcreteType>` when saved or forwarded objects must be inspected.
- Prefer one assertion focus per test method. Multiple related assertions are fine when they validate the same behavior.

## Controller test pattern

- For controller unit tests, mock the service and assert the returned `ResponseEntity`.
- For MVC slice tests, use the project's existing `MockMvc` or `WebMvcTest` pattern only if the codebase already uses it.
- When an endpoint retrieves an entity by id, mock `service.findById(id)` instead of mocking repository access directly.
- Assert status codes according to the contract: `200`, `201`, `204`, and `404` where applicable.

## Mockito usage rules

- Prefer `when(...).thenReturn(...)` and `when(...).thenThrow(...)` with concrete values.
- Use `verify(...)` for meaningful collaboration checks, not for every mocked call.
- Use `verifyNoMoreInteractions(...)` only when the extra strictness adds value and will not make tests brittle.
- Avoid stubbing behavior that the test does not exercise.
- Keep captors typed, for example `ArgumentCaptor<MyEntity>`.

## Coverage targets

- Cover happy path, not-found path, and the main business-rule failure path.
- Add update-flow tests when the service mutates aggregates or collections.
- Add serialization or validation-focused controller tests only when the endpoint contract is the risk being changed.
- Do not add broad integration tests when a unit test is enough unless the task explicitly asks for integration coverage.

## Constraints

- Preserve the project's assertion library and test naming style when it is already consistent.
- Do not introduce helper builders, fixture mothers, or utility layers for a small number of tests.
- Prefer explicit object construction inside the test when it keeps the intent obvious.
- If a test requires many repeated fixtures, extract only the minimum helper surface needed.
