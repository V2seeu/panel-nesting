export interface AvoidZone {
  cx: number
  cy: number
  radius: number
}

export interface Sheet {
  width: number
  height: number
  margin: number
  gap: number
  avoidZones: AvoidZone[]
}

export interface Part {
  id: string
  name: string
  width: number
  height: number
  quantity: number
  rotatable: boolean
}

export interface Placement {
  partId: string
  partName: string
  sheetIndex: number
  x: number
  y: number
  rotated: boolean
  width: number
  height: number
}

export interface SheetResult {
  index: number
  placements: Placement[]
}

export interface NestingResult {
  totalSheets: number
  utilization: number
  sheets: SheetResult[]
}

export interface NestingRequest {
  sheet: Sheet
  parts: Part[]
}
