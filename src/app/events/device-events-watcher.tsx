import { useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { notify } from "@/components";
import { tokenStorage } from "@/app/utils/token-storage";
import { appKeys } from "@/features/apps/api";
import { backupKeys } from "@/features/backups/api";
import { dashboardKeys } from "@/features/dashboard/api";
import { deviceKeys, type DeviceEventDto } from "@/features/devices/api";
import { machineKeys } from "@/features/machines/api";
import { operationKeys } from "@/features/operations/api";
import { storageKeys } from "@/features/storage/api";

const EVENTS_URL = "/api/events";

/**
 * Assina os eventos do backend (SSE) e mantém as telas em dia sem recarregar: dispositivo
 * conectado ou desconectado, informações relidas, operação terminada. Não desenha nada.
 */
export const DeviceEventsWatcher = () => {
  const queryClient = useQueryClient();

  useEffect(() => {
    const source = new EventSource(tokenStorage.withToken(EVENTS_URL));

    const handleEvent = (message: MessageEvent<string>) => {
      const event = JSON.parse(message.data) as DeviceEventDto;
      queryClient.invalidateQueries({ queryKey: deviceKeys.all });
      queryClient.invalidateQueries({ queryKey: dashboardKeys.all });

      switch (event.type) {
        case "DISCOVERED":
          notify.success("Novo dispositivo detectado", event.message);
          break;
        case "ONLINE":
          notify.info(event.message);
          break;
        case "OFFLINE":
          notify.warning(event.message);
          break;
        case "OPERATION_FINISHED":
          queryClient.invalidateQueries({ queryKey: operationKeys.all });
          queryClient.invalidateQueries({ queryKey: backupKeys.all });
          queryClient.invalidateQueries({ queryKey: appKeys.all });
          queryClient.invalidateQueries({ queryKey: storageKeys.all });
          queryClient.invalidateQueries({ queryKey: machineKeys.all });
          if (event.message.endsWith("falhou")) {
            notify.error(event.message, event.deviceName ?? undefined);
          } else {
            notify.success(event.message, event.deviceName ?? undefined);
          }
          break;
        case "UPDATED":
          break;
      }
    };

    source.addEventListener("device", handleEvent);
    return () => {
      source.removeEventListener("device", handleEvent);
      source.close();
    };
  }, [queryClient]);

  return null;
};
