import { describe, expect, it } from "vitest";
import { formatBytes, formatUptime, percent } from "./format";

describe("formatBytes", () => {
  it("usa a unidade adequada com vírgula decimal", () => {
    expect(formatBytes(512)).toBe("512 B");
    expect(formatBytes(1536)).toBe("1,5 KB");
    expect(formatBytes(2_842_804_224)).toBe("2,6 GB");
  });

  it("mostra traço quando o valor é desconhecido", () => {
    expect(formatBytes(null)).toBe("—");
    expect(formatBytes(undefined)).toBe("—");
  });
});

describe("formatUptime", () => {
  it("resume em dias e horas quando passa de um dia", () => {
    expect(formatUptime(147_039)).toBe("1d 16h");
  });

  it("mostra horas e minutos abaixo de um dia", () => {
    expect(formatUptime(3_900)).toBe("1h 5min");
  });
});

describe("percent", () => {
  it("não divide por zero", () => {
    expect(percent(10, 0)).toBe(0);
    expect(percent(25, 100)).toBe(25);
  });
});
