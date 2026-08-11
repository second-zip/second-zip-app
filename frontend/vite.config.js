import { fileURLToPath, URL } from 'node:url';

import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import vueDevTools from 'vite-plugin-vue-devtools';

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: './coverage',
      thresholds: {
        statements: 90,
        branches: 85,
        functions: 90,
        lines: 90,
      },
      include: [
        'src/api/auth.js',
        'src/api/token.js',
        'src/stores/auth.js',
        'src/utils/authValidator.js',
        'src/composables/useSignupForm.js',
        'src/composables/useDictionaryCharacter.js',
        'src/utils/logger.js',
        'src/components/auth/AuthInputBox.vue',
        'src/views/auth/LoginView.vue',
        'src/views/auth/SignupView.vue',
        'src/api/map.js',
        'src/api/report.js',
        'src/constants/map/regionMap.js',
        'src/constants/report/list.js',
        'src/utils/map/**/*.js',
        'src/utils/report/date.js',
        'src/utils/report/list.js',
        'src/composables/map/**/*.js',
        'src/composables/report/useReportList.js',
        'src/components/main/MainDataTabs.vue',
        'src/components/main/RiskMapCard.vue',
        'src/components/main/map/KoreaRegionMap.vue',
        'src/components/common/secretary/SecretaryCharacter.vue',
        'src/components/common/secretary/SecretaryGuide.vue',
        'src/components/report/list/*.vue',
        'src/views/MainPageView.vue',
        'src/views/report/ReportListView.vue',
      ],
    },
  },
});
