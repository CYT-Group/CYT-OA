import { defineConfig, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

// 开发代理目标：默认指向本机 48081（稳定实例）；联调新代码时用 VITE_PROXY_TARGET 覆盖
const proxyTarget = process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:48081'

// 同事从局域网 IP 打开时，浏览器带的 Origin 是 http://192.168.1.x:4810，代理原样转给后端
// 会被 application-local.yml 的 CORS 白名单拒成 403「Invalid CORS request」，前端只显示成「没有该操作权限」。
// 页面和接口其实同源（都走 4810），CORS 本不该参与，所以把 Origin 改回本机地址，而不是给后端白名单加 IP。
const devOrigin = 'http://127.0.0.1:4810'

const proxy: ProxyOptions = {
  target: proxyTarget,
  changeOrigin: true,
  configure(instance) {
    instance.on('proxyReq', (req) => req.setHeader('origin', devOrigin))
  }
}

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  server: {
    // 绑 0.0.0.0：局域网里同事可用本机 IP 打开，启动日志也会出现 Network 行。
    // 不写 host 时 vite 在新版 Node 上只绑 ::1，127.0.0.1 会被拒；
    // 不写 strictPort 时端口被占会静默跳口，容易和别的 vite 实例混在一起。
    // 代理是服务端行为，浏览器只访问 4810，后端仍走 127.0.0.1，不用往外开
    host: '0.0.0.0',
    port: 4810,
    strictPort: true,
    proxy: {
      '/admin-api': proxy,
      '/api': proxy
    }
  },
  preview: {
    host: '0.0.0.0',
    port: 4810,
    strictPort: true
  }
})
