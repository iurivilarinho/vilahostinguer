import { Navigate, Route, Routes } from "react-router-dom";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { PageAccount } from "@/features/portal/account";
import { PageBilling, PageInvoice } from "@/features/portal/billing";
import { PageHome } from "@/features/portal/catalog";
import { PageCheckout } from "@/features/portal/checkout";
import { PageDashboard } from "@/features/portal/dashboard";
import { PageServer, PageServers } from "@/features/portal/servers";
import { PageLogin, PageRegister } from "@/features/portal/session";
import { AreaLayout } from "../layout/area-layout";
import { PublicLayout } from "../layout/public-layout";
import { RedirectIfSession, RequireSession } from "./require-session";

export const PortalRouter = () => (
  <Routes>
    <Route element={<PublicLayout />}>
      <Route path={RotasPortal.home} element={<PageHome />} />
      <Route element={<RedirectIfSession />}>
        <Route path={RotasPortal.login} element={<PageLogin />} />
        <Route path={RotasPortal.register} element={<PageRegister />} />
      </Route>
    </Route>
    <Route element={<RequireSession />}>
      <Route element={<AreaLayout />}>
        <Route path={RotasPortal.area} element={<PageDashboard />} />
        <Route path={RotasPortal.servers} element={<PageServers />} />
        <Route path={`${RotasPortal.servers}/:id`} element={<PageServer />} />
        <Route path={`${RotasPortal.servers}/:id/:section`} element={<PageServer />} />
        <Route path={`${RotasPortal.area}/contratar/:planId`} element={<PageCheckout />} />
        <Route path={RotasPortal.billing} element={<PageBilling />} />
        <Route path={`${RotasPortal.area}/faturas/:id`} element={<PageInvoice />} />
        <Route path={RotasPortal.account} element={<PageAccount />} />
      </Route>
    </Route>
    <Route path="*" element={<Navigate to={RotasPortal.home} replace />} />
  </Routes>
);
