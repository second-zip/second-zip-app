<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getApiError } from '@/api/utils/error';
import { getLatestTerms } from '@/api/terms';
import { useAuthStore } from '@/stores/auth';
import { useSignupForm } from '@/composables/useSignupForm';
import { SIGNUP_FIELDS } from '@/constants/auth/signupFields';

import BottomSheetLayout from '@/layouts/BottomSheetLayout.vue';
import DefaultSheetHeader from '@/layouts/DefaultSheetHeader.vue';
import AuthInputBox from '@/components/auth/AuthInputBox.vue';
import BaseButton from '@/components/common/BaseButton.vue';

import AuthIcon from '@/assets/icons/nav/mypage-blue-22.svg';

const authStore = useAuthStore();
const router = useRouter();

const {
  form,
  getStatus,
  getMessage,
  handleFieldInput,
  startAllFields,
  isFormValid,
} = useSignupForm();

const errorMessage = ref('');
const expandedTermType = ref('');
const signupTerms = ref([]);
const isTermsLoading = ref(true);

const REQUIRED_TERM_TYPES = ['SERVICE', 'PRIVACY_POLICY'];

const loadSignupTerms = async () => {
  try {
    const terms = await getLatestTerms();

    signupTerms.value = terms.filter(
      ({ required, termType }) =>
        required && REQUIRED_TERM_TYPES.includes(termType),
    );
    form.termConsents = signupTerms.value.map(({ termId }) => ({
      termId,
      agreed: false,
    }));
  } catch (error) {
    errorMessage.value = getApiError(error).message;
  } finally {
    isTermsLoading.value = false;
  }
};

onMounted(loadSignupTerms);

const getConsent = (termId) =>
  form.termConsents.find((consent) => consent.termId === termId);

const hasRequiredConsents = computed(() =>
  signupTerms.value.length === REQUIRED_TERM_TYPES.length &&
  signupTerms.value.every(({ termId }) => getConsent(termId)?.agreed),
);

const toggleTerm = (termType) => {
  expandedTermType.value =
    expandedTermType.value === termType ? '' : termType;
};

const confirmTerm = (term) => {
  const consent = getConsent(term.termId);

  if (consent) {
    consent.agreed = true;
  }

  expandedTermType.value = '';
};

const handleSignup = async () => {
  startAllFields();

  errorMessage.value = '';

  if (!isFormValid()) {
    errorMessage.value = '입력값을 다시 확인해 주세요.';
    return;
  }

  if (!hasRequiredConsents.value) {
    errorMessage.value = '필수 약관을 모두 확인하고 동의해 주세요.';
    return;
  }

  try {
    await authStore.signup({ ...form });
    await router.replace('/login');
  } catch (error) {
    errorMessage.value = getApiError(error).message;
  }
};
</script>

<template>
  <BottomSheetLayout :title-ratio="10">
    <template #header>
      <DefaultSheetHeader title="회원가입" :icon="AuthIcon" />
    </template>

    <form
      class="signup-box w-100 d-flex flex-column"
      @submit.prevent="handleSignup"
    >
      <AuthInputBox
        v-for="field in SIGNUP_FIELDS"
        :id="field.id"
        :key="field.key"
        v-model="form[field.key]"
        :type="field.type"
        :label="field.label"
        :autocomplete="field.autocomplete"
        :message="getMessage(field.key)"
        :status="getStatus(field.key)"
        @update:model-value="handleFieldInput(field.key, $event)"
      />

      <section class="terms-section" aria-labelledby="terms-heading">
        <h2 id="terms-heading" class="terms-title fw-bold mb-1">
          약관 동의
        </h2>
        <p class="terms-description">
          필수 약관을 확인하고 동의해 주세요.
        </p>
        <div class="term-list">
          <p v-if="isTermsLoading" class="terms-state mb-0">
            약관을 불러오는 중입니다.
          </p>
          <article
            v-for="term in signupTerms"
            :key="term.termId"
            class="term-item"
          >
            <button
              class="term-row d-flex align-items-center"
              type="button"
              role="checkbox"
              :aria-checked="Boolean(getConsent(term.termId)?.agreed)"
              :aria-expanded="expandedTermType === term.termType"
              @click="toggleTerm(term.termType)"
            >
              <span
                class="term-checkbox"
                :class="{
                  'term-checkbox--checked': getConsent(term.termId)?.agreed,
                }"
                aria-hidden="true"
              >
                ✓
              </span>
              <span class="term-label"> [필수] {{ term.title }} 동의 </span>
              <span class="term-open-button" aria-hidden="true">
                <span
                  class="term-chevron"
                  :class="{
                    'term-chevron--expanded':
                      expandedTermType === term.termType,
                  }"
                >
                  ›
                </span>
              </span>
            </button>
            <div
              v-if="expandedTermType === term.termType"
              class="term-accordion"
            >
              <div class="term-content" tabindex="0">
                <pre>{{ term.content }}</pre>
              </div>
              <button
                class="term-inline-confirm"
                type="button"
                @click="confirmTerm(term)"
              >
                동의하고 확인
              </button>
            </div>
          </article>
        </div>
      </section>
      <p v-if="errorMessage" class="error-message fs-6 mb-0 fw-semibold w-100">
        {{ errorMessage }}
      </p>
      <BaseButton type="submit" :disabled="!hasRequiredConsents">
        계정 생성
      </BaseButton>
    </form>
  </BottomSheetLayout>
