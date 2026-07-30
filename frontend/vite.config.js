import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  test: {
    environment: 'jsdom',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: './coverage',
      include: [
        'src/api/auth.js',
        'src/api/token.js',
        'src/stores/auth.js',
        'src/utils/authValidator.js',
        'src/composables/useSignupForm.js',
        'src/components/auth/AuthInputBox.vue',
        'src/views/auth/LoginView.vue',
        'src/views/auth/SignupView.vue',
      ],
    },
  },
})
