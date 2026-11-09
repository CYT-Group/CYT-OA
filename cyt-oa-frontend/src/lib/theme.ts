import { computed, ref, watch } from 'vue'

export type ColorMode = 'light' | 'dark'

const STORAGE_KEY = 'cyt-oa-theme'

function preferred(): ColorMode {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'light' || saved === 'dark') return saved
  return 'light'
}

export const colorMode = ref<ColorMode>(preferred())

function apply(mode: ColorMode) {
  document.documentElement.classList.toggle('dark', mode === 'dark')
  document.documentElement.style.colorScheme = mode
}

apply(colorMode.value)

watch(colorMode, (mode) => {
  apply(mode)
  localStorage.setItem(STORAGE_KEY, mode)
})

export const isDark = computed(() => colorMode.value === 'dark')

export function toggleTheme() {
  colorMode.value = colorMode.value === 'dark' ? 'light' : 'dark'
}
