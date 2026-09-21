import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Button, FieldWrapper, Input, Switch, Typography } from "@/components";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { usePortalInfoQuery } from "../../catalog/api";
import { useRegisterMutation } from "../api";
import { AuthCard } from "../components/auth-card";
import { DEFAULT_REGISTER_VALUES, registerSchema, type RegisterFormValues } from "../form/schemas";
import { safeReturnPath } from "../utils/safe-return-path";

export const PageRegister = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const returnTo = safeReturnPath(searchParams.get("voltar"));
  const { data: info } = usePortalInfoQuery();
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerSchema), defaultValues: DEFAULT_REGISTER_VALUES });
  const { mutate: createAccount, isPending } = useRegisterMutation({ onSuccess: () => navigate(returnTo, { replace: true }) });

  if (info && !info.registrationOpen) {
    return (
      <AuthCard title="Cadastro fechado" description="No momento não estamos aceitando clientes novos.">
        <Link to={RotasPortal.login} className="inline-link">
          Já tenho conta
        </Link>
      </AuthCard>
    );
  }

  return (
    <AuthCard
      title="Criar conta"
      description="Leva um minuto. Depois é só escolher o plano e pagar."
      footer={
        <Typography variant="body-sm" as="p" className="text-center">
          Já tem conta?{" "}
          <Link to={`${RotasPortal.login}?voltar=${encodeURIComponent(returnTo)}`} className="inline-link">
            Entrar
          </Link>
        </Typography>
      }
    >
      <form
        className="flex flex-col gap-4"
        onSubmit={handleSubmit((values) =>
          createAccount({
            name: values.name,
            email: values.email,
            password: values.password,
            phone: values.phone,
            document: values.document,
            acceptTerms: values.acceptTerms,
          }),
        )}
      >
        <FieldWrapper label="Nome completo" htmlFor="register-name" error={errors.name?.message}>
          <Input id="register-name" autoComplete="name" {...register("name")} aria-invalid={Boolean(errors.name)} />
        </FieldWrapper>
        <FieldWrapper label="E-mail" htmlFor="register-email" error={errors.email?.message}>
          <Input id="register-email" type="email" autoComplete="email" {...register("email")} aria-invalid={Boolean(errors.email)} />
        </FieldWrapper>
        <div className="grid gap-4 sm:grid-cols-2">
          <FieldWrapper label="Telefone" htmlFor="register-phone" error={errors.phone?.message}>
            <Input id="register-phone" type="tel" autoComplete="tel" {...register("phone")} />
          </FieldWrapper>
          <FieldWrapper label="CPF ou CNPJ" htmlFor="register-document" error={errors.document?.message}>
            <Input id="register-document" inputMode="numeric" {...register("document")} />
          </FieldWrapper>
        </div>
        <FieldWrapper label="Senha" htmlFor="register-password" error={errors.password?.message} description="Mínimo de 8 caracteres">
          <Input id="register-password" type="password" autoComplete="new-password" {...register("password")} aria-invalid={Boolean(errors.password)} />
        </FieldWrapper>
        <FieldWrapper label="Repita a senha" htmlFor="register-confirmation" error={errors.confirmation?.message}>
          <Input id="register-confirmation" type="password" autoComplete="new-password" {...register("confirmation")} />
        </FieldWrapper>
        <div className="flex flex-col gap-1">
          <label htmlFor="register-terms" className="flex items-center gap-3">
            <Switch id="register-terms" checked={watch("acceptTerms")} onCheckedChange={(value) => setValue("acceptTerms", value, { shouldValidate: true })} />
            <Typography variant="body-sm" as="span">
              Li e aceito os termos de uso e a política de privacidade
            </Typography>
          </label>
          {errors.acceptTerms?.message && (
            <Typography variant="caption" className="text-destructive-foreground">
              {errors.acceptTerms.message}
            </Typography>
          )}
        </div>
        <Button type="submit" size="lg" loading={isPending}>
          Criar conta
        </Button>
      </form>
    </AuthCard>
  );
};
