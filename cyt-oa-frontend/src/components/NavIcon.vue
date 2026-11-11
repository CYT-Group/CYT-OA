<template>
  <span v-if="emoji" class="nav-emoji" aria-hidden="true">{{ emoji }}</span>
  <component
    v-else
    :is="iconOf(name || 'file')"
    class="nav-item-icon"
    :stroke-width="1.8"
    aria-hidden="true"
  />
</template>

<script setup lang="ts">
import { computed } from "vue";
import { iconAlias, iconOf } from "../lib/icons";

const props = defineProps<{ name?: string; glyph?: string }>();
const glyphs: Record<string, string> = {
  home: "🏠",
  target: "🎪",
  bell: "🔔",
  "trending-up": "📊",
  clipboard: "📝",
  user: "👤",
  users: "👥",
  megaphone: "📰",
  stamp: "📜",
  box: "📦",
  truck: "🚚",
  book: "📑",
  folder: "📂",
  settings: "⚙️",
  shield: "🔐",
  calendar: "📅",
  "file-text": "📄",
  file: "📄",
  building: "🏢",
  package: "📦",
  key: "🔐",
  chart: "📊",
  briefcase: "👥",
  gift: "🎁",
  heart: "💜",
  plane: "✈️",
  receipt: "💰",
  clock: "⏰",
  "calendar-plus": "📅",
  message: "🗣",
  lightbulb: "💡",
  mail: "✉️",
  award: "🏅",
  pin: "📌",
  sparkles: "✨",
  upload: "📤",
};
const emoji = computed(
  () =>
    props.glyph ??
    glyphs[iconAlias[props.name ?? "file"] ?? props.name ?? "file"],
);
</script>
