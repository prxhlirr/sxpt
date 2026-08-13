import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  plugins: [vue()],
  optimizeDeps: {
    // 该依赖发布包内的 index.js.map 存在未闭合字符串，dev 预构建会被 esbuild 解析失败；跳过预构建后由浏览器按 ESM 加载。
    exclude: ['@open-file-viewer/core']
  },
  resolve: {
    alias: {
      // hls.js 当前包声明的 ESM 入口在本地安装目录中缺失，构建阶段固定到实际存在的入口。
      'hls.js': fileURLToPath(new URL('./node_modules/hls.js/dist/hls.js', import.meta.url)),
      // mermaid 当前包声明的 ESM 入口在本地安装目录中缺失，构建阶段固定到实际存在的入口。
      mermaid: fileURLToPath(new URL('./node_modules/mermaid/dist/mermaid.js', import.meta.url))
    }
  },
  test: {
    environment: 'node',
    globals: true
  }
});
