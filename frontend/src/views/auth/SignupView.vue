<script setup>
import { ref } from 'vue';

import { getApiError } from '@/api/utils/error';
import { useAuthStore } from '@/stores/auth';
import { useSignupForm } from '@/composables/useSignupForm';
import { SIGNUP_FIELDS } from '@/constants/signupFields';

import BottomSheetLayout from '@/layouts/BottomSheetLayout.vue';
import DefaultSheetHeader from '@/layouts/DefaultSheetHeader.vue';
import AuthInputBox from '@/components/auth/AuthInputBox.vue';
import BaseButton from '@/components/common/BaseButton.vue';

import AuthIcon from '@/assets/icons/nav/mypage-blue-22.svg';

const authStore = useAuthStore();

const {
  form,
  getStatus,
  getMessage,
  handleFieldInput,
  startAllFields,
  isFormValid,
} = useSignupForm();

const errorMessage = ref('');
const successMessage = ref('');

const handleSignup = async () => {
  startAllFields();

  errorMessage.value = '';
  successMessage.value = '';

  if (!isFormValid()) {
    errorMessage.value = '입력값을 다시 확인해 주세요.';
    return;
  }

  try {
    const user = await authStore.signup({ ...form });

    successMessage.value = `${user.nickname}님의 회원가입이 완료되었습니다.`;
  } catch (error) {
    errorMessage.value = getApiError(error).message;
  }
};
</script>

<template>
  <BottomSheetLayout :title-ratio="15">
    <template #header>
      <DefaultSheetHeader title="회원가입" :icon="AuthIcon" />
    </template>

    <form
      class="signup-box w-100 d-flex flex-column gap-4"
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

      <p v-if="errorMessage" class="mb-0 text-danger">
        {{ errorMessage }}
      </p>

      <p v-if="successMessage" class="mb-0 text-success">
        {{ successMessage }}
      </p>

      <BaseButton type="submit"> 계정 생성 </BaseButton>
    </form>
  </BottomSheetLayout>
</template>

<style scoped>
.signup-box {
  padding: 20px;
}
</style>
