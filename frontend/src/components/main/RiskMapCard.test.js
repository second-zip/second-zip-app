import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import RiskMapCard from './RiskMapCard.vue';

describe('RiskMapCard', () => {
  it('지도 슬롯과 여섯 단계 범례를 렌더링한다', () => {
    const wrapper = mount(RiskMapCard, {
      slots: { default: '<div class="test-map">지도</div>' },
    });
    const legends = wrapper.findAll('.risk-map-card__legend-item');

    expect(wrapper.get('.test-map').text()).toBe('지도');
    expect(legends).toHaveLength(6);
    expect(legends.map((item) => item.text())).toEqual([
      '매우위험',
      '위험',
      '매우주의',
      '주의',
      '안전',
      '매우안전',
    ]);
  });

  it('각 범례에 위험 단계별 색상 클래스를 적용한다', () => {
    const wrapper = mount(RiskMapCard);
    const colors = wrapper.findAll('.risk-map-card__legend-color');

    expect(colors.map((color) => color.classes()[1])).toEqual([
      'risk-map-card__legend-color--very-dangerous',
      'risk-map-card__legend-color--dangerous',
      'risk-map-card__legend-color--very-caution',
      'risk-map-card__legend-color--caution',
      'risk-map-card__legend-color--safe',
      'risk-map-card__legend-color--very-safe',
    ]);
  });
});
