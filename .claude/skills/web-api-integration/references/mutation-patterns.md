# Mutation Pattern

## API function

```ts
const createEntity = async (payload: CreateEntityRequest): Promise<EntityApiDto> => {
  const { data } = await apiClient.post("/entities", payload);
  return data;
};
```

## Hook

```ts
import { resolveErrorMessage } from "@/api/utils/resolveErrorMessage";
import { resolveSuccessMessage } from "@/api/utils/resolveSuccessMessage";

export const useCreateEntity = (options?: MutationOptions<EntityApiDto, CreateEntityRequest>) => {
  return useMutation({
    mutationFn: createEntity,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["entities"] });

      if (options?.showToast !== false) {
        toast.success(
          resolveSuccessMessage({
            successMessage: options?.successMessage,
            data,
            variables,
            defaultMessage: "Entidade criada com sucesso",
          }),
        );
      }

      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        toast.error(
          resolveErrorMessage({
            error,
            fallbackMessage: options?.errorMessage ?? "Erro ao criar entidade",
          }),
        );
      }

      options?.onError?.(error);
    },
  });
};
```

## Rules

- Mutations must invalidate related queries in `onSuccess`
- `options?.onSuccess` and `options?.onError` callbacks must always be called after internal logic
- Toast is shown by default — opt out via `showToast: false`
- `successMessage` accepts either a static string or a function `(data, variables) => string`
- Success messages must use `resolveSuccessMessage` — never inline the ternary resolution
- Error messages must use `resolveErrorMessage` — never access `error.message` directly

---

# resolveSuccessMessage

```ts
import { resolveSuccessMessage } from "@/api/utils/resolveSuccessMessage";
```

Resolves the success message for a mutation from `MutationOptions`. Handles both static strings and dynamic functions `(data, variables) => string`, falling back to `defaultMessage`.

```ts
toast.success(
  resolveSuccessMessage({
    successMessage: options?.successMessage,
    data,
    variables,
    defaultMessage: "Operação realizada com sucesso",
  }),
);
```

Always use `resolveSuccessMessage` in mutation `onSuccess` handlers — never inline the `typeof` ternary.

---

# resolveErrorMessage

```ts
import { resolveErrorMessage } from "@/api/utils/resolveErrorMessage";
```

Extracts the error message from an `AxiosError` response body, falls back to `error.message`, and finally to `"Ocorreu um erro inesperado"`.

```ts
toast.error(
  resolveErrorMessage({
    error,
    fallbackMessage: "Erro ao realizar operação",
  }),
);
```

Always use `resolveErrorMessage` in mutation `onError` handlers — never inline `error.message`.

---

# Mutation With Conditional Toast

Control toast display from the component:

```ts
mutate(payload, {
  showToast: false, // suppresses both success and error toasts
  onSuccess: (data) => {
    // handle success manually
  },
});
```

## Rules

- Toast behavior is opt-out — `showToast: false` suppresses both success and error toasts
- Mutations must remain reusable and UI-agnostic
- All toast and callback logic lives in the hook, not the component