</template>

<style scoped>
.signup-box {
  gap: 0.875rem;
  padding: 12px 20px 16px;
}

/* 작은 화면에서도 스크롤은 유지하되 시각적인 스크롤바는 숨긴다. */
:deep(.sheet-layout) {
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

:deep(.sheet-layout::-webkit-scrollbar) {
  display: none;
}

.error-message {
  color: var(--red-500);
  text-align: center;
}

.terms-section {
  padding: 0.875rem 1rem;
  background: var(--gray-50, #f8f9fb);
  border-radius: 1rem;
}

.terms-title {
  color: var(--black-900);
  font-size: 1rem;
}

.terms-description {
  margin-bottom: 0.5rem;
  color: var(--black-500);
  font-size: 0.75rem;
}

.terms-state {
  color: var(--black-500);
  font-size: 0.8125rem;
}

.term-list {
  padding-top: 0.25rem;
}

.term-item {
  min-height: 2.5rem;
}

.term-row {
  width: 100%;
  min-height: 2.5rem;
  padding: 0;
  color: inherit;
  background: transparent;
  border: 0;
  gap: 0.5rem;
  text-align: left;
  cursor: pointer;
}

.term-row:focus-visible {
  outline: 2px solid var(--blue-500);
  outline-offset: 2px;
}

.term-label {
  flex: 1;
  color: var(--black-700);
  font-size: 0.8125rem;
}

.term-open-button {
  width: 2rem;
  height: 2rem;
  padding: 0;
  color: var(--gray-500);
  font-size: 1.5rem;
  line-height: 1;
}

.term-chevron {
  display: inline-block;
  transition: transform 0.2s ease;
}

.term-chevron--expanded {
  transform: rotate(90deg);
}

.term-checkbox {
  width: 1.125rem;
  height: 1.125rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  color: white;
  background: var(--gray-300, #d9dde5);
  border-radius: 50%;
  font-size: 0.6875rem;
  font-weight: 700;
}

.term-checkbox--checked {
  background: var(--blue-900, #075ef1);
}

.term-accordion {
  width: 100%;
  margin: 0.25rem 0 0.75rem 1.625rem;
}

.term-content {
  max-height: 14rem;
  padding: 0.875rem;
  overflow: auto;
  color: var(--black-700);
  background: white;
  border: 1px solid var(--gray-200);
  border-radius: 0.75rem;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.term-content::-webkit-scrollbar {
  display: none;
}

.term-content pre {
  margin: 0;
  font: inherit;
  font-size: 0.75rem;
  line-height: 1.65;
  white-space: pre-wrap;
}

.term-inline-confirm {
  width: 100%;
  height: 2.5rem;
  margin-top: 0.625rem;
  color: white;
  background: var(--blue-900, #075ef1);
  border: 0;
  border-radius: 0.625rem;
  font-size: 0.8125rem;
  font-weight: 700;
}

@media (min-width: 768px) {
  .signup-box {
    max-width: 720px;
    margin: 0 auto;
    padding: 24px 36px 40px;
  }
}

</style>
