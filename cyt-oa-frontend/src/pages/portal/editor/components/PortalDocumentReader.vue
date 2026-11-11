<template>
  <div class="document-reader" :class="`reader-width-${width}`">
    <nav class="reader-toc" aria-label="文档目录">
      <div class="reader-toc-heading">
        <ListTree class="size-4" />
        <h3>目录</h3>
        <span v-if="items.length">{{ items.length }}</span>
      </div>
      <ul v-if="items.length" ref="tocList" class="reader-toc-list">
        <li v-for="(item, index) in items" :key="index">
          <button
            type="button"
            :data-outline-index="index"
            :class="[`level-${item.level}`, { active: active === index }]"
            :aria-current="active === index ? 'location' : undefined"
            @click="goTo(index)"
          >
            {{ item.label }}
          </button>
        </li>
      </ul>
      <p v-else class="reader-no-toc">正文未设置章节标题</p>
    </nav>
    <div
      ref="viewport"
      class="reader-scroll"
      :class="scrollClass"
      tabindex="0"
      aria-label="文档正文滚动区域"
      @scroll.passive="scheduleActive"
    >
      <article class="reader-paper" @click="onContentClick">
        <slot name="before" />
        <div ref="content" class="reader-content">
          <HrBlocksRenderer
            v-if="blocks?.length"
            :key="contentKey"
            :blocks="blocks"
            :theme="theme"
            :disable-navigation="disableNavigation"
          />
          <HrDocRenderer v-else-if="html" :html="html" />
          <EmptyState
            v-else
            title="正文为空"
            description="文档内容会显示在这里。"
            compact
          />
        </div>
        <slot name="after" />
      </article>
    </div>
  </div>
</template>
<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { ListTree } from "@lucide/vue";
import HrBlocksRenderer from "../legacy/components/HrBlocksRenderer.vue";
import HrDocRenderer from "../legacy/components/HrDocRenderer.vue";
import EmptyState from "../../../../ui/EmptyState.vue";

const props = withDefaults(
  defineProps<{
    blocks?: Record<string, any>[] | null;
    html?: string;
    theme?: string;
    contentKey?: string | number;
    width?: string;
    scrollClass?: string;
    disableNavigation?: boolean;
  }>(),
  {
    blocks: null,
    html: "",
    theme: "",
    contentKey: "",
    width: "desktop",
    scrollClass: "",
    disableNavigation: false,
  },
);
const tocList = ref<HTMLElement>(),
  viewport = ref<HTMLElement>(),
  content = ref<HTMLElement>();
const items = ref<{ element: HTMLElement; label: string; level: number }[]>([]);
const active = ref(0);
let observer: MutationObserver | undefined,
  resizeObserver: ResizeObserver | undefined,
  frame = 0,
  scanFrame = 0;
