<template>
  <div class="img-up" :class="{ small, 'as-tile': asTile }">
    <!-- 已上传：显示预览 + 删除/替换 -->
    <div v-if="modelValue" class="preview" :style="`background-image: url(${modelValue})`">
      <div class="preview-actions">
        <button class="act" @click="trigger" title="替换">↻</button>
        <button class="act danger" @click="clear" title="删除">×</button>
      </div>
    </div>

    <!-- 未上传：显示上传按钮 / 占位 -->
    <button v-else type="button" class="upload-btn" @click="trigger" :disabled="uploading">
      <div class="up-icon">{{ uploading ? '⏳' : '📁' }}</div>
      <div class="up-text">{{ uploading ? (processing ? '压缩中...' : '上传中...') : (asTile ? '+ 添加' : '点击上传图片') }}</div>
      <div v-if="hint && !asTile" class="up-hint">{{ hint }}</div>
    </button>

    <input ref="fileInput" type="file" accept="image/*" hidden @change="onFile">
    <div v-if="error" class="err-msg">{{ error }}</div>

    <!-- 上传/校验失败一律弹模态框：此前只有 11px 的红色小字，很容易被忽略 -->
    <ConfirmDialog
      v-model:open="dialogOpen"
      tone="danger"
      title="图片上传失败"
      :message="dialogMsg"
      confirm-text="知道了"
      hide-cancel
    />

    <!-- 尺寸/比例不符合建议 → 弹框确认（不是硬拦截）：用户点「仍然上传」才继续 -->
    <ConfirmDialog
      v-model:open="sizeOpen"
      tone="warn"
      title="图片尺寸不太合适"
      :message="sizeMsg"
      confirm-text="仍然上传"
      cancel-text="换一张"
      @confirm="confirmSize"
      @cancel="cancelSize"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { uploadApi } from '../../../bridge/api'
import { compressImage } from '../../utils/imageCompress'
import ConfirmDialog from '../ui/ConfirmDialog.vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  folder: { type: String, default: 'news' },
  hint: { type: String, default: '' },
  small: Boolean,
  asTile: Boolean,  // 用于图集（方形磁贴）
  // 自动压缩的长边上限（px）。详情页正文最宽 ~776px，1600 = 2x retina 足够
  maxEdge: { type: Number, default: 1600 },
  // ── 尺寸校验（默认全关；只有明确写了尺寸要求的场景才打开）──
  // 期望宽高比，如 16/9；0 = 不校验
  expectRatio: { type: Number, default: 0 },
  // 比例容差（相对偏差）：默认 12%，即 16:9 接受 1.56~1.99
  ratioTolerance: { type: Number, default: 0.12 },
  // 最小宽度（px）；0 = 不校验。整屏封面用 1200 —— 再小铺满屏幕会发虚
  minWidth: { type: Number, default: 0 },
  // 建议尺寸文案，只用于弹框提示
  recommend: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

const fileInput = ref(null)
const uploading = ref(false)
const processing = ref(false)
const error = ref('')
// 失败既落内联红字（关闭弹框后仍知道哪块出过错），也弹模态框（保证不会错过）
const dialogOpen = ref(false)
const dialogMsg = ref('')
// 尺寸不合适：弹框确认，确认后继续上传
const sizeOpen = ref(false)
const sizeMsg = ref('')
let pendingFile = null

function fail(msg) {
  error.value = msg
  dialogMsg.value = msg
  dialogOpen.value = true
}

function trigger() {
  fileInput.value?.click()
}

/** 常见比例的显示名，用于弹框文案 */
const RATIO_LABELS = [
  [16 / 9, '16:9'], [4 / 3, '4:3'], [3 / 2, '3:2'], [1, '1:1'], [21 / 9, '21:9'], [2 / 1, '2:1'],
]
function ratioLabel(r) {
  const hit = RATIO_LABELS.find(([v]) => Math.abs(v - r) < 0.02)
  return hit ? hit[1] : `${r.toFixed(2)}:1`
}

/**
 * 读原图尺寸。走 createImageBitmap 并烘焙 EXIF 方向 —— 与 compressImage 同一口径，
 * 否则手机竖拍照片（EXIF 旋转）会算出横竖颠倒的比例。
 */
async function readImageSize(file) {
  try {
    const bmp = await createImageBitmap(file, { imageOrientation: 'from-image' })
    const size = { w: bmp.width, h: bmp.height }
    bmp.close?.()
    return size
  } catch { /* 解码不了 → 回落 <img> */ }
  return new Promise((resolve) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => { resolve({ w: img.naturalWidth, h: img.naturalHeight }); URL.revokeObjectURL(url) }
    img.onerror = () => { resolve(null); URL.revokeObjectURL(url) }
    img.src = url
  })
}

