import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Button, FieldWrapper, Input, Typography } from "@/components";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { useLoginMutation } from "../api";
import { AuthCard } from "../components/auth-card";
import { DEFAULT_LOGIN_VALUES, loginSchema, type LoginFormValues } from "../form/schemas";
import { safeReturnPath } from "../utils/safe-return-path";

export const PageLogin = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const returnTo = safeReturnPath(searchParams.get("voltar"));
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema), defaultValues: DEFAULT_LOGIN_VALUES });
  const { mutate: login, isPending } = useLoginMutation({ onSuccess: () => navigate(returnTo, { replace: true }) });

  return (
    <AuthCard
      title="Entrar"
      description="Acesse o painel dos seus servidores."
      footer={
        <Typography variant="body-sm" as="p" className="text-center">
          Ainda não tem conta?{" "}
          <Link to={`${RotasPortal.register}?voltar=${encodeURIComponent(returnTo)}`} className="inline-link">
            Criar conta
          </Link>
        </Typography>
      }
    >
      <form className="flex flex-col gap-4" onSubmit={handleSubmit((values) => login(values))}>
        <FieldWrapper label="E-mail" htmlFor="login-email" error={errors.email?.message}>
          <Input id="login-email" type="email" autoComplete="email" {...register("email")} aria-invalid={Boolean(errors.email)} />
        </FieldWrapper>
        <FieldWrapper label="Senha" htmlFor="login-password" error={errors.password?.message}>
          <Input id="login-password" type="password" autoComplete="current-password" {...register("password")} aria-invalid={Boolean(errors.password)} />
        </FieldWrapper>
        <Button type="submit" size="lg" loading={isPending}>
          Entrar
        </Button>
        <Typography variant="caption" as="p">
          Esqueceu a senha? Fale com o suporte para definir uma nova.
        </Typography>
      </form>
    </AuthCard>
  );
};
