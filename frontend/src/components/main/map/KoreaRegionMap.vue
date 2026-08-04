<script setup>
import { reactive, toRef } from 'vue';

import { MAP_HEIGHT, MAP_WIDTH } from '@/constants/map/regionMap';
import { useKoreaRegionMap } from '@/composables/map/useKoreaRegionMap';
import { getSourceRegionCode } from '@/utils/map/regionMap';

const props = defineProps({
  dataType: {
    type: String,
    required: true,
  },
});

const map = reactive(useKoreaRegionMap(toRef(props, 'dataType')));
</script>

<template>
  <div class="region-map-wrap">
    <nav
      class="region-map__breadcrumb d-flex align-items-center gap-2"
      aria-label="지도 탐색 경로"
    >
      <span v-if="map.currentLevel === 'sido'" aria-current="page">전국</span>
      <button v-else type="button" class="region-map__back" @click="map.resetToSido">
        전국
      </button>
      <template v-if="map.currentLevel !== 'sido'">
        <span aria-hidden="true">&gt;</span>
        <button
          v-if="map.currentLevel === 'district'"
          type="button"
          class="region-map__back"
          @click="map.returnToSigungu"
        >
          {{ map.selectedSidoDisplayName }}
        </button>
        <span v-else aria-current="page">{{ map.selectedSidoDisplayName }}</span>
      </template>
      <template v-if="map.currentLevel === 'district'">
        <span aria-hidden="true">&gt;</span>
        <span aria-current="page">{{ map.selectedCityName }}</span>
      </template>
    </nav>

    <p v-if="map.errorMessage" class="region-map__error" role="alert">
      {{ map.errorMessage }}
    </p>
    <p v-if="map.isCurrentMetricLoading" class="region-map__status">
      지도 데이터를 불러오는 중입니다.
    </p>
    <p v-else-if="map.currentMetricError" class="region-map__status region-map__status--error" role="alert">
      {{ map.currentMetricError }}
    </p>
    <svg
      v-else
      class="region-map"
      :viewBox="`0 0 ${MAP_WIDTH} ${MAP_HEIGHT}`"
      role="img"
      :aria-label="
        map.currentLevel === 'sido'
          ? '대한민국 시도 지도'
          : map.currentLevel === 'district'
            ? `${map.selectedCityName} 구 지도`
            : `${map.selectedSidoName} 시군구 지도`
      "
    >
      <g>
        <g v-for="item in map.currentRenderItems" :key="item.key">
          <g
            v-for="feature in item.features"
            :key="getSourceRegionCode(feature)"
            :transform="map.getFeatureTransform(feature)"
          >
            <path
              :d="map.getPath(feature)"
              class="region-map__area"
              :style="{ fill: item.fill }"
              :class="{
                'region-map__area--active':
                  map.hoveredRegionCode === item.key,
              }"
              role="button"
              tabindex="0"
              :aria-label="map.getRegionAriaLabel(item, feature)"
              @click="map.handleRegionSelect(item, feature)"
              @mouseenter="map.setHoveredRegion(item)"
              @mouseleave="map.clearHoveredRegion"
              @focus="map.setHoveredRegion(item)"
              @blur="map.clearHoveredRegion"
              @keydown.enter.prevent="map.handleRegionSelect(item, feature)"
              @keydown.space.prevent="map.handleRegionSelect(item, feature)"
            />
          </g>
        </g>
      </g>
      <g aria-hidden="true">
        <template
          v-for="item in map.currentRenderItems"
          :key="`label-${item.key}`"
        >
          <text
            v-if="item.showLabel && item.labelPosition"
            class="region-map__label"
            :x="item.labelPosition.x"
            :y="item.labelPosition.y"
          >
            <tspan
              :x="item.labelPosition.x"
              dy="-0.25em"
              class="region-map__label-name"
            >
              {{ item.displayName }}
            </tspan>
            <tspan
              :x="item.labelPosition.x"
              dy="1.2em"
              class="region-map__label-value"
            >
              {{ item.displayValue }}
            </tspan>
          </text>
        </template>
      </g>
    </svg>
    <p
      v-if="map.hasInsetFeature && !map.errorMessage"
      class="region-map__notice"
    >
      ※ 일부 도서 지역은 가독성을 위해 위치를 조정했습니다.
    </p>
  </div>
</template>

<style scoped src="@/assets/styles/region-map.css"></style>
