<script setup>
import { reactive } from 'vue';

import { MAP_HEIGHT, MAP_WIDTH } from '@/constants/map/regionMap';
import { useKoreaRegionMap } from '@/composables/map/useKoreaRegionMap';
import { getFeatureCode } from '@/utils/map/regionMap';

const map = reactive(useKoreaRegionMap());
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
        <g v-for="group in map.currentRenderGroups" :key="group.key">
          <g
            v-for="feature in group.features"
            :key="String(getFeatureCode(feature))"
            :transform="map.getFeatureTransform(feature)"
          >
            <path
              :d="map.getPath(feature)"
              class="region-map__area"
              :class="{
                'region-map__area--selected': map.isSelected(feature),
                'region-map__area--active':
                  map.hoveredRegionCode === group.key,
              }"
              role="button"
              tabindex="0"
              :aria-label="map.getRegionAriaLabel(group, feature)"
              @click="map.handleRegionSelect(group, feature)"
              @mouseenter="map.setHoveredRegion(group)"
              @mouseleave="map.clearHoveredRegion"
              @focus="map.setHoveredRegion(group)"
              @blur="map.clearHoveredRegion"
              @keydown.enter.prevent="map.handleRegionSelect(group, feature)"
              @keydown.space.prevent="map.handleRegionSelect(group, feature)"
            />
          </g>
        </g>
      </g>
      <g aria-hidden="true">
        <template
          v-for="group in map.currentRenderGroups"
          :key="`label-${group.key}`"
        >
          <text
            v-if="
              map.shouldShowGroupLabel(group) &&
              map.getGroupLabelPosition(group)
            "
            class="region-map__label"
            :x="map.getGroupLabelPosition(group).x"
            :y="map.getGroupLabelPosition(group).y"
          >
            {{ map.getGroupDisplayName(group) }}
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
