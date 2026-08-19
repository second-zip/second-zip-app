<!-- 분석 메인화면:분석결과들 임포트해와서 조립해서 보여주는 곳-->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  addReportFavorite,
  createReport,
  deleteReportFavorite,
  getReport,
  getSharedReport,
  shareReport,
} from '@/api/report';
import { createChecklist, getChecklists } from '@/api/checklist';
import { getApiError } from '@/api/utils/error';
import FavoriteIcon from '@/assets/icons/report/favorite-blue-18.svg';
import FavoriteLineIcon from '@/assets/icons/report/favorite-line-18.svg';
import ReportIcon from '@/assets/icons/report/report-blue-18.svg';
import ShareIcon from '@/assets/icons/report/share-blue-16.svg';
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
import {
  ANALYSIS_AND_CHECKLIST_NOTICE,
  GUARANTEE_ELIGIBILITY_NOTICE,
} from '@/constants/legalNotices';
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
const currentReportId = ref(null);
const isFavorite = ref(false);
const isFavoriteUpdating = ref(false);
const isSharing = ref(false);
const isChecklistNavigating = ref(false);
const showCopyToast = ref(false);
const isReportLoading = ref(false);
const reportLoadError = ref('');
const reportActionError = ref('');
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
  const messages = selectSecretaryValue(
    SECRETARY_MESSAGES,
    activeSecretary.value,
  );

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

  currentReportId.value = mapped.analysisReportId ?? null;
  address.value = mapped.address;
  deposit.value = mapped.deposit;
  isFavorite.value = mapped.favorite;
  secretary.value = mapped.secretary ?? secretary.value;
  checks.value = mapped.checks;
  fraudTypes.value = mapped.fraudTypes;
  specialTerms.value = mapped.specialTerms.length
    ? mapped.specialTerms
    : DEFAULT_SPECIAL_TERMS;
};

const getNavigationReport = (analysisReportId) => {
  const navigationState = window.history.state;
  const navigationAnalysis = navigationState?.analysisResult;

  if (
    !navigationAnalysis ||
    String(navigationAnalysis.analysisReportId) !== String(analysisReportId)
  ) {
    return null;
  }

  const generatedSpecialTerms =
    navigationState.specialTermsResult?.specialTerms;

  return {
    ...navigationAnalysis,
    specialTerms: Array.isArray(generatedSpecialTerms)
      ? generatedSpecialTerms
      : navigationAnalysis.specialTerms,
  };
};

