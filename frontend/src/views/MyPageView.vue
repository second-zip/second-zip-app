<script setup>
import { useRouter } from 'vue-router';

import { getApiError } from '@/api/utils/error';
import ActivitySummary from '@/components/mypage/ActivitySummary.vue';
import MyPageHeader from '@/components/mypage/MyPageHeader.vue';
import ProfileCard from '@/components/mypage/ProfileCard.vue';
import { useMyPageDashboard } from '@/composables/mypage/useMyPageDashboard';
import { MYPAGE_ACTIONS, MYPAGE_ROUTES } from '@/constants/mypage';
import { useAuthStore } from '@/stores/auth';
import ArrowIcon from '@/assets/icons/mypage/arrow-gray-14.svg';

const authStore = useAuthStore();
const router = useRouter();
const {
  account,
  activity,
  activityLoading,
  errorMessage,
  secretaryImage,
  secretaryLabel,
} = useMyPageDashboard();

const handleLogout = async () => {
  if (!authStore.isAuthenticated) {
    await router.replace({ name: 'login' });
    return;
  }
  try {
    await authStore.logout();
    await router.replace({ name: 'login' });
  } catch (error) {
    errorMessage.value = getApiError(error).message;
  }
};
</script>

<template>
  <div class="mypage">
    <MyPageHeader title="마이페이지" />
    <div class="mypage__sheet">
      <ProfileCard :email="account.email" />
      <div class="mypage__divider" />
      <div class="mypage__content">
        <p v-if="errorMessage" class="mypage__error" role="alert">
          {{ errorMessage }}
        </p>
        <ActivitySummary
          :summary="activity"
          :loading="activityLoading"
          :secretary-label="secretaryLabel"
          :secretary-image="secretaryImage"
        />
        <div class="mypage__actions d-grid">
          <RouterLink
            v-for="action in MYPAGE_ACTIONS"
            :key="action.routeName"
            :to="{ name: action.routeName }"
            class="mypage__action d-flex align-items-center justify-content-center gap-2 text-decoration-none"
          >
            <img :src="action.icon" alt="" />{{ action.label }}
          </RouterLink>
        </div>
        <nav class="mypage__menu" aria-label="계정 관리">
          <h2>계정 관리</h2>
          <button type="button" @click="handleLogout">
            로그아웃 <img :src="ArrowIcon" alt="" />
          </button>
          <RouterLink :to="{ name: MYPAGE_ROUTES.withdraw }"
            >회원 탈퇴 <img :src="ArrowIcon" alt=""
          /></RouterLink>
        </nav>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mypage {
  min-height: 100%;
  background: #fff;
  color: #111827;
}
.mypage__sheet {
  overflow: hidden;
  border-radius: 28px 28px 0 0;
  background: #fff;
}
.mypage__divider {
  height: 1px;
  margin: 0 20px;
  background: #d9dde5;
}
.mypage__content {
  padding: 20px;
}
.mypage__error {
  color: #e53838;
  font-size: 13px;
}
.mypage__actions {
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin: 20px 0 24px;
}
.mypage__action {
  min-height: 48px;
  color: #111827;
  border: 1px solid #72a9ff;
  border-radius: 12px;
  box-shadow: 0 2px 7px rgba(31, 97, 204, 0.1);
  font-size: 12px;
  font-weight: 600;
}
.mypage__action img {
  width: 18px;
  height: 18px;
}
.mypage__menu h2 {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
}
.mypage__menu button,
.mypage__menu a {
  display: flex;
  width: 100%;
  height: 42px;
  align-items: center;
  justify-content: space-between;
  padding: 0;
  color: #111827;
  border: 0;
  border-bottom: 1px solid #e5e7eb;
  background: transparent;
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
}
.mypage__menu img {
  width: 14px;
  height: 14px;
}
</style>
