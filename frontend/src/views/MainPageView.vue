<script setup>
import { ref } from 'vue';

import MainHero from '@/components/main/MainHero.vue';
import MainDataTabs from '@/components/main/MainDataTabs.vue';
import RiskMapCard from '@/components/main/RiskMapCard.vue';
import KoreaRegionMap from '@/components/main/map/KoreaRegionMap.vue';
import ReportButton from '@/components/main/ReportButton.vue';
import SecretaryGuide from '@/components/common/secretary/SecretaryGuide.vue';

const selectedDataType = ref('fraud-damage');
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

      <ReportButton />
    </div>

    <!-- 하단 남는 영역의 아래쪽 -->
    <div class="main-page__secretary-area d-flex align-items-end">
      <SecretaryGuide
        text="피해 사례를 미리 확인하면 더 안전한 계약이 가능하다냐-옹!"
        change-btn
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
