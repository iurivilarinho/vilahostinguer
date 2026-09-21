import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { AppSheet, Button, FieldWrapper, Input, Select, Switch, Textarea, Typography } from "@/components";
import { useCreateCredentialMutation, useUpdateCredentialMutation, type CredentialDto } from "../api";
import { credentialFormSchema, DEFAULT_CREDENTIAL_FORM_VALUES, type CredentialFormValues } from "./schema";

type CredentialSheetProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  credential?: CredentialDto;
};

export const CredentialSheet = ({ open, onOpenChange, credential }: CredentialSheetProps) => {
  const mode = credential ? "edit" : "create";
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<CredentialFormValues>({
    resolver: zodResolver(credentialFormSchema),
    defaultValues: DEFAULT_CREDENTIAL_FORM_VALUES,
  });
  const { mutateAsync: createCredential, isPending: isCreating } = useCreateCredentialMutation();
  const { mutateAsync: updateCredential, isPending: isUpdating } = useUpdateCredentialMutation();
  const isSaving = isCreating || isUpdating;
  const authType = watch("authType");

  useEffect(() => {
    if (mode === "edit" && credential) {
      reset({
        mode: "edit",
        name: credential.name,
        username: credential.username,
        authType: credential.authType,
        secret: "",
        passphrase: "",
        defaultCredential: credential.defaultCredential,
      });
      return;
    }
    reset(DEFAULT_CREDENTIAL_FORM_VALUES);
  }, [mode, credential, reset, open]);

  const onSubmit = async (values: CredentialFormValues) => {
    const payload = {
      name: values.name,
      username: values.username,
      authType: values.authType,
      secret: values.secret,
      passphrase: values.authType === "PRIVATE_KEY" ? values.passphrase : "",
      defaultCredential: values.defaultCredential,
    };
    if (mode === "create") {
      await createCredential(payload);
    }
    if (mode === "edit" && credential) {
      await updateCredential({ id: credential.id, ...payload });
    }
    onOpenChange(false);
  };

  return (
    <AppSheet
      open={open}
      onOpenChange={onOpenChange}
      title={mode === "create" ? "Nova credencial" : "Editar credencial"}
      description="O segredo é cifrado com a sua conta do Windows antes de ser salvo."
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSaving}>
            Cancelar
          </Button>
          <Button type="submit" form="credential-form" loading={isSaving}>
            {isSaving ? "Salvando..." : "Salvar"}
          </Button>
        </>
      }
    >
      <form id="credential-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <FieldWrapper label="Nome" htmlFor="credential-name" error={errors.name?.message} description="Para reconhecer na lista, ex.: Root dos celulares">
          <Input id="credential-name" {...register("name")} aria-invalid={Boolean(errors.name)} />
        </FieldWrapper>
        <FieldWrapper label="Usuário" htmlFor="credential-username" error={errors.username?.message}>
          <Input id="credential-username" autoComplete="off" {...register("username")} aria-invalid={Boolean(errors.username)} />
        </FieldWrapper>
        <FieldWrapper label="Forma de autenticação" htmlFor="credential-auth-type">
          <Select id="credential-auth-type" {...register("authType")}>
            <option value="PASSWORD">Senha</option>
            <option value="PRIVATE_KEY">Chave privada</option>
          </Select>
        </FieldWrapper>
        {authType === "PASSWORD" ? (
          <FieldWrapper
            label="Senha"
            htmlFor="credential-secret"
            error={errors.secret?.message}
            description={mode === "edit" ? "Deixe em branco para manter a atual" : undefined}
          >
            <Input id="credential-secret" type="password" autoComplete="new-password" {...register("secret")} aria-invalid={Boolean(errors.secret)} />
          </FieldWrapper>
        ) : (
          <>
            <FieldWrapper
              label="Chave privada"
              htmlFor="credential-secret"
              error={errors.secret?.message}
              description={mode === "edit" ? "Deixe em branco para manter a atual" : "Conteúdo do arquivo (OpenSSH ou PEM)"}
            >
              <Textarea
                id="credential-secret"
                rows={8}
                spellCheck={false}
                className="font-mono text-xs"
                placeholder="-----BEGIN OPENSSH PRIVATE KEY-----"
                {...register("secret")}
                aria-invalid={Boolean(errors.secret)}
              />
            </FieldWrapper>
            <FieldWrapper label="Senha da chave" htmlFor="credential-passphrase" description="Só se a chave tiver senha">
              <Input id="credential-passphrase" type="password" autoComplete="new-password" {...register("passphrase")} />
            </FieldWrapper>
          </>
        )}
        <div className="flex items-start justify-between gap-4 rounded-lg border border-border p-4">
          <div className="flex flex-col gap-1">
            <Typography variant="ui-header" as="label" htmlFor="credential-default">
              Credencial padrão
            </Typography>
            <Typography variant="caption" as="p">
              Usada sozinha quando um dispositivo novo é conectado.
            </Typography>
          </div>
          <Switch id="credential-default" checked={watch("defaultCredential")} onCheckedChange={(value) => setValue("defaultCredential", value)} />
        </div>
      </form>
    </AppSheet>
  );
};
