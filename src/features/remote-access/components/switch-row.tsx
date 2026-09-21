import { Switch, Typography } from "@/components";

type SwitchRowProps = {
  id: string;
  title: string;
  description: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
};

/** Linha de formulário com título, explicação e um interruptor à direita. */
export const SwitchRow = ({ id, title, description, checked, onCheckedChange }: SwitchRowProps) => (
  <div className="flex items-start justify-between gap-4 rounded-lg border border-border p-4">
    <div className="flex flex-col gap-1">
      <Typography variant="ui-header" as="label" htmlFor={id}>
        {title}
      </Typography>
      <Typography variant="caption" as="p">
        {description}
      </Typography>
    </div>
    <Switch id={id} checked={checked} onCheckedChange={onCheckedChange} />
  </div>
);
