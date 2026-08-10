<!-- 분석 메인화면:분석결과들 임포트해와서 조립해서 보여주는 곳-->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { createReport, getReport } from '@/api/report';
import AnalysisContent from '@/components/report/analysis/AnalysisContent.vue';
import {
  ANALYSIS_REQUEST,
  DEFAULT_CHECKS,
  DEFAULT_FRAUD_TYPES,
  DEFAULT_SECRETARY_IMAGES,
  DEFAULT_SPECIAL_TERMS,
  MARKET_PRICE,
  RISK_ICONS,
  RISK_LABELS,
  SECRETARY_IMAGES,
  SECRETARY_MESSAGES,
  SPECIAL_TERMS_NOTICE,
} from '@/constants/report/analysis';
import {
  ANALYSIS_PREVIEW_REPORTS,
  MOCK_REPORT_DETAIL,
} from '@/constants/report/mock';
import {
  aggregateRiskStatuses,
  formatKoreanDeposit,
  getRentRatio,
  selectSecretaryValue,
  toNumericAmount,
} from '@/utils/report/analysis';
import { mapReportDetail } from '@/utils/report/mapper';
import { logger } from '@/utils/logger';
import { normalizeCharacterType } from '@/utils/character';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const secretary = ref('cat');
const activeSecretary = computed(() =>
  normalizeCharacterType(
    authStore.characterType ?? authStore.myPage?.characterType,
    secretary.value,
  ),
);
const address = ref('서울시 마포구 합정동 123-45');
const deposit = ref('100000000');
const isFavorite = ref(false);
const showCopyToast = ref(false);
const isReportLoading = ref(false);
const reportLoadError = ref('');
const checks = ref(DEFAULT_CHECKS);
const fraudTypes = ref(DEFAULT_FRAUD_TYPES);
const specialTerms = ref(DEFAULT_SPECIAL_TERMS);
let toastTimer;

onMounted(async () => {
  if (!authStore.isAuthenticated || authStore.myPage) return;

  try {
    await authStore.fetchMyPage();
  } catch (error) {
    logger.error('analysis.fetch-user', error);
    // 조회 실패 시에는 리포트에 저장된 비서 값으로 안전하게 대체합니다.
  }
});

const checkRisk = computed(() =>
  aggregateRiskStatuses(checks.value.map(({ status }) => status)),
);
const fraudStatuses = computed(() =>
  fraudTypes.value.flatMap(({ items }) => items.map(({ status }) => status)),
);
const fraudRisk = computed(() => aggregateRiskStatuses(fraudStatuses.value));
const overallRisk = computed(() =>
  aggregateRiskStatuses([
    ...checks.value.map(({ status }) => status),
    ...fraudStatuses.value,
  ]),
);
const secretaryImage = computed(() => {
  const images = selectSecretaryValue(SECRETARY_IMAGES, activeSecretary.value);

  return images[overallRisk.value] ?? SECRETARY_IMAGES.cat.safe;
});
const defaultSecretaryImage = computed(() =>
  selectSecretaryValue(DEFAULT_SECRETARY_IMAGES, activeSecretary.value),
);
const overallIcon = computed(
  () => RISK_ICONS[overallRisk.value] ?? RISK_ICONS.safe,
);
const overallMessage = computed(() => {
  const messages = selectSecretaryValue(SECRETARY_MESSAGES, activeSecretary.value);

  return messages[overallRisk.value] ?? SECRETARY_MESSAGES.cat.safe;
});
const numericDeposit = computed(() => toNumericAmount(deposit.value));
const formattedDeposit = computed(() =>
  deposit.value === '-' ? '-' : formatKoreanDeposit(numericDeposit.value),
);
const rentRatioDisplay = computed(() =>
  deposit.value === '-'
    ? '-'
    : `${getRentRatio(numericDeposit.value, MARKET_PRICE)}%`,
);

const applyReport = (report) => {
  const mapped = mapReportDetail(report);

  address.value = mapped.address;
  deposit.value = mapped.deposit;
  secretary.value = mapped.secretary ?? secretary.value;
  checks.value = mapped.checks;
  fraudTypes.value = mapped.fraudTypes;
  specialTerms.value = mapped.specialTerms.length
    ? mapped.specialTerms
    : DEFAULT_SPECIAL_TERMS;
};

const loadReport = async (analysisReportId) => {
  isReportLoading.value = true;
  reportLoadError.value = '';

  try {
    if (route.meta.analysisPreview) {
      const scenario =
        typeof route.meta.analysisPreview === 'string'
          ? route.meta.analysisPreview
          : route.params.scenario;

      applyReport(ANALYSIS_PREVIEW_REPORTS[scenario] ?? MOCK_REPORT_DETAIL);
      return;
    }

    const report = analysisReportId
      ? await getReport(analysisReportId)
      : await createReport(ANALYSIS_REQUEST);

    applyReport(report);
    if (!analysisReportId) {
      await router.replace({
        name: 'analysis',
        params: { analysisReportId: report.analysisReportId },
      });
    }
  } catch (error) {
    logger.error('analysis.load-report', error, {
      analysisReportId,
    });
    reportLoadError.value = '분석 결과를 불러오지 못했습니다.';
  } finally {
    isReportLoading.value = false;
  }
};

watch(
  () => [route.params.analysisReportId, route.params.scenario],
  ([analysisReportId]) => loadReport(analysisReportId),
  { immediate: true },
);

