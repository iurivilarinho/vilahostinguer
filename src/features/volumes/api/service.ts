import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { machineKeys } from "@/features/machines/api";
import { operationKeys, type OperationDto } from "@/features/operations/api";
import { api } from "@/lib/api/clients";
import type { MutationOptions, PaginatedApiResponse, QueryOptions, SpringPage } from "@/lib/api/types";
import { normalizeSpringPage } from "@/lib/api/utils/normalize-spring-page";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { toPageParams } from "@/lib/api/utils/to-page-params";
import { volumeKeys } from "./keys";
import type {
  GetVolumesParams,
  VolumeAttachRequest,
  VolumeDeleteRequest,
  VolumeDto,
  VolumeRequest,
  VolumeServerDto,
  VolumeStatus,
} from "./types";

const BUSY_REFRESH_MS = 3_000;
const IDLE_REFRESH_MS = 30_000;
const BUSY: VolumeStatus[] = ["ATTACHING", "DETACHING", "WAITING"];

const getVolumes = async (params?: GetVolumesParams): Promise<PaginatedApiResponse<VolumeDto>> => {
  const { data } = await api.get<SpringPage<VolumeDto>>("/volumes", { params: toPageParams(params) });
  return normalizeSpringPage(data);
};

const getVolumeServer = async (): Promise<VolumeServerDto> => {
  const { data } = await api.get<VolumeServerDto>("/volumes/host");
  return data;
};

const createVolume = async (payload: VolumeRequest): Promise<VolumeDto> => {
  const { data } = await api.post<VolumeDto>("/volumes", payload);
  return data;
};

const attachVolume = async ({ id, ...payload }: VolumeAttachRequest): Promise<OperationDto> => {
  const { data } = await api.post<OperationDto>(`/volumes/${id}/attach`, payload);
  return data;
};

const detachVolume = async (id: number): Promise<OperationDto> => {
  const { data } = await api.post<OperationDto>(`/volumes/${id}/detach`);
  return data;
};

const releaseVolume = async (id: number): Promise<VolumeDto> => {
  const { data } = await api.post<VolumeDto>(`/volumes/${id}/release`);
  return data;
};

const deleteVolume = async ({ id, confirmation }: VolumeDeleteRequest): Promise<VolumeDto> => {
  const { data } = await api.post<VolumeDto>(`/volumes/${id}/delete`, { confirmation });
  return data;
};

/** Enquanto algum disco estiver conectando, desconectando ou esperando o dispositivo, a lista se atualiza sozinha. */
export const useVolumesQuery = (params?: GetVolumesParams, options?: QueryOptions<PaginatedApiResponse<VolumeDto>>) =>
  useQuery({
    queryKey: volumeKeys.list(params),
    queryFn: () => getVolumes(params),
    refetchInterval: (query) =>
      query.state.data?.data.some((volume) => BUSY.includes(volume.status)) ? BUSY_REFRESH_MS : IDLE_REFRESH_MS,
    ...options,
  });

export const useVolumeServerQuery = (options?: QueryOptions<VolumeServerDto>) =>
  useQuery({
    queryKey: volumeKeys.host(),
    queryFn: getVolumeServer,
    refetchInterval: IDLE_REFRESH_MS,
    ...options,
  });

/** Mutação padrão dos discos: atualiza discos (e máquinas e atividades, que mudam junto) e avisa. */
const useVolumeMutation = <TData, TVariables>(
  mutationFn: (variables: TVariables) => Promise<TData>,
  defaultMessage: (data: TData) => string,
  errorMessage: string,
  options?: MutationOptions<TData, TVariables>,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: volumeKeys.all });
      queryClient.invalidateQueries({ queryKey: machineKeys.all });
      queryClient.invalidateQueries({ queryKey: operationKeys.all });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: defaultMessage(data) }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error: Error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? errorMessage));
      }
      options?.onError?.(error);
    },
  });
};

export const useCreateVolumeMutation = (options?: MutationOptions<VolumeDto, VolumeRequest>) =>
  useVolumeMutation(createVolume, (data) => `Disco ${data.name} criado`, "Não foi possível criar o disco", options);

export const useAttachVolumeMutation = (options?: MutationOptions<OperationDto, VolumeAttachRequest>) =>
  useVolumeMutation(attachVolume, (data) => `${data.title}: iniciado`, "Não foi possível conectar o disco", options);

export const useDetachVolumeMutation = (options?: MutationOptions<OperationDto, number>) =>
  useVolumeMutation(detachVolume, (data) => `${data.title}: iniciado`, "Não foi possível desconectar o disco", options);

export const useReleaseVolumeMutation = (options?: MutationOptions<VolumeDto, number>) =>
  useVolumeMutation(releaseVolume, (data) => `Disco ${data.name} liberado`, "Não foi possível liberar o disco", options);

export const useDeleteVolumeMutation = (options?: MutationOptions<VolumeDto, VolumeDeleteRequest>) =>
  useVolumeMutation(deleteVolume, (data) => `Disco ${data.name} apagado`, "Não foi possível apagar o disco", options);
