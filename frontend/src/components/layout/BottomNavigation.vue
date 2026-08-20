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
import MainLogo from '@/assets/images/main-logo.png';

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
    activePaths: ['/mypage', '/login', '/signup'],
    inactiveIcon: mypageGray,
    activeIcon: mypageBlue,
  },
];

const isMenuActive = (menu, isActive, isExactActive) => {
  if (menu.to === '/report' && route.path.startsWith('/report')) return true;
  if (menu.to === '/checklist' && route.path.startsWith('/checklist')) {
    return true;
  }

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
    class="bottom-nav bg-white"
    aria-label="주요 메뉴"
  >
    <div class="bottom-nav__inner d-flex h-100">
      <RouterLink to="/" class="bottom-nav__logo" aria-label="이번집 홈">
        <img :src="MainLogo" alt="이번집" />
      </RouterLink>

      <div class="bottom-nav__items d-flex">
        <RouterLink
          v-for="menu in menus"
          :key="menu.to"
          :to="menu.to"
          custom
          v-slot="{ href, navigate, isActive, isExactActive }"
        >
          <a
            :href="href"
            class="bottom-nav__item d-flex align-items-center justify-content-center gap-1 text-decoration-none"
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
    </div>
  </nav>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  width: 100%;
  height: 64px;
  z-index: 1030;
  transform: translateX(-50%);
  border-top: 1px solid var(--black-100);
  border-color: var(--black-100) !important;
}

.bottom-nav__logo {
  display: none;
}

.bottom-nav__items {
  width: 100%;
  height: 100%;
}

.bottom-nav__item {
  flex: 0 0 20%;
  width: 20%;
  min-width: 0;
  flex-direction: column;
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

@media (min-width: 768px) {
  .bottom-nav {
    position: sticky;
    top: 0;
    bottom: auto;
    left: auto;
    width: var(--app-sidebar-width);
    height: 100dvh;
    align-self: start;
    transform: none;
    border-top: 0;
    border-right: 1px solid var(--black-100);
  }

  .bottom-nav__inner {
    flex-direction: column;
  }

  .bottom-nav__logo {
    display: flex;
    min-height: 116px;
    align-items: center;
    justify-content: center;
    padding: 24px 18px;
  }

  .bottom-nav__logo img {
    display: block;
    width: 100%;
    max-width: 172px;
    height: auto;
  }

  .bottom-nav__items {
    height: auto;
    flex: 1;
    flex-direction: column;
    gap: 8px;
    padding: 16px 12px;
  }

  .bottom-nav__item {
    width: 100%;
    min-height: 52px;
    flex: 0 0 auto;
    flex-direction: row;
    justify-content: flex-start !important;
    gap: 12px !important;
    padding: 0 18px;
    border-radius: 12px;
  }

  .bottom-nav__item.is-active {
    background-color: var(--blue-100);
  }

  .bottom-nav__label {
    font-size: 14px;
  }
}
</style>
