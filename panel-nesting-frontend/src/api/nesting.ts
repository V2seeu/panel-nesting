import axios from 'axios'
import type { NestingRequest, NestingResult } from '../types'

export async function computeNesting(request: NestingRequest): Promise<NestingResult> {
  const response = await axios.post<NestingResult>('/api/nesting/compute', request)
  return response.data
}
