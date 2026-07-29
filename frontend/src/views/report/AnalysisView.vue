<script setup>
import { computed, onBeforeUnmount, ref } from 'vue';

import BaseInput from '@/components/common/BaseInput.vue';
import ContentBox from '@/components/common/ContentBox.vue';
import {
  formatKoreanDeposit,
  selectSecretaryValue,
} from '@/views/report/analysisLogic';
import cautionIcon from '@/assets/icons/report/caution-yellow-22.svg';
import dangerIcon from '@/assets/icons/report/danger-red-22.svg';
import safeIcon from '@/assets/icons/report/safe-green-22.svg';
import catCaution from '@/assets/images/cat-caution.png';
import catDanger from '@/assets/images/cat-danger.png';
import catSafe from '@/assets/images/cat-safe.png';
import catDefault from '@/assets/images/cat.png';
import manCaution from '@/assets/images/main-caution.png';
import manDanger from '@/assets/images/main-danger.png';
import manSafe from '@/assets/images/man-safe.png';
import manDefault from '@/assets/images/man.png';
import womanCaution from '@/assets/images/woman-caution.png';
import womanDanger from '@/assets/images/woman-danger.png';
import womanSafe from '@/assets/images/woman-safe.png';
import womanDefault from '@/assets/images/woman.png';

// 화면 캐릭터 설정: 아래 두 값만 바꾸면 연결된 이미지와 멘트가 모두 변경됩니다.
const analysisDisplay = ref({
  // 비서 선택값: 'cat', 'man', 'woman'
  secretary: 'woman',
  // 위험도 선택값: 'safe', 'caution', 'danger'
  risk: 'caution',
});

const address = ref('서울시 마포구 합정동 123-45');
const deposit = ref('100000000');
const isDepositEditing = ref(false);
const openedItem = ref('mortgage');
const openedFraudType = ref('false-information');
const isChecklistPinned = ref(false);
const showCopyToast = ref(false);
const marketPrice = 180_722_892;
let toastTimer;

const checks = ref([
  {
    id: 'mortgage',
    label: '근저당',
    status: 'safe',
    basis: '등기부등본 기준 근저당권 설정 없음 확인',
    amount: '0원',
  },
  {
    id: 'violation',
    label: '위반건축물 표시',
    status: 'caution',
    basis: '건축물대장상 위반건축물 표기 여부 확인 필요',
    amount: '확인 필요',
  },
  {
    id: 'residential',
    label: '주거용건축물',
    status: 'safe',
    basis: '건축물대장 기준 주거용 용도 확인',
    amount: '주거용',
  },
  {
    id: 'hug',
    label: 'HUG가입가능여부',
    status: 'safe',
    basis: '입력 정보 기준 보증 가입 가능 범위',
    amount: '가입 가능',
  },
  {
    id: 'rights',
    label: '권리침해여부',
    status: 'danger',
    basis: '등기부등본상 권리침해 항목 확인 필요',
    amount: '확인 필요',
  },
]);

const fraudTypes = ref([
  {
    id: 'gap-investment',
    title: '유형 1. 무자본 갭투자·깡통전세형',
    subtitle: '전세가율·선순위채권·보증 가입 기준',
    items: [
      { label: 'A. 높은 전세가율', status: 'danger' },
      { label: 'B. 선순위채권 부담', status: 'caution' },
      { label: 'C. HUG보증보험 사전점검', status: 'safe' },
    ],
  },
  {
    id: 'false-information',
    title: '유형 2. 허위 정보·권리 은폐형',
    subtitle: '소유관계·용도·권리침해 기준',
    items: [
      { label: 'A. 건물·토지 소유관계 불일치', status: 'safe' },
      { label: 'B. 건축물 용도 허위 안내', status: 'caution' },
      { label: 'C. 등기상 권리침해 은폐', status: 'danger' },
    ],
  },
  {
    id: 'trust-property',
    title: '유형 3. 신탁 부동산 사기형',
    subtitle: '신탁등기·소유자·추가 권리침해 기준',
    items: [
      { label: 'A. 신탁등기 존재 여부', status: 'safe' },
      { label: 'B. 등기상 소유자 확인', status: 'safe' },
      { label: 'C. 신탁등기 이후 추가 권리침해 여부', status: 'caution' },
    ],
  },
]);

