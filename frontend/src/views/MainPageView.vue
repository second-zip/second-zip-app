<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import MainHero from '@/components/main/MainHero.vue';
import MainDataTabs from '@/components/main/MainDataTabs.vue';
import RiskMapCard from '@/components/main/RiskMapCard.vue';
import KoreaRegionMap from '@/components/main/map/KoreaRegionMap.vue';
import ReportButton from '@/components/main/ReportButton.vue';
import SecretaryGuide from '@/components/common/secretary/SecretaryGuide.vue';
import { useAuthStore } from '@/stores/auth';
import { logger } from '@/utils/logger';

const SECRETARY_MESSAGES = {
  'fraud-damage': {
    CAT: '피해 사례를 미리 확인하면\n더 안전한 계약이 가능하다냐-옹!',
    MAN: '피해 사례를 미리 확인하면\n더 안전하게 계약할 수 있어!',
    WOMAN: '더욱 안전한 계약을 위해\n피해 사례를 확인해 보시길 바랍니다!',
  },
  'price-index': {
    CAT: '가격 변동률이 큰 지역은\n사기 위험도 높아질 수 있다냥…',
    MAN: '가격 변동률이 큰 지역은\n사기 위험도 높을 수 있으니 조심해!',
    WOMAN: '가격 변동률이 큰 지역은\n사기 위험도 높을 수 있으니 조심하세요…',
  },
};
const CHARACTER_TYPES = new Set(['CAT', 'MAN', 'WOMAN']);

const authStore = useAuthStore();
const router = useRouter();
const selectedDataType = ref('fraud-damage');
const isUserLoaded = ref(
  !authStore.isAuthenticated || Boolean(authStore.myPage),
);
const characterType = computed(() => {
  if (!authStore.isAuthenticated) return 'CAT';

  const type = authStore.myPage?.characterType;
  return CHARACTER_TYPES.has(type) ? type : 'CAT';
});
const secretaryMessage = computed(
  () => SECRETARY_MESSAGES[selectedDataType.value][characterType.value],
);

const goToReport = () => router.push('/report');
const goToCharacter = () => {
  const characterSettingsPath = '/mypage#ai-secretary';

  if (!authStore.isAuthenticated) {
    return router.push({
      name: 'login',
      query: { redirect: characterSettingsPath },
    });
  }

  return router.push({ name: 'mypage', hash: '#ai-secretary' });
};

onMounted(async () => {
  if (!authStore.isAuthenticated || authStore.myPage) return;

  try {
    await authStore.fetchMyPage();
  } catch (error) {
    logger.error('main.fetch-user', error);
    // 회원정보 조회에 실패한 동안에는 기본 CAT 캐릭터를 사용한다.
  } finally {
    isUserLoaded.value = true;
  }
});
</script>

<template>
  <section class="main-page w-100">
    <!-- 상단 남는 영역의 정중앙 -->
    <div
      class="main-page__hero-area d-flex align-items-center justify-content-center"
    >
      <MainHero />
    </div>

    <!-- 가운데 고정 콘텐츠 -->
    <div class="main-page__content d-flex flex-column gap-4 mx-auto">
      <MainDataTabs v-model="selectedDataType" />

      <RiskMapCard>
        <KoreaRegionMap :data-type="selectedDataType" />
      </RiskMapCard>

      <ReportButton @click="goToReport" />
    </div>

    <!-- 하단 남는 영역의 아래쪽 -->
    <div class="main-page__secretary-area d-flex align-items-end">
      <SecretaryGuide
        v-if="isUserLoaded"
        :text="secretaryMessage"
        :character-type="characterType"
        change-btn
        @change="goToCharacter"
      />
    </div>
  </section>
</template>

<style scoped>
.main-page {
  min-height: 100%;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto minmax(0, 1fr);
  background-color: #fff;
}

.main-page__hero-area,
.main-page__secretary-area {
  min-width: 0;
  min-height: 0;
}

.main-page__content {
  width: 100%;
  padding: 0 20px;
}

.main-page__secretary-area {
  width: 100%;
}
</style>