const loadReport = async (analysisReportId, shareToken) => {
  isReportLoading.value = true;
  reportLoadError.value = '';
  reportActionError.value = '';

  try {
    if (route.meta.analysisPreview) {
      const scenario =
        typeof route.meta.analysisPreview === 'string'
          ? route.meta.analysisPreview
          : route.params.scenario;

      applyReport(ANALYSIS_PREVIEW_REPORTS[scenario] ?? MOCK_REPORT_DETAIL);
      return;
    }

    if (route.meta.analysisShared) {
      applyReport(await getSharedReport(shareToken));
      return;
    }

    const navigationReport = analysisReportId
      ? getNavigationReport(analysisReportId)
      : null;
    const report =
      navigationReport ??
      (analysisReportId
        ? await getReport(analysisReportId)
        : await createReport(ANALYSIS_REQUEST));

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
  () => [
    route.params.analysisReportId,
    route.params.scenario,
    route.params.shareToken,
  ],
  ([analysisReportId, , shareToken]) =>
    loadReport(analysisReportId, shareToken),
  { immediate: true },
);

const copyText = async (text) => {
  try {
    await navigator.clipboard.writeText(text);
  } catch {
    const textarea = document.createElement('textarea');

    textarea.value = text;
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    textarea.remove();
  }
};

const showLinkCopiedToast = () => {
  window.clearTimeout(toastTimer);
  showCopyToast.value = true;
  toastTimer = window.setTimeout(() => {
    showCopyToast.value = false;
  }, 2000);
};

const toggleFavorite = async () => {
  if (!currentReportId.value || isFavoriteUpdating.value) return;

  isFavoriteUpdating.value = true;
  reportActionError.value = '';
  const nextFavorite = !isFavorite.value;

  try {
    const request = nextFavorite
      ? addReportFavorite
      : deleteReportFavorite;
    await request(currentReportId.value);
    isFavorite.value = nextFavorite;
  } catch (error) {
    logger.error('analysis.toggle-favorite', error, {
      analysisReportId: currentReportId.value,
    });
    reportActionError.value = getApiError(error).message;
  } finally {
    isFavoriteUpdating.value = false;
  }
};

const sharePage = async () => {
  if (!currentReportId.value || isSharing.value) return;

  isSharing.value = true;
  reportActionError.value = '';

  try {
    const { shareToken } = await shareReport(currentReportId.value);
    if (!shareToken) throw new Error('Share token is missing');

    const sharePath = router.resolve({
      name: 'analysis-shared',
      params: { shareToken },
    }).href;
    const shareUrl = new URL(sharePath, window.location.origin).href;

    await copyText(shareUrl);
    showLinkCopiedToast();
  } catch (error) {
    logger.error('analysis.share-report', error, {
      analysisReportId: currentReportId.value,
    });
    reportActionError.value = getApiError(error).message;
  } finally {
    isSharing.value = false;
  }
};

const goToReportList = () => router.push({ name: 'report-list' });

const openChecklist = async () => {
  if (!currentReportId.value || isChecklistNavigating.value) return;

  isChecklistNavigating.value = true;
  reportActionError.value = '';

  try {
    const checklists = await getChecklists();
    const currentChecklist = Array.isArray(checklists)
      ? checklists.find(
          ({ analysisReportId }) =>
            String(analysisReportId) === String(currentReportId.value),
        )
      : null;
    const hasChecklist = Boolean(
      currentChecklist?.checklistCreated &&
        currentChecklist?.reportChecklistId,
    );
    const createdChecklist = hasChecklist
      ? currentChecklist
      : await createChecklist(currentReportId.value);
    const reportChecklistId = createdChecklist?.reportChecklistId;

    if (!reportChecklistId) throw new Error('Checklist id is missing');

    await router.push({
      name: 'checklist-detail',
      params: { reportChecklistId },
    });
  } catch (error) {
    logger.error('analysis.open-checklist', error, {
      analysisReportId: currentReportId.value,
    });
    reportActionError.value = getApiError(error).message;
  } finally {
    isChecklistNavigating.value = false;
  }
};

onBeforeUnmount(() => window.clearTimeout(toastTimer));
</script>

<template>
  <section class="analysis-page mx-auto bg-white">
    <header
      class="address-bar d-flex align-items-center justify-content-between"
    >
      <div class="address d-flex align-items-center min-w-0">
        <img
          :src="ReportIcon"
          class="address-icon flex-shrink-0"
          alt=""
        />
        <h1 class="text-truncate mb-0">{{ address }}</h1>
      </div>

      <div
        v-if="!route.meta.analysisShared"
        class="header-actions d-flex flex-shrink-0"
      >
        <button
          class="icon-button favorite-button"
          :class="{ active: isFavorite }"
          type="button"
          :disabled="isFavoriteUpdating || !currentReportId"
          :aria-pressed="isFavorite"
          :aria-label="isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가'"
          @click="toggleFavorite"
        >
          <img
            :src="isFavorite ? FavoriteIcon : FavoriteLineIcon"
            alt=""
          />
        </button>
        <button
          class="icon-button share-button"
          type="button"
          :disabled="isSharing || !currentReportId"
          aria-label="리포트 공유 링크 복사"
          @click="sharePage"
        >
          <img :src="ShareIcon" alt="" />
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
      <button
        type="button"
        @click="
          loadReport(
            route.params.analysisReportId,
            route.params.shareToken,
          )
        "
      >
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
    >
      <div
        v-if="!route.meta.analysisShared"
        class="result-actions"
        aria-label="분석 결과 다음 동작"
      >
        <p
          v-if="reportActionError"
          class="result-actions__error"
          role="alert"
        >
          {{ reportActionError }}
        </p>
        <div class="result-actions__buttons d-grid">
          <button
            type="button"
            class="result-actions__button result-actions__button--secondary"
            @click="goToReportList"
          >
            목록 보기
          </button>
          <button
            type="button"
            class="result-actions__button result-actions__button--primary"
            :disabled="isChecklistNavigating || !currentReportId"
            @click="openChecklist"
          >
            {{ isChecklistNavigating ? '확인 중...' : '체크리스트 확인' }}
          </button>
        </div>
      </div>
    </AnalysisContent>

    <aside class="legal-notices" aria-label="분석 결과 이용 시 유의사항">
      <p>{{ ANALYSIS_AND_CHECKLIST_NOTICE }}</p>
      <p>{{ GUARANTEE_ELIGIBILITY_NOTICE }}</p>
    </aside>

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

.legal-notices {
  margin: 1rem 1.25rem 2rem;
  padding: 1rem;
  color: var(--gray-700);
  background: var(--gray-100);
  border-radius: 0.75rem;
  font-size: 0.6875rem;
  line-height: 1.6;
}

.result-actions {
  width: min(12rem, 100%);
  margin-top: 2rem;
}

.result-actions__buttons {
  grid-template-columns: minmax(0, 1fr);
  gap: 0.75rem;
}

.result-actions__button {
  min-height: 3rem;
  padding: 0.75rem;
  border: 0.0625rem solid var(--blue-700);
  border-radius: 0.75rem;
  font-size: 0.8125rem;
  font-weight: 700;
}

.result-actions__button--secondary {
  color: var(--blue-700);
  background: white;
}

.result-actions__button--primary {
  color: white;
  background: var(--blue-700);
}

.result-actions__button:disabled,
.icon-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.result-actions__error {
  margin: 0 0 0.75rem;
  color: var(--red-500);
  font-size: 0.75rem;
  text-align: left;
}

.legal-notices p {
  margin: 0;
}

.legal-notices p + p {
  margin-top: 0.5rem;
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

.icon-button img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.icon-button.active {
  background: var(--blue-100);
  border-color: var(--blue-700);
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

@media (min-width: 768px) {
  .analysis-page {
    max-width: 900px;
    min-height: 100%;
    padding: 0 clamp(16px, 3vw, 40px) 32px;
  }

  .copy-toast {
    bottom: 2rem;
    left: calc(50% + (var(--app-sidebar-width) / 2)) !important;
  }
}
</style>
