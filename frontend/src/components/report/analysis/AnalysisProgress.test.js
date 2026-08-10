import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import AnalysisProgress from './AnalysisProgress.vue';

const mountProgress = (props = {}) =>
  mount(AnalysisProgress, {
    props: {
      completedSteps: 2,
      totalSteps: 6,
      progress: 100 / 3,
      currentMessage: '집 정보를 확인하고 있어요.',
      status: 'running',
      ...props,
    },
  });

describe('AnalysisProgress', () => {
  it('현재 메시지와 진행률을 접근성 속성에 반영한다', () => {
    const wrapper = mountProgress();
    const progressbar = wrapper.get('[role="progressbar"]');

    expect(wrapper.text()).toContain('집 정보를 확인하고 있어요.');
    expect(wrapper.text()).toContain('2 / 6');
    expect(progressbar.attributes('aria-valuenow')).toBe('2');
    expect(progressbar.attributes('aria-valuemax')).toBe('6');
    expect(wrapper.get('.analysis-progress__bar').attributes('style')).toContain(
      '33.333',
    );
    expect(wrapper.get('img').attributes('alt')).toBe('집 분석 안내 캐릭터');
  });

  it('실패 상태에서 오류 안내를 alert로 표시한다', () => {
    const wrapper = mountProgress({
      status: 'failed',
      errorMessage: '인증을 완료하지 못했습니다.',
    });

    expect(wrapper.text()).toContain('분석을 완료하지 못했어요.');
    expect(wrapper.get('[role="alert"]').text()).toBe(
      '인증을 완료하지 못했습니다.',
    );
    expect(wrapper.find('[role="status"]').exists()).toBe(false);
  });
});