/** 尺寸不合适就返回提示文案（空串 = 没问题，直接传） */
async function sizeWarning(file) {
  if (!props.expectRatio && !props.minWidth) return ''
  const dim = await readImageSize(file)
  if (!dim || !dim.w || !dim.h) return ''
  const { w, h } = dim
  const problems = []
  if (props.minWidth && w < props.minWidth) {
    problems.push(`宽度只有 ${w}px（建议 ≥${props.minWidth}px，铺满屏幕会发虚）`)
  }
  if (props.expectRatio) {
    const actual = w / h
    if (Math.abs(actual - props.expectRatio) / props.expectRatio > props.ratioTolerance) {
      problems.push(`比例约 ${actual.toFixed(2)}:1，与建议的 ${ratioLabel(props.expectRatio)} 不符，裁切会切掉较多画面`)
    }
  }
  if (!problems.length) return ''
  const tail = props.recommend ? `建议尺寸：${props.recommend}。` : ''
  return `当前图片 ${w}×${h}：${problems.join('；')}。${tail}仍要使用这张图片吗？`
}

function confirmSize() {
  sizeOpen.value = false
  const f = pendingFile
  pendingFile = null
  if (f) doUpload(f)
}
function cancelSize() {
  sizeOpen.value = false
  pendingFile = null
}

async function onFile(e) {
  const file = e.target.files?.[0]
  e.target.value = ''  // 立刻清空：同一张图二次选择也要能触发 change
  if (!file) return
  error.value = ''
  if (file.size > 25 * 1024 * 1024) {
    fail('图片超过 25MB，请先手动压缩')
    return
  }
  // 尺寸不合适 → 先弹框，用户确认后才继续
  const warn = await sizeWarning(file)
  if (warn) {
    pendingFile = file
    sizeMsg.value = warn
    sizeOpen.value = true
    return
  }
  await doUpload(file)
}

async function doUpload(file) {
  uploading.value = true
  processing.value = true
  error.value = ''
  try {
    file = await compressImage(file, props.maxEdge)
    processing.value = false
    if (file.size > 10 * 1024 * 1024) {
      fail('压缩后仍超过 10MB，请换一张或手动压缩')
      return
    }
    const res = await uploadApi.uploadFile(file, props.folder)
    if (res?.data?.url) {
      emit('update:modelValue', res.data.url)
    } else {
      fail('上传失败')
    }
  } catch (err) {
    const msg = err.message || ''
    if (/timeout|ECONNABORTED/i.test(msg)) {
      fail('上传超时，请检查网络后重试（图片越大耗时越久，建议先压缩到 2MB 内）')
    } else {
      fail('上传失败：' + (msg || '未知错误'))
    }
  } finally {
    uploading.value = false
    processing.value = false
  }
}

function clear() {
  emit('update:modelValue', '')
}
</script>

<style scoped>
.img-up { width: 100%; }
.img-up.as-tile { aspect-ratio: 1; }

.preview {
  width: 100%; aspect-ratio: 16/9; background-size: cover; background-position: center;
  border-radius: 10px; border: 1px solid var(--bd); position: relative;
  background-color: #f0f2f8;
}
.img-up.small .preview { aspect-ratio: 16/9; max-height: 140px; }
.img-up.as-tile .preview { aspect-ratio: 1; }
.preview-actions { position: absolute; top: 8px; right: 8px; display: flex; gap: 6px; }
.act {
  width: 28px; height: 28px; border-radius: 50%; border: none; cursor: pointer;
  background: rgba(0,0,0,.6); color: #fff; font-size: 14px;
  display: flex; align-items: center; justify-content: center;
}
.act.danger:hover { background: #DC2626; }

.upload-btn {
  width: 100%; padding: 28px 16px; border: 2px dashed var(--bd); border-radius: 10px;
  background: rgba(255,255,255,0.5); cursor: pointer; transition: .15s;
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  font-family: var(--fn);
}
.img-up.small .upload-btn { padding: 16px 12px; }
.img-up.as-tile .upload-btn { aspect-ratio: 1; padding: 8px; }
.upload-btn:hover:not(:disabled) { border-color: var(--v); background: rgba(99,102,241,0.04); }
.upload-btn:disabled { opacity: .6; cursor: default; }
.up-icon { font-size: 24px; }
.img-up.small .up-icon { font-size: 18px; }
.up-text { font-size: 12px; color: var(--t2); font-weight: 600; }
.up-hint { font-size: 10px; color: var(--t4); margin-top: 2px; }
.err-msg { font-size: 11px; color: #DC2626; margin-top: 6px; }
</style>
