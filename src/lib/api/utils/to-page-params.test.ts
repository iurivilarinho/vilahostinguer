import { describe, expect, it } from "vitest";
import { normalizeSpringPage } from "./normalize-spring-page";
import { toPageParams } from "./to-page-params";

type Item = { name: string; createdAt: string };
type Filter = { search?: string; status?: string[]; online?: boolean };

describe("toPageParams", () => {
  it("achata filtro, ordenação e paginação no formato do Spring", () => {
    const params = toPageParams<Item, Filter>({
      page: 2,
      size: 25,
      sort: [{ by: "createdAt", direction: "desc" }],
      filter: { search: "j4", status: ["READY", "DISCOVERED"], online: false },
    });
    expect(params).toEqual({ page: 2, size: 25, sort: ["createdAt,desc"], search: "j4", status: ["READY", "DISCOVERED"], online: false });
  });

  it("descarta filtros vazios", () => {
    expect(toPageParams<Item, Filter>({ filter: { search: "", status: [] } })).toEqual({});
  });
});

describe("normalizeSpringPage", () => {
  it("converte o Page do Spring mantendo a página 0-based", () => {
    const page = normalizeSpringPage({ content: [1, 2], number: 0, size: 2, totalElements: 5, totalPages: 3, first: true, last: false });
    expect(page).toEqual({ data: [1, 2], page: 0, size: 2, totalElements: 5, totalPages: 3, hasNext: true, hasPrevious: false });
  });
});
