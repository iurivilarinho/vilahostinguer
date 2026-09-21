import { Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "@/app/layout/app-layout";
import { Rotas } from "@/app/variables/rotas";
import { PageBackups } from "@/features/backups";
import { PageCredentials } from "@/features/credentials";
import { PageDashboard } from "@/features/dashboard";
import {
  PageDevice,
  PageDevices,
  SectionActivity,
  SectionApps,
  SectionBackups,
  SectionFiles,
  SectionMachines,
  SectionOverview,
  SectionSettings,
  SectionStorage,
  SectionTerminal,
} from "@/features/devices";
import { PageMachines } from "@/features/machines";
import { PageOperations } from "@/features/operations";
import { PageRemoteAccess } from "@/features/remote-access";
import { PageDesignSystem } from "@/features/public/design-system";
import { PageSettings } from "@/features/settings";
import { PageTerminal } from "@/features/terminal";

export const AppRouter = () => (
  <Routes>
    <Route element={<AppLayout />}>
      <Route path={Rotas.dashboard} element={<PageDashboard />} />
      <Route path={Rotas.devices.list} element={<PageDevices />} />
      <Route path={`${Rotas.devices.list}/:id`} element={<PageDevice />}>
        <Route index element={<Navigate to="visao-geral" replace />} />
        <Route path="visao-geral" element={<SectionOverview />} />
        <Route path="terminal" element={<SectionTerminal />} />
        <Route path="aplicativos" element={<SectionApps />} />
        <Route path="maquinas" element={<SectionMachines />} />
        <Route path="arquivos" element={<SectionFiles />} />
        <Route path="backups" element={<SectionBackups />} />
        <Route path="armazenamento" element={<SectionStorage />} />
        <Route path="atividades" element={<SectionActivity />} />
        <Route path="configuracoes" element={<SectionSettings />} />
      </Route>
      <Route path={Rotas.terminal} element={<PageTerminal />} />
      <Route path={Rotas.machines} element={<PageMachines />} />
      <Route path={Rotas.remoteAccess} element={<PageRemoteAccess />} />
      <Route path={Rotas.backups} element={<PageBackups />} />
      <Route path={Rotas.operations} element={<PageOperations />} />
      <Route path={Rotas.credentials} element={<PageCredentials />} />
      <Route path={Rotas.settings} element={<PageSettings />} />
      <Route path={Rotas.designSystem} element={<PageDesignSystem />} />
      <Route path="*" element={<Navigate to={Rotas.dashboard} replace />} />
    </Route>
  </Routes>
);
