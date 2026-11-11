<template>
  <div class="form-card">
    <div class="card-hd" v-if="step || title || $slots['title-extra']">
      <span v-if="step" class="step-num">{{ step }}</span>
      <span class="card-title">{{ title }}</span>
      <slot name="title-extra"></slot>
    </div>
    <slot></slot>
  </div>
</template>

<script setup>
defineProps({ step: [String, Number], title: String })
</script>

<style scoped>
.form-card {
 background: rgba(255,255,255,0.9);
  border: 1px solid var(--bd); border-radius: 16px; padding: 24px;
  margin-bottom: 20px;
}
.card-hd {
  font-size: 15px; font-weight: 700; margin-bottom: 18px;
  display: flex; align-items: center; gap: 10px;
}
.step-num {
  width: 24px; height: 24px; border-radius: 50%;
  background: var(--grad); color: #fff; font-size: 12px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
@media (max-width: 768px) {
  .form-card { padding: 18px; }
}
</style>
