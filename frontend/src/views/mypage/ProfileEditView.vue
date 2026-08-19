<script setup>
import { useProfileEditForm } from '@/composables/mypage/useProfileEditForm';
import { PROFILE_EDIT_COPY } from '@/constants/mypage';
import ProfileImage from '@/assets/images/mypage-profile.png';
import ProfileIcon from '@/assets/icons/mypage/info-blue-18-1.svg';

const { email, formMessage, nickname, passwords, saveAccount, submitting } =
  useProfileEditForm();
</script>

<template>
  <div class="profile-edit-page">
    <main class="profile-edit-page__sheet">
      <div class="profile-edit-page__title d-flex align-items-center justify-content-center gap-2">
        <img :src="ProfileIcon" alt="" />
        <h1>{{ PROFILE_EDIT_COPY.title }}</h1>
      </div>

      <section class="profile-edit-page__account d-flex align-items-center gap-3">
        <img :src="ProfileImage" alt="프로필" />
        <span>ID</span>
        <strong>{{ email }}</strong>
      </section>

      <form class="account-form" @submit.prevent="saveAccount">
      <div class="profile-form">
        <label for="nickname">{{ PROFILE_EDIT_COPY.nicknameLabel }}</label>
        <input id="nickname" v-model="nickname" type="text" maxlength="20" autocomplete="nickname" />
      </div>

      <div class="password-form">
        <h2>{{ PROFILE_EDIT_COPY.passwordTitle }}</h2>
        <label for="current-password">{{ PROFILE_EDIT_COPY.currentPasswordLabel }}</label>
        <input id="current-password" v-model="passwords.currentPassword" type="password" autocomplete="current-password" />
        <p class="input-guide is-green">{{ PROFILE_EDIT_COPY.currentPasswordGuide }}</p>

        <label for="new-password">{{ PROFILE_EDIT_COPY.newPasswordLabel }}</label>
        <input id="new-password" v-model="passwords.newPassword" type="password" autocomplete="new-password" />
        <p class="input-guide is-red">{{ PROFILE_EDIT_COPY.newPasswordGuide }}</p>
      </div>
      <p v-if="formMessage" class="form-message" role="alert">{{ formMessage }}</p>
      <button class="account-form__submit" type="submit" :disabled="submitting">
        {{ submitting ? '변경 중...' : '회원정보 변경하기' }}
      </button>
      </form>
    </main>
  </div>
</template>

<style scoped>
.profile-edit-page { height: 100%; min-height: 0; padding-top: 1px; background: #f2f3f5; color: #16191f; }
.profile-edit-page__sheet { height: 100%; min-height: 0; padding: 45px 20px 24px; overflow-y: auto; border-radius: 30px 30px 0 0; background: #fff; }
.profile-edit-page__title { height: 52px; border: 1.5px solid #75abff; border-radius: 13px; box-shadow: 0 3px 7px rgba(37, 105, 218, .13); }
.profile-edit-page__title img { width: 18px; height: 18px; }.profile-edit-page__title h1 { margin: 0; font-size: 13px; font-weight: 700; }
.profile-edit-page__account { height: 100px; padding: 20px 8px 12px; }.profile-edit-page__account > img { width: 70px; height: 70px; flex: 0 0 70px; }.profile-edit-page__account span { margin-left: 3px; color: #7a8290; font-size: 11px; }.profile-edit-page__account strong { overflow: hidden; color: #066cff; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.profile-form, .password-form { display: grid; }.profile-form label, .password-form h2 { margin: 0 0 10px; font-size: 15px; font-weight: 700; }.password-form { margin-top: 49px; }.password-form label { margin-bottom: 9px; color: #666e7c; font-size: 11px; }
.profile-form input, .password-form input { width: 100%; height: 52px; padding: 0 14px; border: 1.5px solid #dfe4ed; border-radius: 13px; background: #f7f8fa; outline: none; font-size: 14px; }.profile-form input { border-color: #1670ff; background: #f8faff; font-size: 15px; font-weight: 600; }.profile-form input:focus, .password-form input:focus { border-color: #1670ff; box-shadow: 0 0 0 2px rgba(22,112,255,.08); }
.input-guide { margin: 8px 5px 25px; font-size: 10px; }.input-guide.is-green { color: #00b970; }.input-guide.is-red { color: #ff323b; }.form-message { margin: 8px 5px; color: #176cf3; font-size: 10px; }
.account-form__submit { width: 100%; height: 48px; margin-top: 18px; color: #fff; border: 0; border-radius: 12px; background: #176cf3; font-size: 13px; font-weight: 700; box-shadow: 0 4px 10px rgba(23,108,243,.2); }.account-form__submit:disabled { opacity: .55; }

@media (max-height: 680px) {
  .profile-edit-page__sheet { padding-top: 28px; }
  .profile-edit-page__account { height: 88px; padding-top: 13px; }
  .password-form { margin-top: 34px; }
  .input-guide { margin-bottom: 18px; }
}

@media (min-width: 768px) {
  .profile-edit-page__sheet {
    width: min(100%, 720px);
    margin: 0 auto;
    padding: 48px clamp(32px, 6vw, 64px) 40px;
  }
}
</style>
