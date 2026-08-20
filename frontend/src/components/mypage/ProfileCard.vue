<script setup>
import ProfileImage from '@/assets/images/mypage-profile.png';
defineProps({
  nickname: { type: String, default: '-' },
  email: { type: String, default: '-' },
});
</script>

<template>
  <section class="profile-card d-flex align-items-center gap-3 bg-white">
    <img :src="ProfileImage" class="profile-card__image" alt="프로필" />
    <div class="profile-card__details min-w-0">
      <strong class="profile-card__nickname">{{ nickname }}</strong>
      <div>
        <span class="profile-card__id-label">ID</span>
        <strong class="profile-card__email">{{ email }}</strong>
      </div>
    </div>
  </section>
</template>

<style scoped>
.profile-card {
  min-height: 112px;
  padding: 20px;
  border-radius: 26px 26px 0 0;
}
.profile-card__image {
  width: 58px;
  height: 58px;
  object-fit: contain;
}
.profile-card__details {
  display: flex;
  flex-direction: column;
}
.profile-card__nickname {
  color: #111827;
  font-size: 16px;
  letter-spacing: 0.02em;
}
.profile-card__id-label {
  margin-right: 8px;
  color: #7a8290;
  font-size: 11px;
}
.profile-card__email {
  color: #1670f8;
  font-size: 13px;
  overflow-wrap: anywhere;
  vertical-align: middle;
}
.min-w-0 {
  min-width: 0;
}
</style>
