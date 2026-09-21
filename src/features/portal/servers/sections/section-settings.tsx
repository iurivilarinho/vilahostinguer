import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { Button, Card, CardContent, CardHeader, ConfirmDialog, FieldWrapper, Input, Textarea, Typography } from "@/components";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { formatDate } from "@/lib/format";
import { useCancelSubscriptionMutation, useKeepSubscriptionMutation } from "../../billing/api";
import { OptionCard } from "../../checkout/components/option-card";
import { useServerPasswordMutation, type ServerDto } from "../api";
import { cancelSchema, DEFAULT_CANCEL_VALUES, DEFAULT_SERVER_PASSWORD_VALUES, serverPasswordSchema, type CancelFormValues, type ServerPasswordFormValues } from "../form/schemas";

/** Senha do usuário do servidor e cancelamento da assinatura. */
export const SectionSettings = ({ server }: { server: ServerDto }) => {
  const navigate = useNavigate();
  const [confirmingCancel, setConfirmingCancel] = useState<CancelFormValues | null>(null);
  const active = server.subscriptionStatus === "ACTIVE";
  const passwordForm = useForm<ServerPasswordFormValues>({ resolver: zodResolver(serverPasswordSchema), defaultValues: DEFAULT_SERVER_PASSWORD_VALUES });
  const cancelForm = useForm<CancelFormValues>({ resolver: zodResolver(cancelSchema), defaultValues: DEFAULT_CANCEL_VALUES });
  const { mutate: changePassword, isPending: isChanging } = useServerPasswordMutation({ onSuccess: () => passwordForm.reset(DEFAULT_SERVER_PASSWORD_VALUES) });
  const { mutate: cancel, isPending: isCanceling } = useCancelSubscriptionMutation({
    onSuccess: (subscription) => {
      setConfirmingCancel(null);
      if (subscription.status === "CANCELED") {
        navigate(RotasPortal.servers);
      }
    },
  });
  const { mutate: keep, isPending: isKeeping } = useKeepSubscriptionMutation();
  const atPeriodEnd = cancelForm.watch("atPeriodEnd");

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader>
          <div className="flex flex-col gap-1">
            <Typography variant="title-sm">Senha de {server.username}</Typography>
            <Typography variant="caption">Vale para o SSH e para o sudo dentro do servidor.</Typography>
          </div>
        </CardHeader>
        <CardContent>
          <form
            className="grid gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end"
            onSubmit={passwordForm.handleSubmit((values) => changePassword({ id: server.id, password: values.password }))}
          >
            <FieldWrapper label="Nova senha" htmlFor="server-password" error={passwordForm.formState.errors.password?.message}>
              <Input id="server-password" type="password" autoComplete="new-password" {...passwordForm.register("password")} disabled={!active} />
            </FieldWrapper>
            <FieldWrapper label="Repita a senha" htmlFor="server-password-confirmation" error={passwordForm.formState.errors.confirmation?.message}>
              <Input id="server-password-confirmation" type="password" autoComplete="new-password" {...passwordForm.register("confirmation")} disabled={!active} />
            </FieldWrapper>
            <Button type="submit" loading={isChanging} disabled={!active}>
              Trocar senha
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div className="flex flex-col gap-1">
            <Typography variant="title-sm">Cancelar assinatura</Typography>
            <Typography variant="caption">Sem multa. Você escolhe quando o servidor é apagado.</Typography>
          </div>
        </CardHeader>
        <CardContent>
          {server.cancelAtPeriodEnd ? (
            <div className="flex flex-wrap items-center justify-between gap-3">
              <Typography variant="body-sm">O servidor será apagado em {formatDate(server.nextDueDate)}.</Typography>
              <Button variant="outline" loading={isKeeping} onClick={() => keep(server.id)}>
                Manter assinatura
              </Button>
            </div>
          ) : (
            <form className="flex flex-col gap-4" onSubmit={cancelForm.handleSubmit((values) => setConfirmingCancel(values))}>
              <div role="radiogroup" className="grid gap-3 sm:grid-cols-2">
                <OptionCard selected={atPeriodEnd} onSelect={() => cancelForm.setValue("atPeriodEnd", true)} disabled={!active}>
                  <Typography variant="ui-header">No fim do período pago</Typography>
                  <Typography variant="caption">O servidor funciona até {formatDate(server.nextDueDate)} e não renova.</Typography>
                </OptionCard>
                <OptionCard selected={!atPeriodEnd} onSelect={() => cancelForm.setValue("atPeriodEnd", false)}>
                  <Typography variant="ui-header">Agora</Typography>
                  <Typography variant="caption">O servidor é apagado hoje, com tudo o que tem dentro.</Typography>
                </OptionCard>
              </div>
              <FieldWrapper label="Motivo (opcional)" htmlFor="cancel-reason" error={cancelForm.formState.errors.reason?.message}>
                <Textarea id="cancel-reason" rows={2} {...cancelForm.register("reason")} />
              </FieldWrapper>
              <Button type="submit" variant="destructive" className="self-start">
                Cancelar assinatura
              </Button>
            </form>
          )}
        </CardContent>
      </Card>

      <ConfirmDialog
        open={confirmingCancel !== null}
        onOpenChange={(open) => !open && setConfirmingCancel(null)}
        title={`Cancelar ${server.hostname}?`}
        description={
          confirmingCancel?.atPeriodEnd
            ? `O servidor continua funcionando até ${formatDate(server.nextDueDate)} e então é apagado.`
            : "O servidor é apagado agora, com tudo o que tem dentro. Não dá para desfazer."
        }
        confirmLabel="Cancelar assinatura"
        destructive
        loading={isCanceling}
        typeToConfirm={confirmingCancel?.atPeriodEnd ? undefined : server.hostname}
        onConfirm={() => confirmingCancel && cancel({ id: server.id, atPeriodEnd: confirmingCancel.atPeriodEnd, reason: confirmingCancel.reason })}
      />
    </div>
  );
};
