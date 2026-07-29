<script setup>
import { useRoute } from 'vue-router';

import dictGray from '@/assets/icons/nav/dict-gray-22.svg';
import dictBlue from '@/assets/icons/nav/dict-blue-22.svg';

import reportGray from '@/assets/icons/nav/report-gray-22.svg';
import reportBlue from '@/assets/icons/nav/report-blue-22.svg';

import mainGray from '@/assets/icons/nav/main-gray-22.svg';
import mainBlue from '@/assets/icons/nav/main-blue-22.svg';

import checklistGray from '@/assets/icons/nav/checklist-gray-22.svg';
import checklistBlue from '@/assets/icons/nav/checklist-blue-22.svg';

import mypageGray from '@/assets/icons/nav/mypage-gray-22.svg';
import mypageBlue from '@/assets/icons/nav/mypage-blue-22.svg';

const route = useRoute();

const menus = [
  {
    label: '도감',
    to: '/dictionary',
    inactiveIcon: dictGray,
    activeIcon: dictBlue,
  },
  {
    label: '리포트',
    to: '/report',
    inactiveIcon: reportGray,
    activeIcon: reportBlue,
  },
  {
    label: '이번집',
    to: '/',
    exact: true,
    inactiveIcon: mainGray,
    activeIcon: mainBlue,
  },
  {
    label: '체크리스트',
    to: '/checklist',
    inactiveIcon: checklistGray,
    activeIcon: checklistBlue,
  },
  {
    label: 'MY',
    to: '/mypage',
    activePaths: ['/login', '/signup'],
    inactiveIcon: mypageGray,
    activeIcon: mypageBlue,
  },
];

const isMenuActive = (menu, isActive, isExactActive) => {
  if (menu.exact) {
    return isExactActive;
  }

  const isAdditionalPathActive = menu.activePaths?.some((path) => {
    return route.path === path || route.path.startsWith(`${path}/`);
  });

  return isActive || isAdditionalPathActive;
};
</script>

<template>
  <nav
    class="bottom-nav position-fixed bottom-0 start-50 translate-middle-x bg-white border-top"
    aria-label="하단 주요 메뉴"
  >
    <div class="d-flex h-100">
      <RouterLink
        v-for="menu in menus"
        :key="menu.to"
        :to="menu.to"
        custom
        v-slot="{ href, navigate, isActive, isExactActive }"
      >
        <a
          :href="href"
          class="bottom-nav__item col d-flex flex-column align-items-center justify-content-center gap-1 text-decoration-none"
          :class="{
            'is-active': isMenuActive(menu, isActive, isExactActive),
          }"
          :aria-current="
            isMenuActive(menu, isActive, isExactActive) ? 'page' : undefined
          "
          @click="navigate"
        >
          <img
            :src="
              isMenuActive(menu, isActive, isExactActive)
                ? menu.activeIcon
                : menu.inactiveIcon
            "
            class="bottom-nav__icon d-block"
            alt=""
          />

          <span class="bottom-nav__label fw-medium">
            {{ menu.label }}
          </span>
        </a>
      </RouterLink>
    </div>
  </nav>
</template>

<style scoped>
.bottom-nav {
  width: 100%;
  height: 64px;
  z-index: 1030;
  border-color: var(--black-100) !important;
}

.bottom-nav__item {
  flex: 0 0 20%;
  width: 20%;
  min-width: 0;
}

.bottom-nav__icon {
  width: 22px;
  height: 22px;
  object-fit: contain;
}

.bottom-nav__label {
  color: var(--black-300);
  font-size: 12px;
  line-height: 1;
}

.bottom-nav__item.is-active .bottom-nav__label {
  color: var(--blue-900);
}

/* 태블릿·데스크톱에서는 앱 콘텐츠 너비와 동일하게 고정 */
@media (min-width: 768px) {
  .bottom-nav {
    width: 402px;
  }
}
</style>
