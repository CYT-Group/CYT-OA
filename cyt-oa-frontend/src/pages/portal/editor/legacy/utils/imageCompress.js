// 图片上传前的客户端自动压缩（ImageUpload.vue 与 RichTextEditor.vue 共用）
//
// 规则：长边缩到 maxEdge，重新编码。GIF（动图）不处理；PNG 保持 PNG（截图文字不糊），
// 过大时尝试 WebP；照片类走 JPEG/WebP 0.85。
// 注意：输出文件扩展名必须与实际编码一致（后端做 magic bytes 校验），
// 且 canvas.toBlob 对不支持的格式会静默回退 PNG，所以要用 blob.type 反查真实编码。

const SKIP_COMPRESS_UNDER = 300 * 1024
const EXT_BY_TYPE = { 'image/jpeg': '.jpg', 'image/png': '.png', 'image/webp': '.webp' }

function toBlob(canvas, type, quality) {
  return new Promise(resolve => canvas.toBlob(resolve, type, quality))
}

/**
 * @param {File} file 原始图片文件
 * @param {number} maxEdge 长边上限 px（详情页正文最宽 ~776px，1600 = 2x retina 足够）
 * @returns {Promise<File>} 压缩后的文件；无收益/不支持的类型返回原文件
 */
export async function compressImage(file, maxEdge = 1600) {
  if (!/^image\/(jpeg|png|webp)$/.test(file.type)) return file
  let bmp
  try {
    // imageOrientation: 按 EXIF 旋转烘焙进像素，避免手机照片方向错乱
    bmp = await createImageBitmap(file, { imageOrientation: 'from-image' })
  } catch {
    return file // 解码失败（损坏/超大），交给后端原样处理
  }
  const scale = Math.min(1, maxEdge / Math.max(bmp.width, bmp.height))
  if (scale === 1 && file.size <= SKIP_COMPRESS_UNDER) { bmp.close?.(); return file }

  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(bmp.width * scale))
  canvas.height = Math.max(1, Math.round(bmp.height * scale))
  canvas.getContext('2d').drawImage(bmp, 0, 0, canvas.width, canvas.height)
  bmp.close?.()

  const isPng = file.type === 'image/png'
  let wantType = isPng ? 'image/png' : file.type === 'image/webp' ? 'image/webp' : 'image/jpeg'
  let blob = await toBlob(canvas, wantType, isPng ? undefined : 0.85)
  if (blob && blob.type !== wantType) blob = null // 浏览器不支持该编码，静默回退了 → 弃用
  // PNG 缩放后仍很大 → 尝试 WebP（对截图文字的伤害比 JPEG 小）
  if (isPng && blob && blob.size > 1.5 * 1024 * 1024) {
    const webp = await toBlob(canvas, 'image/webp', 0.9)
    if (webp && webp.type === 'image/webp' && webp.size < blob.size) blob = webp
  }
  if (!blob || blob.size >= file.size) return file // 压缩没有收益就传原图

  const ext = EXT_BY_TYPE[blob.type]
  const base = (file.name || 'image').replace(/\.[^.]+$/, '')
  return new File([blob], base + ext, { type: blob.type })
}
