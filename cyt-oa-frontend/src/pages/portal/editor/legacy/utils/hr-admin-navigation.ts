/** Only reuse an actual list history entry, retaining its filters and paging. */
export function isHrDocumentListHistory(previous: unknown, origin: string): boolean {
  if (typeof previous !== 'string' || !previous) return false
  try {
    const url = new URL(previous, origin)
    return url.origin === origin && url.pathname === '/portal/hr-documents'
  } catch {
    return false
  }
}
