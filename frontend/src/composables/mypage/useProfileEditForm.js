import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getApiError } from '@/api/utils/error';
import { useAuthStore } from '@/stores/auth';
import { isValidNickname, isValidPassword } from '@/utils/mypage';

export const useProfileEditForm = () => {
  const authStore = useAuthStore();
  const router = useRouter();
  const nickname = ref('');
  const passwords = reactive({ currentPassword: '', newPassword: '' });
  const submitting = ref(false);
  const formMessage = ref('');
  const email = computed(() => authStore.myPage?.email ?? '');

  onMounted(async () => {
    const account = authStore.myPage ?? await authStore.fetchMyPage();
    nickname.value = account?.nickname ?? '';
  });

  const validate = () => {
    if (!isValidNickname(nickname.value)) return '닉네임은 2~20자로 입력해 주세요.';
    if (!passwords.currentPassword) return '현재 비밀번호를 입력해 주세요.';
    if (!isValidPassword(passwords.newPassword)) {
      return '변경할 비밀번호는 영문, 숫자, 특수문자를 포함해 8자 이상 입력해 주세요.';
    }
    return '';
  };

  const saveAccount = async () => {
    formMessage.value = validate();
    if (formMessage.value) return;

    submitting.value = true;
    try {
      await authStore.updateProfile(nickname.value.trim());

      // 비밀번호 변경 API는 성공 즉시 모든 인증 토큰을 무효화하므로 마지막에 호출합니다.
      await authStore.changePassword({
        currentPassword: passwords.currentPassword,
        newPassword: passwords.newPassword,
        newPasswordConfirm: passwords.newPassword,
      });
      await router.replace({ name: 'login', query: { accountUpdated: 'true' } });
    } catch (error) {
      formMessage.value = getApiError(error).message;
    } finally {
      submitting.value = false;
    }
  };

  return { email, formMessage, nickname, passwords, saveAccount, submitting };
};
