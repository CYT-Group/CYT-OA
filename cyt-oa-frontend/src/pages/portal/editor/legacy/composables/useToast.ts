import { ref } from 'vue'

export type ToastType = 'ok' | 'err'

const message = ref('')
const visible = ref(false)
const type = ref<ToastType>('ok')

// 停留时长：成功一闪而过即可；错误要留足「读完 + 复制」的时间
const OK_MS = 3000
const ERR_MS = 8000

let timer: ReturnType<typeof setTimeout> | undefined
let remaining = 0
let startedAt = 0

function clearTimer() {
  if (timer !== undefined) clearTimeout(timer)
  timer = undefined
}

function schedule(ms: number) {
  clearTimer()
  remaining = ms
  startedAt = Date.now()
  timer = setTimeout(() => { visible.value = false; timer = undefined }, ms)
}

export function useToast() {
  function show(msg: string, t: ToastType = 'ok') {
    message.value = msg
    type.value = t
    visible.value = true
    schedule(t === 'err' ? ERR_MS : OK_MS)
  }

  // 鼠标移到提示上 → 暂停倒计时（够时间读完 / 点复制）；移开继续。
  // 错误信息常常是要贴给别人的（比如后端 422 的字段路径），不能让它 3 秒就没了。
  function pause() {
    if (!visible.value || timer === undefined) return
    clearTimer()
    remaining -= Date.now() - startedAt
  }

  function resume() {
    if (!visible.value || timer !== undefined) return
    schedule(Math.max(remaining, 1500))
  }

  function hide() {
    clearTimer()
    visible.value = false
  }

  return { message, visible, type, show, pause, resume, hide }
}
