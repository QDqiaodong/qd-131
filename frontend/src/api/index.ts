import axios from 'axios'
import type { ApiResponse, Prop, Crew, BindingDetail, ScheduleChangeLog, ConflictCheckResponse, PropCreateRequest, CrewCreateRequest, BindingCreateRequest, BindingUpdateRequest } from '@/types'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export const propApi = {
  getAll: (sceneType?: string, status?: string) => {
    const params: Record<string, string> = {}
    if (sceneType) params.sceneType = sceneType
    if (status) params.status = status
    return api.get<ApiResponse<Prop[]>>('/props', { params })
  },
  getById: (id: number) => api.get<ApiResponse<Prop>>(`/props/${id}`),
  getByCode: (code: string) => api.get<ApiResponse<Prop>>(`/props/code/${code}`),
  create: (data: PropCreateRequest) => api.post<ApiResponse<Prop>>('/props', data),
  update: (id: number, data: PropCreateRequest) => api.put<ApiResponse<Prop>>(`/props/${id}`, data),
  delete: (id: number) => api.delete<ApiResponse<void>>(`/props/${id}`)
}

export const crewApi = {
  getAll: (status?: string) => {
    const params: Record<string, string> = {}
    if (status) params.status = status
    return api.get<ApiResponse<Crew[]>>('/crews', { params })
  },
  getById: (id: number) => api.get<ApiResponse<Crew>>(`/crews/${id}`),
  getByName: (name: string) => api.get<ApiResponse<Crew>>(`/crews/name/${name}`),
  create: (data: CrewCreateRequest) => api.post<ApiResponse<Crew>>('/crews', data),
  update: (id: number, data: CrewCreateRequest) => api.put<ApiResponse<Crew>>(`/crews/${id}`, data),
  delete: (id: number) => api.delete<ApiResponse<void>>(`/crews/${id}`)
}

export const bindingApi = {
  getAll: () => api.get<ApiResponse<BindingDetail[]>>('/bindings'),
  getById: (id: number) => api.get<ApiResponse<BindingDetail>>(`/bindings/${id}`),
  getByPropId: (propId: number) => api.get<ApiResponse<BindingDetail[]>>(`/bindings/prop/${propId}`),
  getByCrewId: (crewId: number) => api.get<ApiResponse<BindingDetail[]>>(`/bindings/crew/${crewId}`),
  getByCrewName: (crewName: string) => api.get<ApiResponse<BindingDetail[]>>(`/bindings/crew/name/${crewName}`),
  getByDateRange: (startDate: string, endDate: string) => 
    api.get<ApiResponse<BindingDetail[]>>(`/bindings/date-range`, {
      params: { startDate, endDate }
    }),
  checkConflict: (propId: number, startDate: string, endDate: string, excludeBindingId?: number) =>
    api.get<ApiResponse<ConflictCheckResponse>>(`/bindings/conflict-check`, {
      params: { propId, startDate, endDate, excludeBindingId }
    }),
  create: (data: BindingCreateRequest) => api.post<ApiResponse<BindingDetail>>('/bindings', data),
  update: (data: BindingUpdateRequest) => api.put<ApiResponse<BindingDetail>>('/bindings', data),
  cancel: (id: number, reason?: string, operator?: string) =>
    api.put<ApiResponse<void>>(`/bindings/${id}/cancel`, {
      params: { reason, operator }
    })
}

export const changeLogApi = {
  getAll: () => api.get<ApiResponse<ScheduleChangeLog[]>>('/bindings/logs'),
  getByBindingId: (bindingId: number) => api.get<ApiResponse<ScheduleChangeLog[]>>(`/bindings/logs/binding/${bindingId}`),
  getConflicts: () => api.get<ApiResponse<ScheduleChangeLog[]>>('/bindings/logs/conflicts')
}
