import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { Button, Card, CardContent, CardHeader, FieldWrapper, Input, PageHeader, Typography } from "@/components";
import { useChangePasswordMutation, useSessionQuery, useUpdateProfileMutation } from "../../session/api";
import {
  DEFAULT_PASSWORD_VALUES,
  passwordSchema,
  profileSchema,
  type PasswordFormValues,
  type ProfileFormValues,
} from "../../session/form/schemas";

/** Dados cadastrais e senha de acesso ao painel. */
export const PageAccount = () => {
  const { data: customer } = useSessionQuery();
  const profileForm = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { name: "", email: "", phone: "", document: "" },
  });
  const passwordForm = useForm<PasswordFormValues>({ resolver: zodResolver(passwordSchema), defaultValues: DEFAULT_PASSWORD_VALUES });
  const { mutate: updateProfile, isPending: isSaving } = useUpdateProfileMutation();
  const { mutate: changePassword, isPending: isChanging } = useChangePasswordMutation({ onSuccess: () => passwordForm.reset(DEFAULT_PASSWORD_VALUES) });

  useEffect(() => {
    if (customer) {
      profileForm.reset({ name: customer.name, email: customer.email, phone: customer.phone ?? "", document: customer.document ?? "" });
    }
  }, [customer, profileForm]);

  return (
    <>
      <PageHeader title="Conta" description="Seus dados e a senha de acesso a este painel." />
      <Card>
        <CardHeader>
          <Typography variant="title-sm">Dados pessoais</Typography>
        </CardHeader>
        <CardContent>
          <form className="grid gap-4 sm:grid-cols-2" onSubmit={profileForm.handleSubmit((values) => updateProfile(values))}>
            <FieldWrapper label="Nome completo" htmlFor="account-name" error={profileForm.formState.errors.name?.message}>
              <Input id="account-name" autoComplete="name" {...profileForm.register("name")} />
            </FieldWrapper>
            <FieldWrapper label="E-mail" htmlFor="account-email" error={profileForm.formState.errors.email?.message} description="Também é o seu login.">
              <Input id="account-email" type="email" autoComplete="email" {...profileForm.register("email")} />
            </FieldWrapper>
            <FieldWrapper label="Telefone" htmlFor="account-phone" error={profileForm.formState.errors.phone?.message}>
              <Input id="account-phone" type="tel" autoComplete="tel" {...profileForm.register("phone")} />
            </FieldWrapper>
            <FieldWrapper label="CPF ou CNPJ" htmlFor="account-document" error={profileForm.formState.errors.document?.message}>
              <Input id="account-document" inputMode="numeric" {...profileForm.register("document")} />
            </FieldWrapper>
            <div className="sm:col-span-2">
              <Button type="submit" loading={isSaving}>
                Salvar dados
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <div className="flex flex-col gap-1">
            <Typography variant="title-sm">Senha do painel</Typography>
            <Typography variant="caption">Trocar a senha encerra as outras sessões abertas.</Typography>
          </div>
        </CardHeader>
        <CardContent>
          <form
            className="grid gap-4 sm:grid-cols-3 sm:items-end"
            onSubmit={passwordForm.handleSubmit((values) => changePassword({ currentPassword: values.currentPassword, newPassword: values.newPassword }))}
          >
            <FieldWrapper label="Senha atual" htmlFor="account-current" error={passwordForm.formState.errors.currentPassword?.message}>
              <Input id="account-current" type="password" autoComplete="current-password" {...passwordForm.register("currentPassword")} />
            </FieldWrapper>
            <FieldWrapper label="Nova senha" htmlFor="account-new" error={passwordForm.formState.errors.newPassword?.message}>
              <Input id="account-new" type="password" autoComplete="new-password" {...passwordForm.register("newPassword")} />
            </FieldWrapper>
            <FieldWrapper label="Repita a nova senha" htmlFor="account-confirmation" error={passwordForm.formState.errors.confirmation?.message}>
              <Input id="account-confirmation" type="password" autoComplete="new-password" {...passwordForm.register("confirmation")} />
            </FieldWrapper>
            <div className="sm:col-span-3">
              <Button type="submit" loading={isChanging}>
                Trocar senha
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </>
  );
};
