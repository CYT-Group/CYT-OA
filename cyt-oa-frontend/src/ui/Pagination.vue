<template>
  <nav
    v-if="total > 0"
    aria-label="分页"
    :class="
      cn(
        'flex min-w-0 flex-wrap items-center justify-between gap-2',
        props.class
      )
    "
  >
    <p
      class="shrink-0 whitespace-nowrap text-xs text-muted-foreground"
      aria-live="polite"
    >
      共
      <span class="font-medium tabular-nums text-foreground">{{ total }}</span>
      条
      <span v-if="pageCount > 1 && !compact"
        >· 第 {{ page }} / {{ pageCount }} 页</span
      >
    </p>
    <div
      v-if="pageCount > 1"
      class="flex max-w-full flex-wrap items-center justify-center gap-0.5"
    >
      <Button
        variant="ghost"
        size="icon-sm"
        :disabled="disabled || page <= 1"
        aria-label="上一页"
        @click="go(page - 1)"
      >
        <ChevronLeft />
      </Button>
      <span
        v-if="compact"
        class="min-w-12 whitespace-nowrap text-center text-xs tabular-nums"
        :aria-label="`第 ${page} 页，共 ${pageCount} 页`"
        >{{ page }} / {{ pageCount }}</span
      >
      <template v-for="(item, index) in compact ? [] : pages" :key="index">
        <span
          v-if="item === '…'"
          class="w-6 text-center text-xs text-muted-foreground"
          >…</span
        >
        <Button
          v-else
          :variant="item === page ? 'secondary' : 'ghost'"
          size="icon-sm"
          :class="
            item === page
              ? 'font-semibold tabular-nums text-foreground ring-1 ring-border'
              : 'tabular-nums text-muted-foreground'
          "
          :aria-current="item === page ? 'page' : undefined"
          :aria-label="`第 ${item} 页`"
          :disabled="disabled"
          @click="go(Number(item))"
        >
          {{ item }}
        </Button>
      </template>
      <Button
        variant="ghost"
        size="icon-sm"
        :disabled="disabled || page >= pageCount"
        aria-label="下一页"
        @click="go(page + 1)"
      >
        <ChevronRight />
      </Button>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ChevronLeft, ChevronRight } from '@lucide/vue';
import { cn } from '../lib/utils';
import Button from './Button.vue';

const props = withDefaults(
  defineProps<{
    page: number;
    pageSize: number;
    total: number;
    compact?: boolean;
    disabled?: boolean;
    class?: string;
  }>(),
  { pageSize: 10, compact: false, disabled: false }
);

const emit = defineEmits<{ 'update:page': [value: number] }>();

const pageCount = computed(() =>
  Math.max(1, Math.ceil(props.total / Math.max(1, props.pageSize)))
);

const pages = computed<(number | '…')[]>(() => {
  const count = pageCount.value;
  const cur = props.page;
  if (count <= 7) return Array.from({ length: count }, (_, i) => i + 1);
  if (cur <= 4) return [1, 2, 3, 4, 5, '…', count];
  if (cur >= count - 3)
    return [1, '…', count - 4, count - 3, count - 2, count - 1, count];
  return [1, '…', cur - 1, cur, cur + 1, '…', count];
});

function go(next: number) {
  const target = Math.min(pageCount.value, Math.max(1, next));
  if (target !== props.page) emit('update:page', target);
}
</script>
