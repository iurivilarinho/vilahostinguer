import type { ReactNode } from "react";
import { cn } from "@/lib/merge-classes";

export type TypographyVariant =
  | "display-xl"
  | "display-lg"
  | "display-md"
  | "display-sm"
  | "title-lg"
  | "title-md"
  | "title-sm"
  | "body-md"
  | "body-sm"
  | "caption"
  | "button"
  | "nav-link"
  | "hero-title"
  | "hero-description"
  | "section-label"
  | "section-heading"
  | "section-intro"
  | "ui-header"
  | "inline-link"
  | "mono";

type TypographyElement = "h1" | "h2" | "h3" | "h4" | "p" | "span" | "div" | "label" | "code";

const defaultElement: Record<TypographyVariant, TypographyElement> = {
  "display-xl": "h1",
  "display-lg": "h1",
  "display-md": "h1",
  "display-sm": "h2",
  "title-lg": "h2",
  "title-md": "h3",
  "title-sm": "h3",
  "body-md": "p",
  "body-sm": "p",
  caption: "span",
  button: "span",
  "nav-link": "span",
  "hero-title": "h1",
  "hero-description": "p",
  "section-label": "span",
  "section-heading": "h2",
  "section-intro": "p",
  "ui-header": "span",
  "inline-link": "span",
  mono: "code",
};

type TypographyProps = {
  variant: TypographyVariant;
  children: ReactNode;
  as?: TypographyElement;
  className?: string;
  title?: string;
  htmlFor?: string;
};

/** Todo texto de conteúdo passa por aqui: a variante define tamanho, peso e o elemento semântico. */
export const Typography = ({ variant, children, as, className, title, htmlFor }: TypographyProps) => {
  const Element = as ?? defaultElement[variant];
  return (
    <Element className={cn(variant, className)} title={title} {...(Element === "label" ? { htmlFor } : {})}>
      {children}
    </Element>
  );
};
