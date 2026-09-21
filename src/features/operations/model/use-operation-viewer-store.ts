import { create } from "zustand";

type OperationViewerState = {
  operationId: number | null;
  open: (operationId: number) => void;
  close: () => void;
};

/** Qual operação está aberta no diálogo de saída. Qualquer tela abre; o diálogo é um só, no layout. */
export const useOperationViewerStore = create<OperationViewerState>((set) => ({
  operationId: null,
  open: (operationId) => set({ operationId }),
  close: () => set({ operationId: null }),
}));

export const openOperationViewer = (operationId: number) => useOperationViewerStore.getState().open(operationId);
