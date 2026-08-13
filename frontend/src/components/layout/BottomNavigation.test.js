import { mount } from '@vue/test-utils';
import { h } from 'vue';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import ChecklistBlueIcon from '@/assets/icons/nav/checklist-blue-22.svg';
import ChecklistGrayIcon from '@/assets/icons/nav/checklist-gray-22.svg';
import BottomNavigation from './BottomNavigation.vue';

const route = { path: '/' };
vi.mock('vue-router', () => ({ useRoute: () => route }));

const RouterLinkStub = {
  props: ['to'],
  setup(props, { slots }) {
    return () => h('div', slots.default({
      href: props.to,
      isActive: route.path === props.to,
      isExactActive: route.path === props.to,
      navigate: vi.fn(),
    }));
  },
};

const findMenu = (wrapper, label) => wrapper
  .findAll('.bottom-nav__item')
  .find((menu) => menu.text() === label);

describe('BottomNavigation', () => {
  beforeEach(() => { route.path = '/'; });

  test.each(['/checklist', '/checklist/25'])(
    '%s에서 체크리스트 메뉴를 활성화한다',
    (path) => {
      route.path = path;
      const wrapper = mount(BottomNavigation, {
        global: { stubs: { RouterLink: RouterLinkStub } },
      });
      const checklist = findMenu(wrapper, '체크리스트');

      expect(checklist.classes()).toContain('is-active');
      expect(checklist.attributes('aria-current')).toBe('page');
      expect(checklist.get('img').attributes('src'))
        .toBe(ChecklistBlueIcon);
    },
  );

  test('다른 화면에서는 체크리스트 메뉴를 비활성화한다', () => {
    route.path = '/report/analysis/3';
    const wrapper = mount(BottomNavigation, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    });
    const checklist = findMenu(wrapper, '체크리스트');

    expect(checklist.classes()).not.toContain('is-active');
    expect(checklist.attributes('aria-current')).toBeUndefined();
    expect(checklist.get('img').attributes('src'))
      .toBe(ChecklistGrayIcon);
  });
});
