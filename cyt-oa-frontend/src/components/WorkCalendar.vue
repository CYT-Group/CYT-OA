<template>
  <section class="panel overflow-hidden" aria-label="工作日历">
    <div class="panel-header">
      <h2 class="panel-title"><CalendarDays class="size-4 text-primary" />工作日历</h2>
      <button type="button" class="rounded px-2 py-1 text-xs text-primary hover:bg-primary/8" @click="reset">
        今天
      </button>
    </div>
    <div class="p-5">
      <div class="mb-4 flex items-center justify-between">
        <p class="text-sm font-semibold tabular-nums">{{ year }} 年 {{ month + 1 }} 月</p>
        <div class="flex gap-1">
          <Button variant="ghost" size="icon-sm" aria-label="上个月" @click="move(-1)"
            ><ChevronLeft
          /></Button>
          <Button variant="ghost" size="icon-sm" aria-label="下个月" @click="move(1)"
            ><ChevronRight
          /></Button>
        </div>
      </div>
      <div class="calendar-grid">
        <span v-for="day in ['一', '二', '三', '四', '五', '六', '日']" :key="day" class="weekday">{{
          day
        }}</span>
        <span v-for="n in offset" :key="'blank-' + n" aria-hidden="true" />
        <span
          v-for="day in days"
          :key="day"
          :class="isToday(day) ? 'today' : (offset + day - 1) % 7 > 4 ? 'text-muted-foreground' : ''"
          :aria-current="isToday(day) ? 'date' : undefined"
          >{{ day }}</span
        >
      </div>
      <div v-if="$slots.default" class="mt-5 border-t border-border pt-4"><slot /></div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { CalendarDays, ChevronLeft, ChevronRight } from '@lucide/vue'
import Button from '../ui/Button.vue'

const today = new Date()
const cursor = ref(new Date(today.getFullYear(), today.getMonth(), 1))
const year = computed(() => cursor.value.getFullYear())
const month = computed(() => cursor.value.getMonth())
const offset = computed(() => (cursor.value.getDay() + 6) % 7)
const days = computed(() => new Date(year.value, month.value + 1, 0).getDate())
function isToday(day: number) {
  return day === today.getDate() && month.value === today.getMonth() && year.value === today.getFullYear()
}
function move(delta: number) {
  cursor.value = new Date(year.value, month.value + delta, 1)
}
function reset() {
  cursor.value = new Date(today.getFullYear(), today.getMonth(), 1)
}
</script>