// AI 연결 전 화면 확인용 특약 사항 더미 데이터
const specialTerms = ref([
  {
    title: '보증금 즉시 반환 명시',
    description:
      '계약 해지 또는 만료 시 임대인은 잔금 지급일로부터 3영업일 이내에 보증금 전액을 임차인에게 반환해야 한다.',
  },
  {
    title: '근저당 추가 설정 금지',
    description:
      '계약 기간 중 임대인은 해당 부동산에 신규 근저당권을 설정하거나 기존 채권최고액을 증액할 수 없다.',
  },
  {
    title: '잔금일 등기부등본 재확인',
    description:
      '임차인은 잔금 송금 직전 해당 부동산의 등기부등본을 재발급해 이상 유무를 확인한 후 지급하여야 한다.',
  },
  {
    title: '전세보증보험 가입 협조',
    description:
      '임대인은 임차인의 HUG·SGI 전세보증보험 가입을 위한 서류 제출 및 관련 절차에 적극 협조하여야 한다.',
  },
  {
    title: '매각·양도 시 임차인 우선 보호',
    description:
      '계약 기간 중 임대인이 해당 부동산을 제3자에게 매각 또는 양도할 경우 임차인에게 사전 고지하고 보증금 반환 의무를 승계하도록 조치해야 한다.',
  },
]);

const specialTermsNotice =
  '* 본 특약은 AI 분석에 따른 권고사항이며,\n법적 효력은 계약서 작성 시 실제 내용에 따릅니다.';

const riskIcons = {
  safe: safeIcon,
  caution: cautionIcon,
  danger: dangerIcon,
};

//안전도에 따른 비서 이미지출력
// 캐릭터와 위험도별 문구를 이곳에서 직접 수정하면 화면에 반영됩니다.
const secretaryMessages = {
  // 고양이 분석 멘트
  cat: {
    safe: '안전이다냥!',
    caution: '조금 더 살펴보자냥!',
    danger: '위험하다냥!',
  },

  // 남자 위험 멘트
  man: {
    safe: '안전하네 나처럼',
    caution: '여기는 조심하는게 좋겠어',
    danger: '여기는 안돼!',
  },

  // 여자 위험 멘트
  woman: {
    safe: '안전한 집으로 추정됩니다.',
    caution: '확실히 주의가 필요합니다.',
    danger: '여긴 피하시길 권장합니다앗',
  },
};

const secretaryImages = {
  cat: {
    safe: catSafe,
    caution: catCaution,
    danger: catDanger,
  },
  man: {
    safe: manSafe,
    caution: manCaution,
    danger: manDanger,
  },
  woman: {
    safe: womanSafe,
    caution: womanCaution,
    danger: womanDanger,
  },
};

const secretaryImage = computed(() => {
  const riskImages = selectSecretaryValue(
    secretaryImages,
    analysisDisplay.value.secretary,
  );

  return riskImages[analysisDisplay.value.risk] ?? secretaryImages.cat.safe;
});
const defaultSecretaryImages = {
  cat: catDefault,
  man: manDefault,
  woman: womanDefault,
};
const defaultSecretaryImage = computed(() =>
  selectSecretaryValue(defaultSecretaryImages, analysisDisplay.value.secretary),
);
const overallIcon = computed(
  () => riskIcons[analysisDisplay.value.risk] ?? riskIcons.safe,
);
const overallMessage = computed(() => {
  const riskMessages = selectSecretaryValue(
    secretaryMessages,
    analysisDisplay.value.secretary,
  );

  return riskMessages[analysisDisplay.value.risk] ?? secretaryMessages.cat.safe;
});

const numericDeposit = computed(
  () => Number(deposit.value.replace(/\D/g, '')) || 0,
);
const formattedDeposit = computed(() =>
  formatKoreanDeposit(numericDeposit.value),
);
const depositInputValue = computed(() =>
  isDepositEditing.value ? deposit.value : formattedDeposit.value,
);
const rentRatio = computed(() =>
  Math.min(Math.round((numericDeposit.value / marketPrice) * 100), 999),
);

const handleDeposit = (value) => {
  deposit.value = value.replace(/\D/g, '');
};

const toggleItem = (id) => {
  openedItem.value = openedItem.value === id ? '' : id;
};

const toggleFraudType = (id) => {
  openedFraudType.value = openedFraudType.value === id ? '' : id;
};

