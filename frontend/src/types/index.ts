export interface Prop {
  id: number
  propCode: string
  propName: string
  sceneType: string
  material: string
  specification: string
  quantity: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface Crew {
  id: number
  crewName: string
  projectName: string
  director: string
  startDate: string
  endDate: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface BindingDetail {
  id: number
  propId: number
  propCode: string
  propName: string
  sceneType: string
  crewId: number
  crewName: string
  projectName: string
  startDate: string
  endDate: string
  status: string
  bindingType: string
  remark: string
  createdAt: string
  updatedAt: string
}

export interface ScheduleChangeLog {
  id: number
  bindingId: number
  propId: number
  crewId: number
  originalStartDate: string
  originalEndDate: string
  newStartDate: string
  newEndDate: string
  changeType: string
  changeReason: string
  operator: string
  conflictDetected: boolean
  conflictDescription: string
  createdAt: string
}

export interface ConflictCheckResponse {
  hasConflict: boolean
  conflictMessage: string
  conflictingBindingId: number
  conflictingCrewName: string
  conflictingStartDate: string
  conflictingEndDate: string
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PropCreateRequest {
  propCode: string
  propName: string
  sceneType: string
  material?: string
  specification?: string
  quantity: number
}

export interface CrewCreateRequest {
  crewName: string
  projectName?: string
  director?: string
  startDate?: string
  endDate?: string
}

export interface BindingCreateRequest {
  propId: number
  crewId: number
  startDate: string
  endDate: string
  bindingType?: string
  remark?: string
  operator?: string
}

export interface BindingUpdateRequest {
  id: number
  startDate?: string
  endDate?: string
  bindingType?: string
  remark?: string
  changeReason?: string
  operator?: string
}
