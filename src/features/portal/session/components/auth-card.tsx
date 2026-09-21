import type { ReactNode } from "react";
import { Card, Typography } from "@/components";

type AuthCardProps = {
  title: string;
  description: string;
  children: ReactNode;
  footer?: ReactNode;
};

/** Cartão central das telas de acesso. */
export const AuthCard = ({ title, description, children, footer }: AuthCardProps) => (
  <div className="flex justify-center px-4 py-12 md:py-20">
    <Card className="flex w-full max-w-md flex-col gap-6 p-6 md:p-8">
      <div className="flex flex-col gap-1">
        <Typography variant="display-sm">{title}</Typography>
        <Typography variant="body-sm" className="text-muted-foreground">
          {description}
        </Typography>
      </div>
      {children}
      {footer && <div className="border-t border-border pt-4">{footer}</div>}
    </Card>
  </div>
);