const getFraudTypeStatus = (items) => {
  if (items.some((item) => item.status === 'danger')) return 'danger';
  if (items.some((item) => item.status === 'caution')) return 'caution';
  return 'safe';
};

const toggleChecklistPin = () => {
  isChecklistPinned.value = !isChecklistPinned.value;
};

const sharePage = async () => {
  const url = window.location.href;

  try {
    await navigator.clipboard.writeText(url);
  } catch {
    const textarea = document.createElement('textarea');
    textarea.value = url;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
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

onBeforeUnmount(() => {
  window.clearTimeout(toastTimer);
});
</script>

<template>
  <section class="analysis-page mx-auto bg-white">
    <!-- 주소와 책갈피·공유 기능을 표시하는 분석 페이지 상단 영역입니다. -->
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
        <BaseInput
          id="analysis-address"
          v-model="address"
          name="address"
          aria-label="분석 주소"
          placeholder="주소를 입력해 주세요"
        />
      </div>

      <div class="header-actions d-flex flex-shrink-0">
        <button
          class="icon-button"
          :class="{ active: isChecklistPinned }"
          type="button"
          :aria-pressed="isChecklistPinned"
          aria-label="필수 점검 상단 고정"
          @click="toggleChecklistPin"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="m12 3.5 2.63 5.33 5.88.85-4.25 4.15 1 5.85L12 16.91l-5.26 2.77 1-5.85-4.25-4.15 5.88-.85z"
            />
          </svg>
        </button>
        <button
          class="icon-button"
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

    <main class="analysis-content">
      <!-- 선택한 비서와 위험도에 맞는 캐릭터·판정 멘트를 표시하는 영역입니다. -->
      <section
        class="risk-summary d-flex"
        :class="`risk-summary--${analysisDisplay.risk}`"
      >
        <div
          class="character-wrap d-flex align-items-center justify-content-center rounded-circle"
          :class="`character-wrap--${analysisDisplay.risk}`"
        >
          <img :src="secretaryImage" alt="" />
        </div>
        <div class="risk-copy d-flex flex-column justify-content-center">
          <p class="font-kkubulim mb-0">이번 집의 전세 위험도는...</p>
          <div
            class="risk-result d-flex align-items-center justify-content-center"
          >
            <img :src="overallIcon" alt="" />
            <strong class="font-kkubulim">{{ overallMessage }}</strong>
          </div>
        </div>
      </section>

      <!-- 사용자가 보증금을 입력하고 전세가율을 확인하는 요약 카드입니다. -->
      <ContentBox class="price-card">
        <label class="price-field mb-0" for="deposit">
          <span>입력한 보증금</span>
          <span class="deposit-input-wrap">
            <BaseInput
              id="deposit"
              :model-value="depositInputValue"
              inputmode="numeric"
              aria-label="보증금"
              @focus="isDepositEditing = true"
              @blur="isDepositEditing = false"
              @update:model-value="handleDeposit"
            />
            <b>원</b>
          </span>
        </label>
        <div class="price-divider" aria-hidden="true"></div>
        <div class="ratio-field">
          <span>전세가율 <small>(80% 미만 안전)</small></span>
          <strong>{{ rentRatio }}%</strong>
        </div>
      </ContentBox>

      <!-- 필수 점검 결과를 상태별 아코디언으로 확인하는 영역입니다. -->
      <ContentBox
        class="inspection-card"
        :class="{ 'inspection-card--pinned': isChecklistPinned }"
        shadow
      >
        <div class="inspection-heading d-flex align-items-center">
          <span class="heading-dot" aria-hidden="true"></span>
          <h2 class="mb-0">필수 점검</h2>
          <img :src="dangerIcon" alt="위험" />
          <span class="danger-pill">위험</span>
        </div>

        <div class="accordion accordion-flush">
          <article
            v-for="check in checks"
            :key="check.id"
            class="accordion-item"
          >
            <h3 class="accordion-header">
              <button
                class="accordion-button d-flex"
                :class="{ collapsed: openedItem !== check.id }"
                type="button"
                :aria-expanded="openedItem === check.id"
                :aria-controls="`${check.id}-detail`"
                @click="toggleItem(check.id)"
              >
                <img :src="riskIcons[check.status]" alt="" />
                <span>{{ check.label }}</span>
              </button>
            </h3>

            <div
              :id="`${check.id}-detail`"
              class="accordion-collapse collapse"
              :class="{ show: openedItem === check.id }"
            >
              <div
                class="accordion-body"
                :class="`accordion-body--${check.status}`"
              >
                <div class="detail-row">
                  <span>판정 근거</span>
                  <strong>{{ check.basis }}</strong>
                </div>
                <div class="detail-row">
                  <span>사용 금액</span>
                  <strong :class="`text-${check.status}`">{{
                    check.amount
                  }}</strong>
                </div>
              </div>
            </div>
          </article>
        </div>
      </ContentBox>

      <!-- 예방 가능한 전세사기 유형과 세부 판정을 확인하는 영역입니다. -->
      <ContentBox class="fraud-card" shadow>
        <div class="fraud-heading d-flex align-items-center">
          <span class="heading-dot" aria-hidden="true"></span>
          <h2 class="mb-0">예방 가능한 전세사기 유형</h2>
        </div>

        <div class="accordion accordion-flush">
          <article
            v-for="fraudType in fraudTypes"
            :key="fraudType.id"
            class="accordion-item fraud-type"
          >
            <h3 class="accordion-header">
              <button
                class="accordion-button fraud-type-button"
                :class="{ collapsed: openedFraudType !== fraudType.id }"
                type="button"
                :aria-expanded="openedFraudType === fraudType.id"
                :aria-controls="`${fraudType.id}-detail`"
                @click="toggleFraudType(fraudType.id)"
              >
                <img
                  class="fraud-type-status"
                  :src="riskIcons[getFraudTypeStatus(fraudType.items)]"
                  alt=""
                />
                <span class="fraud-type-copy">
                  <strong>{{ fraudType.title }}</strong>
                  <small>{{ fraudType.subtitle }}</small>
                </span>
              </button>
            </h3>

            <div
              :id="`${fraudType.id}-detail`"
              class="accordion-collapse collapse"
              :class="{ show: openedFraudType === fraudType.id }"
            >
              <div class="fraud-items">
                <div
                  v-for="item in fraudType.items"
                  :key="item.label"
                  class="fraud-item d-flex align-items-center"
                  :class="`fraud-item--${item.status}`"
                >
                  <span>{{ item.label }}</span>
                  <img :src="riskIcons[item.status]" :alt="item.status" />
                </div>
              </div>
            </div>
          </article>
        </div>
      </ContentBox>

      <!-- AI 추천 특약 목록과 선택한 비서 캐릭터를 표시하는 영역입니다. -->
      <section class="special-terms">
        <div class="special-terms-heading d-flex align-items-center">
          <span class="heading-dot" aria-hidden="true"></span>
          <h2 class="mb-0">AI가 추천하는 특약 사항</h2>
        </div>

        <ol class="special-terms-list">
          <li
            v-for="(term, index) in specialTerms"
            :key="term.title"
            class="special-term"
          >
            <div class="term-title d-flex align-items-center">
              <span class="term-number">{{ index + 1 }}</span>
              <strong>{{ term.title }}</strong>
            </div>
            <p>{{ term.description }}</p>
          </li>
        </ol>

        <p class="special-terms-notice mb-0">
          {{ specialTermsNotice }}
        </p>

        <img
          class="special-terms-character"
          :src="defaultSecretaryImage"
          alt=""
        />
      </section>
    </main>

    <!-- 공유 링크 복사 완료를 알리는 하단 토스트 메시지입니다. -->
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
/* 분석 페이지 전체 너비와 기본 배경을 설정하는 영역입니다. */
.analysis-page {
  width: 100%;
  max-width: 25.125rem;
  min-height: 100dvh;
  color: var(--black-900);
}

/* 주소와 우측 상단 기능 버튼을 배치하는 영역입니다. */
.address-bar {
  min-height: 5rem;
  padding: 1rem 1.25rem 0.75rem;
}

.address {
  flex: 1;
  min-width: 0;
  gap: 0.375rem;
}

.address :deep(.base-input) {
  min-width: 0;
  height: auto;
  padding: 0;
  overflow: hidden;
  color: var(--black-900);
  background: transparent;
  border: 0;
  border-radius: 0;
  font-size: 0.875rem;
  font-weight: 500;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.address :deep(.base-input:focus) {
  background: transparent;
  border: 0;
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
  align-self: center;
  gap: 0.5625rem;
  margin-left: 0.75rem;
  transform: translateY(-0.25rem);
}

/* 우측 상단 책갈피·공유 버튼 */
.icon-button {
  width: 2.625rem;
  height: 2.625rem;
  padding: 0.625rem;
  color: var(--blue-900);
  background: white;
  border: 0.0625rem solid var(--blue-500);
  border-radius: 0.875rem;
  box-shadow: 0 0.125rem 0.375rem rgb(59 128 255 / 10%);
}

.icon-button svg {
  width: 100%;
  height: 100%;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
  transform: translateY(-0.125rem);
}

.icon-button.active {
  color: white;
  background: var(--blue-900);
  border-color: var(--blue-900);
}

.icon-button.active svg {
  fill: currentColor;
}

.analysis-content {
  padding: 0 1.25rem 12rem;
}

/* 비서 캐릭터 원과 위험도 판정 박스를 분리해 배치하는 영역입니다. */
.risk-summary {
  position: relative;
  width: 100%;
  height: 6.25rem;
  overflow: visible !important;
}

.risk-copy {
  min-width: 0;
  flex: 1;
  height: 6.25rem;
  margin-left: 7.0875rem;
  padding: 0.625rem 0.75rem;
  background: var(--green-100);
  border: 0.0625rem solid var(--green-500);
  border-radius: 1.25rem;
}

.risk-summary--caution .risk-copy {
  background: var(--yellow-100);
  border-color: var(--yellow-500);
}

.risk-summary--danger .risk-copy {
  background: var(--red-100);
  border-color: var(--red-500);
}

.character-wrap {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 2;
  width: 6.25rem;
  height: 6.25rem;
  flex-shrink: 0;
  overflow: hidden;
  background: var(--green-500);
  border: 0.0625rem solid var(--green-500);
}

.character-wrap--caution {
  background: var(--yellow-500);
  border-color: var(--yellow-500);
}

.character-wrap--danger {
  background: var(--red-500);
  border-color: var(--red-500);
}

.character-wrap img {
  width: 6.25rem;
  height: 6.25rem;
  object-fit: contain;
}

.risk-copy p {
  font-size: 0.75rem;
}

.risk-result {
  gap: 0.5rem;
  margin-top: 0.375rem;
}

.risk-result img {
  width: 3.25rem;
  height: 3.25rem;
}

.risk-result strong {
  min-width: 0;
  font-size: clamp(0.875rem, 4vw, 1.25rem);
  line-height: 1.2;
  text-align: center;
  overflow-wrap: anywhere;
}

/* 보증금 입력값과 전세가율을 나란히 표시하는 영역입니다. */
.price-card {
  display: grid !important;
  grid-template-columns: minmax(0, 1fr) 0.0625rem minmax(0, 1fr);
  align-items: center;
  min-height: 5.875rem;
  margin-top: 1rem;
  padding: 0.75rem 1.125rem;
  background: var(--blue-100);
  border-color: var(--blue-300) !important;
  border-radius: 1rem !important;
}

.price-field,
.ratio-field {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
}

.price-field {
  padding-right: 1rem;
}

.ratio-field {
  padding-left: 1.25rem;
}

.price-field span,
.ratio-field span {
  color: var(--blue-900);
  font-size: 0.75rem;
  font-weight: 700;
}

.ratio-field small {
  color: var(--blue-500);
  font-size: 0.5625rem;
  white-space: nowrap;
}

.deposit-input-wrap,
.ratio-field strong {
  margin-top: 0.25rem;
  font-size: 1.125rem;
  line-height: 1.25;
}

.deposit-input-wrap {
  display: flex;
  align-items: center;
  min-width: 0;
  color: var(--black-900) !important;
}

.deposit-input-wrap :deep(.base-input) {
  width: 100% !important;
  min-width: 0;
  height: 1.5rem;
  padding: 0;
  color: var(--black-900);
  background: transparent;
  border: 0;
  border-radius: 0;
  font-size: 1.125rem;
  line-height: 1.25;
}

.deposit-input-wrap :deep(.base-input:focus) {
  background: transparent;
  border: 0;
}

.deposit-input-wrap b {
  flex-shrink: 0;
  color: var(--black-900);
  font-size: 1.125rem;
  font-weight: 600;
  line-height: 1.25;
}

.ratio-field strong {
  font-weight: 600;
}

.price-divider {
  width: 0.0625rem;
  height: 4.25rem;
  background: var(--blue-300);
}

/* 필수 점검 카드와 고정 상태를 설정하는 영역입니다. */
.inspection-card {
  margin-top: 1rem;
  overflow: hidden;
  border-color: var(--black-100) !important;
  border-radius: 1rem !important;
}

.inspection-card--pinned {
  position: sticky;
  top: 0.75rem;
  z-index: 1020;
}

/* 예방 가능한 전세사기 유형 아코디언을 담는 영역입니다. */
.fraud-card {
  min-height: 25rem;
  margin-top: 1rem;
  overflow: hidden;
  border-color: var(--black-100) !important;
  border-radius: 1rem !important;
}

.fraud-heading {
  min-height: 3.25rem;
  padding: 0 1rem;
  gap: 0.5rem;
  border-bottom: 0.0625rem solid var(--black-100);
}

.fraud-heading h2 {
  font-size: 0.875rem;
  font-weight: 700;
}

.inspection-heading {
  min-height: 3.25rem;
  padding: 0 1rem;
  gap: 0.5rem;
  border-bottom: 0.0625rem solid var(--black-100);
}

.inspection-heading h2 {
  flex: 1;
  font-size: 0.875rem;
  font-weight: 700;
}

.inspection-heading img {
  width: 1.125rem;
  height: 1.125rem;
}

.heading-dot {
  width: 0.375rem;
  height: 0.375rem;
  background: var(--blue-900);
  border-radius: 50%;
}

.danger-pill {
  padding: 0.125rem 0.375rem;
  color: var(--red-500);
  background: var(--red-100);
  border-radius: 999rem;
  font-size: 0.5625rem;
  font-weight: 700;
}

.accordion-item {
  border-color: var(--black-100);
}

/* 필수 점검 항목 아코디언 버튼 */
.accordion-button {
  min-height: 3rem;
  padding: 0.75rem 1rem;
  gap: 0.625rem;
  color: var(--black-900);
  background: white;
  font-size: 0.75rem;
  font-weight: 500;
  box-shadow: none;
}

.accordion-button:not(.collapsed) {
  color: var(--black-900);
  background: white;
  box-shadow: none;
}

.accordion-button:focus {
  box-shadow: none;
}

.accordion-button img {
  width: 1.125rem;
  height: 1.125rem;
  flex: 0 0 1.125rem;
}

.accordion-button::after {
  width: 0.75rem;
  height: 0.75rem;
  margin-left: auto;
  background-size: 0.75rem;
  opacity: 0.55;
}

/* 예방 가능한 전세사기 유형 아코디언 버튼 */
.fraud-type-button {
  min-height: 3.75rem;
  gap: 0.625rem;
}

.fraud-type-status {
  width: 1.125rem;
  height: 1.125rem;
  flex: 0 0 1.125rem;
}

.fraud-type-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 0.1875rem;
}

.fraud-type-copy strong {
  font-size: 0.75rem;
  font-weight: 700;
}

.fraud-type-copy small {
  overflow: hidden;
  color: var(--black-300);
  font-size: 0.625rem;
  font-weight: 400;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fraud-items {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0 0.875rem 0.875rem;
}

.fraud-item {
  min-height: 2.5rem;
  padding: 0.5rem 0.75rem;
  gap: 0.625rem;
  background: var(--green-100);
  border: 0.0625rem solid var(--green-500);
  border-radius: 0.625rem;
  font-size: 0.6875rem;
  font-weight: 600;
}

.fraud-item span {
  min-width: 0;
  flex: 1;
}

.fraud-item img {
  width: 1.125rem;
  height: 1.125rem;
  flex: 0 0 1.125rem;
}

.fraud-item--caution {
  background: var(--yellow-100);
  border-color: var(--yellow-500);
}

.fraud-item--danger {
  background: var(--red-100);
  border-color: var(--red-500);
}

/* AI 추천 특약 목록·경고 문구·하단 캐릭터를 배치하는 영역입니다. */
.special-terms {
  position: relative;
  margin-top: 1rem;
  margin-bottom: 0;
  padding: 1rem 1rem 4rem;
  overflow: visible;
  background: white;
  border: 0.0625rem dashed var(--black-300);
  border-radius: 1rem;
}

.special-terms-heading {
  gap: 0.5rem;
  padding-bottom: 0.875rem;
  border-bottom: 0.0625rem dashed var(--black-100);
}

.special-terms-heading h2 {
  font-size: 0.875rem;
  font-weight: 700;
}

.special-terms-list {
  position: relative;
  z-index: 3;
  display: flex;
  max-height: 40rem;
  flex-direction: column;
  gap: 0.875rem;
  margin: 1rem 0 0;
  padding: 0 0.375rem 0.75rem 0;
  overflow-y: scroll;
  background: white;
  border-bottom: 0.0625rem dashed var(--black-100);
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: var(--blue-300) transparent;
  list-style: none;
}

.special-terms-list::-webkit-scrollbar {
  width: 0.25rem;
}

.special-terms-list::-webkit-scrollbar-thumb {
  background: var(--blue-300);
  border-radius: 999rem;
}

.special-term {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.term-title {
  gap: 0.5rem;
}

.term-title strong {
  font-size: 0.75rem;
  font-weight: 700;
}

.term-number {
  display: inline-flex;
  width: 1.25rem;
  height: 1.25rem;
  align-items: center;
  justify-content: center;
  flex: 0 0 1.25rem;
  color: var(--blue-900);
  background: var(--blue-100);
  border: 0.0625rem solid var(--blue-300);
  border-radius: 50%;
  font-size: 0.625rem;
  font-weight: 700;
}

.special-term p {
  margin: 0 0 0 1.75rem;
  padding: 0.75rem;
  color: var(--black-700);
  background: var(--blue-100);
  border: 0.0625rem solid var(--blue-300);
  border-radius: 0.75rem;
  font-size: 0.625rem;
  line-height: 1.5;
  box-shadow: 0 0.125rem 0.375rem rgb(59 128 255 / 10%);
}

.special-terms-notice {
  position: absolute;
  right: 1rem;
  bottom: 0.875rem;
  left: 1rem;
  z-index: 3;
  max-width: 13rem;
  color: var(--black-500);
  font-size: 0.5625rem;
  line-height: 1.4;
  white-space: pre-line;
}

.special-terms-character {
  position: absolute;
  right: -4.5rem;
  bottom: -11rem;
  z-index: 2;
  width: 15rem;
  height: 15rem;
  object-fit: contain;
  pointer-events: none;
}

.accordion-body {
  margin: 0 0.875rem 0.875rem;
  padding: 0.25rem 0.75rem;
  background: var(--green-100);
  border: 0.0625rem solid var(--green-500);
  border-radius: 0.75rem;
}

.accordion-body--caution {
  background: var(--yellow-100);
  border-color: var(--yellow-500);
}

.accordion-body--danger {
  background: var(--red-100);
  border-color: var(--red-500);
}

.accordion-body--caution .detail-row + .detail-row {
  border-color: rgb(247 144 9 / 30%);
}

.accordion-body--danger .detail-row + .detail-row {
  border-color: rgb(247 9 9 / 30%);
}

.detail-row {
  display: grid;
  grid-template-columns: 4.5rem minmax(0, 1fr);
  padding: 0.625rem 0;
  font-size: 0.6875rem;
}

.detail-row + .detail-row {
  border-top: 0.0625rem solid rgb(18 183 106 / 30%);
}

.detail-row span {
  font-weight: 600;
}

.detail-row strong {
  font-weight: 500;
}

.text-safe {
  color: var(--green-500);
}

.text-caution {
  color: var(--yellow-500);
}

.text-danger {
  color: var(--red-500) !important;
}

/* 링크 복사 완료 상태를 안내하는 토스트 메시지입니다. */
.copy-toast {
  bottom: 5rem;
  z-index: 1090;
  width: max-content;
  max-width: calc(100% - 2.5rem);
  padding: 0.75rem 1rem;
  color: white;
  background: rgb(17 17 24 / 88%);
  border-radius: 0.75rem;
  font-size: 0.8125rem;
  font-weight: 600;
  box-shadow: 0 0.25rem 1rem rgb(17 17 24 / 16%);
}

.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 0.5rem) !important;
}

@media (max-width: 22rem) {
  .address-bar,
  .analysis-content {
    padding-right: 1rem;
    padding-left: 1rem;
  }

  .character-wrap {
    width: 6.25rem;
  }

  .risk-result strong {
    font-size: 1.0625rem;
  }
}
</style>
