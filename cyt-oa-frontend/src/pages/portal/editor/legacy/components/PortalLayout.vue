<template>
  <div class="portal-layout">
    <!-- Animated background -->
    <div class="bg-canvas">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>
    <div class="aurora">
      <div class="aurora-beam b1"></div>
      <div class="aurora-beam b2"></div>
      <div class="aurora-beam b3"></div>
    </div>
    <div class="light-streak"></div>
    <div class="grid-pattern"></div>
    <div class="particles">
      <div class="particle" v-for="n in 8" :key="n"></div>
    </div>

    <!-- Live clock -->
    <div class="live-clock">
      <span class="dot"></span>
      <span>{{ clockText }}</span>
    </div>

    <!-- Page content slot -->
    <main>
      <slot></slot>
    </main>

    <!-- Footer -->
    <footer>
      <div class="footer-inner">
        <span class="footer-copy">&copy; 2026 Changyuntech.net · CYT Group</span>
        <ul class="footer-links">
          <li><router-link to="/">首页</router-link></li>
          <li><router-link to="/brands">品牌</router-link></li>
          <li><router-link to="/culture">企业文化</router-link></li>
          <li><router-link to="/about">关于</router-link></li>
        </ul>
        <div class="footer-copy">portal.cyt.group</div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const clockText = ref('SHENZHEN · 00:00:00')
let clockTimer = null

function updateClock() {
  const d = new Date()
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  clockText.value = `SHENZHEN · ${h}:${m}:${s}`
}

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>
