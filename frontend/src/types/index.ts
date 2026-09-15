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
  genre: string
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
  crewGenre: string
  projectName: string
  startDate: string
  endDate: string
  status: string
  bindingType: string
  remark: string
  version: number
  createdAt: string
  updatedAt: string
}

export interface ExpiringBinding {
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
  remainingDays: number
  status: string
  bindingType: string
  remark: string
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

export interface BarcodeCheckResponse {
  valid: boolean
  duplicate: boolean
  message: string
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
  genre: string
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
  version: number
  startDate?: string
  endDate?: string
  bindingType?: string
  remark?: string
  changeReason?: string
  operator?: string
}

export interface ImportBatch {
  id: number
  fileName: string
  operator: string
  totalRows: number
  passedRows: number
  failedRows: number
  writtenRows: number
  notWrittenRows: number
  status: string
  createdAt: string
  committedAt: string
}

export interface ImportRow {
  id: number
  batchId: number
  rowNo: number
  propCode: string
  crewName: string
  startDateRaw: string
  endDateRaw: string
  remark: string
  validateStatus: string
  validateMessage: string
  writeStatus: string
  writeMessage: string
  bindingId: number | null
}

export interface ImportBatchResult {
  batch: ImportBatch
  rows: ImportRow[]
}
