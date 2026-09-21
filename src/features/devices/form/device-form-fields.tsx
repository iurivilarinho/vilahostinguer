import type { FieldErrors, UseFormRegister } from "react-hook-form";
import { Link } from "react-router-dom";
import { FieldWrapper, Input, Select, Textarea, Typography } from "@/components";
import { Rotas } from "@/app/variables/rotas";
import { useCredentialsQuery } from "@/features/credentials/api";
import type { DeviceFormValues } from "./schema";

const CREDENTIAL_OPTIONS_PARAMS = { page: 0, size: 100, filter: { active: true } };

type DeviceFormFieldsProps = {
  register: UseFormRegister<DeviceFormValues>;
  errors: FieldErrors<DeviceFormValues>;
  idPrefix: string;
};

/** Campos do dispositivo, usados no cadastro manual e na tela de configurações do dispositivo. */
export const DeviceFormFields = ({ register, errors, idPrefix }: DeviceFormFieldsProps) => {
  const { data: credentials } = useCredentialsQuery(CREDENTIAL_OPTIONS_PARAMS);

  return (
    <div className="flex flex-col gap-4">
      <FieldWrapper label="Nome" htmlFor={`${idPrefix}-name`} error={errors.name?.message}>
        <Input id={`${idPrefix}-name`} {...register("name")} aria-invalid={Boolean(errors.name)} />
      </FieldWrapper>
      <div className="grid grid-cols-[1fr_7rem] gap-3">
        <FieldWrapper label="Endereço" htmlFor={`${idPrefix}-host`} error={errors.host?.message} description="IP ou nome na rede">
          <Input id={`${idPrefix}-host`} placeholder="192.168.0.50" {...register("host")} aria-invalid={Boolean(errors.host)} />
        </FieldWrapper>
        <FieldWrapper label="Porta SSH" htmlFor={`${idPrefix}-port`} error={errors.port?.message}>
          <Input id={`${idPrefix}-port`} type="number" inputMode="numeric" {...register("port")} aria-invalid={Boolean(errors.port)} />
        </FieldWrapper>
      </div>
      <FieldWrapper label="Credencial" htmlFor={`${idPrefix}-credential`}>
        <Select id={`${idPrefix}-credential`} {...register("credentialId")}>
          <option value="">Nenhuma (só monitorar se está online)</option>
          {credentials?.data.map((credential) => (
            <option key={credential.id} value={String(credential.id)}>
              {credential.name} — {credential.username}
            </option>
          ))}
        </Select>
      </FieldWrapper>
      {credentials && credentials.data.length === 0 && (
        <Typography variant="caption" as="p">
          Nenhuma credencial cadastrada.{" "}
          <Link to={Rotas.credentials} className="inline-link">
            Cadastrar em Credenciais
          </Link>
        </Typography>
      )}
      <FieldWrapper label="Anotações" htmlFor={`${idPrefix}-notes`} error={errors.notes?.message}>
        <Textarea id={`${idPrefix}-notes`} rows={3} {...register("notes")} />
      </FieldWrapper>
    </div>
  );
};
