// Core API types

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data?: T
  errors?: Record<string, string>
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export type RoleCode =
  | 'SYSTEM_ADMIN'
  | 'FOUNDER'
  | 'DIRECTOR'
  | 'METHODIST'
  | 'EDUCATOR'
  | 'KAZ_TEACHER'
  | 'MUSIC_TEACHER'
  | 'PE_INSTRUCTOR'
  | 'NURSE'
  | 'JANITOR'
  | 'ACCOUNTANT'
  | 'PARENT'

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userId: string
  roleCode: RoleCode
  organizationId: string
  fullName: string
}

export interface Organization {
  id: string
  name: string
  legalName?: string
  bin?: string
  address?: string
  phone?: string
  email?: string
  logoUrl?: string
  active: boolean
  createdAt: string
}

export interface Branch {
  id: string
  organizationId: string
  name: string
  address?: string
  phone?: string
  headName?: string
  designCapacity?: number
  active: boolean
  createdAt: string
}

export interface User {
  id: string
  organizationId: string
  email: string
  phone?: string
  firstName: string
  lastName: string
  middleName?: string
  roleCode: RoleCode
  photoUrl?: string
  active: boolean
  preferredLanguage: 'ru' | 'kk'
  lastLoginAt?: string
  createdAt: string
  fullName?: string
}

export interface Group {
  id: string
  organizationId: string
  branchId: string
  name: string
  ageFromMonths?: number
  ageToMonths?: number
  language: 'ru' | 'kk'
  groupType?: string
  educatorId?: string
  educatorPhone?: string
  educatorEmail?: string
  educatorInfo?: string
  academicYear?: string
  active: boolean
}

export interface Child {
  id: string
  organizationId: string
  branchId: string
  groupId?: string
  lastName: string
  firstName: string
  middleName?: string
  birthDate: string
  gender?: 'male' | 'female'
  iin?: string
  photoUrl?: string
  admissionDate?: string
  admissionOrderNum?: string
  status: 'active' | 'transferred' | 'discharged' | 'graduated'
  notes?: string
}

export type MarkSymbol = '1' | 'б' | 'о'

export interface ChildAttendanceRow {
  childId: string
  fullName: string
  marks: Record<number, string> // day -> mark symbol
}

export interface AttendanceSheet {
  monthId: string
  groupId: string
  year: number
  month: number
  closed: boolean
  rows: ChildAttendanceRow[]
}

// ── Cyclogram ─────────────────────────────────────────────────────────────────

export type DayOfWeek = 'monday' | 'tuesday' | 'wednesday' | 'thursday' | 'friday'

export interface CyclogramSlot {
  time: string      // "08:00-08:30"
  activity: string
}

export type CyclogramContent = Partial<Record<DayOfWeek, CyclogramSlot[]>>

export interface Cyclogram {
  id: string
  organizationId: string
  branchId: string
  groupId: string
  academicYear: string
  month: number
  week: number
  language: 'ru' | 'kk'
  content: string | null  // JSONB as JSON string
  status: 'draft' | 'approved'
  generatedByAi: boolean
  createdAt: string
  updatedAt: string
}

// ── DailyInfo ─────────────────────────────────────────────────────────────────

export interface DailyPost {
  id: string
  organizationId: string
  branchId: string
  groupId: string
  postDate: string        // ISO date: "2026-08-13"
  theme: string | null
  description: string | null
  homeTasks: string | null
  published: boolean
  publishedAt: string | null
  createdAt: string
  updatedAt: string
}

// ── Plan (Перспективный план) ─────────────────────────────────────────────────

export type PlanStatus = 'draft' | 'review' | 'approved' | 'returned'

export interface ProspectivePlan {
  id: string
  organizationId: string
  branchId: string
  groupId: string
  academicYear: string
  month: number
  week: number
  theme: string | null
  language: 'ru' | 'kk'
  overallStatus: PlanStatus
  fillPct: number
}

export interface PlanSection {
  id: string
  planId: string
  domain: string
  domainNameRu: string | null
  domainNameKk: string | null
  ownerRole: string | null
  ownerUserId: string | null
  content: string | null
  objectives: string | null
  materials: string | null
  status: PlanStatus
  version: number
  sortOrder: number
  returnComment: string | null
}

// ── Observation (Наблюдение) ──────────────────────────────────────────────────

export interface ObservationIndicator {
  id: string
  domain: string
  criterion: string
  indicator: string
  ageGroup: string
}

export interface ObservationResultEntry {
  indicatorId: string
  level: string   // 'H' | 'S' | 'V' (низкий / средний / высокий)
}

export interface ObservationData {
  id: string
  childId: string
  period: string       // e.g. "start" | "middle" | "end"
  academicYear: string
  complete: boolean
  indicators: ObservationIndicator[]
  results: ObservationResultEntry[]
}

export interface IndividualCard {
  id?: string
  childId: string
  observationId: string
  gameName?: string
  gameObjectives?: string
  gameProcedure?: string
  customNotes?: string
}

// ── Schedule (Расписание) ─────────────────────────────────────────────────────

export interface ScheduleEntry {
  id: string
  dayOfWeek: number   // 1=Пн..5=Пт
  startTime: string   // "HH:mm"
  endTime: string | null
  subject: string
  educatorId: string | null
  educatorRole: string | null
  notes: string | null
}

export interface ScheduleData {
  id: string
  organizationId: string
  branchId: string
  groupId: string
  academicYear: string
  language: 'ru' | 'kk'
  approvalInfo: string | null
  published: boolean
  entries: ScheduleEntry[]
}
