import { mount } from '@vue/test-utils';
import { describe, expect, test } from 'vitest';

import PlusIcon from '@/assets/icons/report/plus-white-14.svg';
import ChecklistCreateButton from './ChecklistCreateButton.vue';
import ChecklistDetailHeader from './ChecklistDetailHeader.vue';
import ChecklistProgress from './ChecklistProgress.vue';
import CircularProgress from './CircularProgress.vue';

describe('ChecklistCreateButton', () => {
  test('생성 이벤트를 전달하고 진행 중에는 중복 클릭을 막는다', async () => {
    const wrapper = mount(ChecklistCreateButton);
    await wrapper.get('button').trigger('click');
    expect(wrapper.emitted('create')).toHaveLength(1);
    expect(wrapper.get('img').attributes('src')).toBe(PlusIcon);

    await wrapper.setProps({ isLoading: true });
    expect(wrapper.get('button').attributes('disabled')).toBeDefined();
    expect(wrapper.text()).toBe('생성 중');
    expect(wrapper.find('img').exists()).toBe(false);
  });
});

describe('ChecklistDetailHeader', () => {
  test('주소와 초기화 상태를 표시하고 reset 이벤트를 전달한다', async () => {
    const wrapper = mount(ChecklistDetailHeader, {
      props: { address: '서울시 마포구 101호' },
    });
    expect(wrapper.get('h1').text()).toBe('서울시 마포구 101호');
    await wrapper.get('button').trigger('click');
    expect(wrapper.emitted('reset')).toHaveLength(1);

    await wrapper.setProps({ isResetting: true });
    expect(wrapper.get('button').text()).toBe('초기화 중');
    expect(wrapper.get('button').attributes('disabled')).toBeDefined();
  });
});

describe('ChecklistProgress', () => {
  test('완료 개수와 0~100 범위로 제한한 진행률을 표시한다', async () => {
    const wrapper = mount(ChecklistProgress, {
      props: { completedCount: 2, totalCount: 6, progress: 33.4 },
    });
    expect(wrapper.text()).toContain('2 / 6');
    expect(wrapper.get('[role="progressbar"]').attributes('aria-valuenow'))
      .toBe('33');
    expect(wrapper.get('.checklist-progress__bar').attributes('style'))
      .toContain('33.4%');

    await wrapper.setProps({ progress: 120 });
    expect(wrapper.get('.checklist-progress__bar').attributes('style'))
      .toContain('100%');
  });
});

describe('CircularProgress', () => {
  test('아이콘 크기와 진행 각도를 동적으로 계산한다', async () => {
    const wrapper = mount(CircularProgress, {
      props: { value: 25, size: 20 },
    });
    expect(wrapper.attributes('aria-valuenow')).toBe('25');
    expect(wrapper.attributes('style')).toContain('--progress-angle: 90deg');
    expect(wrapper.attributes('style')).toContain('width: 20px');

    await wrapper.setProps({ value: -20 });
    expect(wrapper.attributes('aria-valuenow')).toBe('0');
  });
});
