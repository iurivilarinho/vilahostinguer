import { Server } from "lucide-react";
import { Link } from "react-router-dom";
import { Typography } from "@/components";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { usePortalInfoQuery } from "@/features/portal/catalog/api";
import { cn } from "@/lib/merge-classes";

type PortalBrandProps = {
  to?: string;
  className?: string;
};

/** Marca da empresa (nome configurado no painel administrativo). */
export const PortalBrand = ({ to = RotasPortal.home, className }: PortalBrandProps) => {
  const { data: info } = usePortalInfoQuery();
  return (
    <Link to={to} className={cn("flex items-center gap-2.5", className)}>
      <span className="flex size-9 items-center justify-center rounded-lg bg-primary text-primary-foreground">
        <Server className="size-5" />
      </span>
      <Typography variant="title-md" as="span">
        {info?.companyName ?? "Servidores"}
      </Typography>
    </Link>
  );
};
