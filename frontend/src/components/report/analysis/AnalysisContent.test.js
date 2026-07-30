import { mount } from '@vue/test-utils';
import { describe, expect, test } from 'vitest';

import AAnalysis from './A_analysis.vue';
import AnalysisContent from './AnalysisContent.vue';
import BAnalysis from './B_analysis.vue';
import CAnalysis from './C_analysis.vue';
import DAnalysis from './D_analysis.vue';
import EAnalysis from './E_analysis.vue';

const icons = {
  safe: '/safe.svg',
  caution: '/caution.svg',
  danger: '/danger.svg',
};
const labels = { safe: '안전', caution: '주의', danger: '위험' };
const checks = [
  {
    id: 'mortgage',
    label: '근저당권',
    status: 'safe',
    basis: '등기부등본',
    amount: '0원',
  },
];
const fraudTypes = [
  {
    id: 'fraud',
    title: '전세사기 유형',
    subtitle: '세부 설명',
    items: [{ label: '세부 항목', status: 'caution' }],
  },
];
const terms = [{ title: '특약', description: '특약 설명' }];

describe('분석 결과 구역', () => {
  test('위험도와 비서 결과를 표시한다', () => {
    const wrapper = mount(AAnalysis, {
      props: {
        risk: 'danger',
        character: '/woman.png',
        icon: icons.danger,
        message: '위험합니다',
      },
    });

    expect(wrapper.classes()).toContain('risk-summary--danger');
    expect(wrapper.text()).toContain('위험합니다');
  });

  test('보증금과 전세가율을 표시한다', () => {
    const wrapper = mount(BAnalysis, {
      props: { deposit: '1억 0만원', ratio: '55%' },
    });

    expect(wrapper.text()).toContain('1억 0만원');
    expect(wrapper.text()).toContain('55%');
  });

  test('필수 점검 상세를 열고 닫는다', async () => {
    const wrapper = mount(CAnalysis, {
      props: { checks, risk: 'safe', icons, labels },
    });
    const button = wrapper.get('.accordion-button');

    expect(button.attributes('aria-expanded')).toBe('false');
    expect(wrapper.get('.accordion-collapse').classes()).not.toContain('show');

    await button.trigger('click');
    expect(button.attributes('aria-expanded')).toBe('true');
    expect(wrapper.get('.accordion-collapse').classes()).toContain('show');
  });

  test('전세사기 세부 항목을 펼친다', async () => {
    const wrapper = mount(DAnalysis, {
      props: { fraudTypes, risk: 'caution', icons, labels },
    });
    const button = wrapper.get('.accordion-button');

    expect(button.attributes('aria-expanded')).toBe('false');
    await button.trigger('click');
    expect(button.attributes('aria-expanded')).toBe('true');
    expect(wrapper.get('.accordion-collapse').classes()).toContain('show');
    expect(wrapper.text()).toContain('세부 항목');
  });

  test('AI 추천 특약을 표시한다', () => {
    const wrapper = mount(EAnalysis, {
      props: {
        terms,
        notice: '법적 효력은 계약 내용에 따릅니다.',
        character: '/cat.png',
      },
    });

    expect(wrapper.text()).toContain('특약 설명');
    expect(wrapper.text()).toContain('법적 효력');
  });

  test('A부터 E까지 순서대로 구성한다', () => {
    const wrapper = mount(AnalysisContent, {
      props: {
        overallRisk: 'safe',
        secretaryImage: '/cat.png',
        overallIcon: icons.safe,
        overallMessage: '안전합니다',
        formattedDeposit: '1억 0만원',
        rentRatioDisplay: '55%',
        checks,
        checkRisk: 'safe',
        fraudTypes,
        fraudRisk: 'caution',
        riskIcons: icons,
        riskLabels: labels,
        specialTerms: terms,
        specialTermsNotice: '안내',
        defaultSecretaryImage: '/cat.png',
      },
    });

    expect(wrapper.findAllComponents(AAnalysis)).toHaveLength(1);
    expect(wrapper.findAllComponents(BAnalysis)).toHaveLength(1);
    expect(wrapper.findAllComponents(CAnalysis)).toHaveLength(1);
    expect(wrapper.findAllComponents(DAnalysis)).toHaveLength(1);
    expect(wrapper.findAllComponents(EAnalysis)).toHaveLength(1);
    const sectionClasses = [
      'risk-summary',
      'price-card',
      'inspection-card',
      'fraud-card',
      'special-terms',
    ];
    const children = [...wrapper.get('.analysis-content').element.children];

    expect(
      children.map((element) =>
        sectionClasses.find((name) => element.classList.contains(name)),
      ),
    ).toEqual(sectionClasses);
  });
});
