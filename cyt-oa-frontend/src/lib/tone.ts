import type { Tone } from '../configs/types'

export type BadgeTone = 'default' | 'secondary' | 'outline' | 'success' | 'warning' | 'info' | 'destructive'

export function toneBadge(tone: Tone): BadgeTone {
  return ({ ok: 'success', warn: 'warning', bad: 'destructive', info: 'info' } as const)[tone]
}