const sharePage = async () => {
  try {
    await navigator.clipboard.writeText(window.location.href);
  } catch {
    const textarea = document.createElement('textarea');

    textarea.value = window.location.href;
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    textarea.remove();
  }

  window.clearTimeout(toastTimer);
  showCopyToast.value = true;
  toastTimer = window.setTimeout(() => {
    showCopyToast.value = false;
  }, 2000);
};

onBeforeUnmount(() => window.clearTimeout(toastTimer));
</script>

<template>
  <section class="analysis-page mx-auto bg-white">
    <header
      class="address-bar d-flex align-items-center justify-content-between"
    >
      <div class="address d-flex align-items-center min-w-0">
        <svg
          class="address-icon flex-shrink-0"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <path d="M7 3h8l4 4v14H7zM15 3v5h4M10 12h6M10 16h6" />
        </svg>
        <h1 class="text-truncate mb-0">{{ address }}</h1>
      </div>

      <div class="header-actions d-flex flex-shrink-0">
        <button
          class="icon-button favorite-button"
          :class="{ active: isFavorite }"
          type="button"
          :aria-pressed="isFavorite"
          :aria-label="isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가'"
          @click="isFavorite = !isFavorite"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="m12 3.5 2.63 5.33 5.88.85-4.25 4.15 1 5.85L12 16.91l-5.26 2.77 1-5.85-4.25-4.15 5.88-.85z"
            />
          </svg>
        </button>
        <button
          class="icon-button share-button"
          type="button"
          aria-label="현재 페이지 링크 복사"
          @click="sharePage"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="18" cy="5" r="2.25" />
            <circle cx="6" cy="12" r="2.25" />
            <circle cx="18" cy="19" r="2.25" />
            <path d="m8 11 8-5M8 13l8 5" />
          </svg>
        </button>
      </div>
    </header>

    <div v-if="isReportLoading" class="report-feedback" role="status">
      분석 결과를 불러오는 중입니다.
    </div>
    <div
      v-else-if="reportLoadError"
      class="report-feedback report-feedback--error"
    >
      <span>{{ reportLoadError }}</span>
      <button type="button" @click="loadReport(route.params.analysisReportId)">
        다시 시도
      </button>
    </div>

    <!--
      분석 결과 구역
      1. 위험도 요약/비서 결과
      2. 보증금·전세가율
      3. 필수 점검
      4. 전세사기 유형·세부 항목
      5. AI 추천 특약
    -->
    <AnalysisContent
      :overall-risk
      :secretary-image
      :overall-icon
      :overall-message
      :formatted-deposit
      :rent-ratio-display
      :checks
      :check-risk
      :fraud-types
      :fraud-risk
      :risk-icons="RISK_ICONS"
      :risk-labels="RISK_LABELS"
      :special-terms
      :special-terms-notice="SPECIAL_TERMS_NOTICE"
      :default-secretary-image
    />

    <Transition name="toast">
      <div
        v-if="showCopyToast"
        class="copy-toast position-fixed start-50 translate-middle-x"
        role="status"
        aria-live="polite"
      >
        링크를 복사했습니다.
      </div>
    </Transition>
  </section>
</template>

<style scoped>
.analysis-page {
  width: 100%;
  max-width: 25.125rem;
  min-height: 100dvh;
  color: var(--black-900);
  caret-color: transparent;
  user-select: none;
}

.address-bar {
  min-height: 5rem;
  padding: 1rem 1.25rem 0.75rem;
}

.report-feedback {
  margin: 0 1.25rem 1rem;
  padding: 0.75rem 1rem;
  color: var(--blue-900);
  background: var(--blue-100);
  border: 0.0625rem solid var(--blue-300);
  border-radius: 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  text-align: center;
}

.report-feedback--error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  color: var(--red-500);
  background: var(--red-100);
  border-color: var(--red-500);
}

.report-feedback button {
  padding: 0.25rem 0.5rem;
  color: inherit;
  background: white;
  border: 0.0625rem solid currentColor;
  border-radius: 0.5rem;
  font-size: 0.6875rem;
  font-weight: 600;
}

.address {
  flex: 1;
  gap: 0.375rem;
}

.address h1 {
  overflow: hidden;
  font-size: 0.875rem;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.address-icon {
  width: 1.125rem;
  height: 1.125rem;
  fill: none;
  stroke: var(--blue-900);
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.7;
}

.header-actions {
  gap: 0.5625rem;
  margin-left: 0.75rem;
}

.icon-button {
  width: 2.625rem;
  height: 2.625rem;
  padding: 0.625rem;
  color: var(--blue-900);
  background: white;
  border: 0.0625rem solid var(--blue-500);
  border-radius: 0.875rem;
}

.icon-button svg {
  width: 100%;
  height: 100%;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.icon-button.active {
  color: white;
  background: var(--blue-900);
  border-color: var(--blue-900);
}

.icon-button.active svg {
  fill: currentColor;
}

.favorite-button svg,
.share-button svg {
  transform: translateY(-0.125rem);
}

.copy-toast {
  bottom: 5rem;
  z-index: 1090;
  max-width: calc(100% - 2.5rem);
  padding: 0.75rem 1rem;
  color: white;
  background: rgb(17 17 24 / 88%);
  border-radius: 0.75rem;
  font-size: 0.8125rem;
  font-weight: 600;
}

.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.2s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
}
</style>
