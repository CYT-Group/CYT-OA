/** 富文本图片先本地预览，所有保存入口统一等待正式图片地址，避免 blob URL 进入数据库。 */
const flushers = new Set<() => Promise<void>>();
export function registerPortalRichText(flush: () => Promise<void>): () => void {
  flushers.add(flush);
  return () => {
    flushers.delete(flush);
  };
}
export async function flushPortalRichTextImages(): Promise<void> {
  for (const flush of [...flushers]) await flush();
}