function updateActive() {
  frame = 0;
  const top = (viewport.value?.getBoundingClientRect().top || 0) + 36;
  let current = 0;
  for (let index = 0; index < items.value.length; index++) {
    if (items.value[index]!.element.getBoundingClientRect().top <= top)
      current = index;
    else break;
  }
  active.value = current;
}
function scheduleActive() {
  if (!frame) frame = requestAnimationFrame(updateActive);
}
watch(active, async (index) => {
  await nextTick();
  const list = tocList.value,
    entry = list?.querySelector<HTMLElement>(`[data-outline-index="${index}"]`);
  if (!list || !entry) return;
  const parent = list.getBoundingClientRect(),
    current = entry.getBoundingClientRect();
  if (current.top < parent.top || current.bottom > parent.bottom)
    list.scrollTop += current.top - parent.top - parent.height / 3;
});
function scan() {
  scanFrame = 0;
  items.value = Array.from(
    content.value?.querySelectorAll<HTMLElement>("h2, h3, h4") || [],
  )
    .filter(
      (element) =>
        element.textContent?.trim() &&
        !element.classList.contains("hb-it-title") &&
        element.getClientRects().length,
    )
    .map((element) => ({
      element,
      label: (
        element.querySelector(".hb-numsec-title, .hb-section-title")
          ?.textContent ||
        element.textContent ||
        ""
      ).trim(),
      level: element.classList.contains("hb-section")
        ? 2
        : element.tagName === "H4"
          ? 3
          : element.tagName === "H3"
            ? 2
            : 1,
    }));
  scheduleActive();
}
function scheduleScan() {
  if (!scanFrame) scanFrame = requestAnimationFrame(scan);
}
function goTo(index: number) {
  const target = items.value[index]?.element,
    scroll = viewport.value;
  if (!target || !scroll) return;
  active.value = index;
  scroll.scrollTo({
    top:
      scroll.scrollTop +
      target.getBoundingClientRect().top -
      scroll.getBoundingClientRect().top -
      24,
    behavior: "auto",
  });
}
function onContentClick(event: MouseEvent) {
  const link = (event.target as Element)?.closest<HTMLAnchorElement>(
    'a[href^="#"]',
  );
  if (!link || !content.value?.contains(link)) return;
  try {
    const id = decodeURIComponent(link.getAttribute("href")!.slice(1));
    const target = content.value.querySelector<HTMLElement>(
        `[id="${CSS.escape(id)}"]`,
      ),
      scroll = viewport.value;
    if (target && scroll) {
      event.preventDefault();
      scroll.scrollTo({
        top:
          scroll.scrollTop +
          target.getBoundingClientRect().top -
          scroll.getBoundingClientRect().top -
          24,
        behavior: "auto",
      });
    }
  } catch {
    /* 非法锚点保留浏览器默认行为。 */
  }
}
function scrollToTop() {
  viewport.value?.scrollTo({ top: 0, behavior: "auto" });
  active.value = 0;
}
watch(
  () => [props.blocks, props.html, props.contentKey],
  async () => {
    await nextTick();
    scrollToTop();
    scheduleScan();
  },
  { flush: "post" },
);
onMounted(async () => {
  await nextTick();
  observer = new MutationObserver(scheduleScan);
  if (content.value)
    observer.observe(content.value, {
      childList: true,
      subtree: true,
      characterData: true,
    });
  resizeObserver = new ResizeObserver(scheduleActive);
  if (viewport.value) resizeObserver.observe(viewport.value);
  if (content.value) resizeObserver.observe(content.value);
  scan();
});
onBeforeUnmount(() => {
  observer?.disconnect();
  resizeObserver?.disconnect();
  cancelAnimationFrame(frame);
  cancelAnimationFrame(scanFrame);
});
defineExpose({ scrollToTop });
</script>
<style scoped>
.document-reader {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  color: var(--foreground);
  font-family: var(--font-sans);
}
.reader-toc {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  border-right: 1px solid var(--border);
  background: var(--card);
}
.reader-toc-heading {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  padding: 18px 18px 12px;
  color: var(--muted-foreground);
  font-size: 12px;
}
.reader-toc-heading h3 {
  color: var(--foreground);
  font-weight: 550;
}
.reader-toc-heading span {
  margin-left: auto;
  font-variant-numeric: tabular-nums;
}
.document-reader .reader-toc .reader-toc-list {
  margin: 0;
  padding: 0 10px 18px;
  overflow-y: auto;
  overscroll-behavior: contain;
  list-style: none;
  scrollbar-width: thin;
}
.reader-toc-list button {
  display: block;
  width: 100%;
  padding: 8px 10px;
  border-left: 2px solid transparent;
  border-radius: 0 5px 5px 0;
  background: transparent;
  color: var(--muted-foreground);
  font-size: 12px;
  line-height: 1.65;
  text-align: left;
  overflow-wrap: anywhere;
  cursor: pointer;
}
.reader-toc-list button.level-2 {
  padding-left: 20px;
  font-size: 11px;
}
.reader-toc-list button.level-3 {
  padding-left: 30px;
  font-size: 11px;
}
.reader-toc-list button:hover {
  background: var(--muted);
  color: var(--foreground);
}
.reader-toc-list button.active {
  border-left-color: var(--primary);
  color: var(--primary);
  background: var(--info-soft);
  font-weight: 550;
}
.reader-toc-list button:focus-visible {
  outline: 2px solid var(--ring);
  outline-offset: -2px;
}
.reader-no-toc {
  padding: 0 18px;
  color: var(--muted-foreground);
  font-size: 11px;
  line-height: 1.8;
}
.reader-scroll {
  min-width: 0;
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  padding: 24px;
  background: var(--background);
  outline-offset: -3px;
  scrollbar-width: thin;
}
.reader-paper {
  --t: #1c2738;
  --t2: #4b5a70;
  --t3: #738095;
  --t4: #8491a6;
  --bd: #dfe5ee;
  --v: #6366f1;
  width: 100%;
  max-width: 880px;
  min-height: 100%;
  margin: 0 auto;
  padding: 32px 36px 48px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  color: #1c2738;
  container: hr-layout-preview / inline-size;
  overflow-wrap: anywhere;
}
.reader-paper :deep(ul) {
  list-style: disc;
}
.reader-paper :deep(ol) {
  list-style: decimal;
}
.reader-content {
  min-width: 0;
}
.reader-width-mobile .reader-paper {
  max-width: 390px;
  padding: 24px 20px 36px;
}
@media (max-width: 1100px) {
  .document-reader {
    grid-template-columns: 172px minmax(0, 1fr);
  }
  .reader-scroll {
    padding: 18px;
  }
  .reader-paper {
    padding: 26px;
  }
}
@media (max-width: 767px) {
  .document-reader {
    display: flex;
    flex-direction: column;
  }
  .reader-toc {
    flex: 0 0 auto;
    max-height: 154px;
    border-right: 0;
    border-bottom: 1px solid var(--border);
  }
  .reader-toc-heading {
    padding: 10px 16px 6px;
  }
  .reader-toc-list {
    padding: 0 8px 8px;
  }
  .reader-toc-list button {
    padding-top: 5px;
    padding-bottom: 5px;
  }
  .reader-no-toc {
    padding: 0 16px 10px;
  }
  .reader-scroll {
    flex: 1;
    padding: 12px;
  }
  .reader-paper,
  .reader-width-mobile .reader-paper {
    padding: 22px 18px 32px;
  }
}
@media print {
  .document-reader {
    display: block;
    height: auto;
    overflow: visible;
  }
  .reader-toc {
    display: block;
    max-height: none;
    border: 0;
    background: #fff;
    margin-bottom: 24px;
  }
  .document-reader .reader-toc .reader-toc-list {
    max-height: none;
    overflow: visible;
    columns: 2;
  }
  .reader-toc-list button {
    break-inside: avoid;
    background: none;
    border-color: transparent;
    color: #475569;
  }
  .reader-scroll {
    height: auto;
    overflow: visible;
    padding: 0;
    background: #fff;
  }
  .reader-paper {
    max-width: none;
    min-height: 0;
    padding: 0;
    border: 0;
    container-type: normal;
  }
}
</style>
