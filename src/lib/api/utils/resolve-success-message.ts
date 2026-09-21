import type { MutationOptions } from "../types";

type ResolveSuccessMessageParams<TData, TVariables> = {
  successMessage: MutationOptions<TData, TVariables>["successMessage"];
  data: TData;
  variables: TVariables;
  defaultMessage: string;
};

export const resolveSuccessMessage = <TData, TVariables>({
  successMessage,
  data,
  variables,
  defaultMessage,
}: ResolveSuccessMessageParams<TData, TVariables>): string => {
  if (typeof successMessage === "function") {
    return successMessage(data, variables);
  }
  return successMessage ?? defaultMessage;
};
