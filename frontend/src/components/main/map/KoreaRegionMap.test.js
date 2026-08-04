import { mount } from '@vue/test-utils';
import { nextTick, reactive } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import KoreaRegionMap from './KoreaRegionMap.vue';

const mocks = vi.hoisted(() => ({ map: null }));

vi.mock('@/composables/map/useKoreaRegionMap', () => ({
  useKoreaRegionMap: () => mocks.map,
}));

const feature = {
  properties: {
    name: '서울특별시',
    sourceRegionCode: '11',
  },
};

const createMap = () =>
  reactive({
    currentLevel: 'sido',
    selectedSidoDisplayName: '',
    selectedSidoName: '',
    selectedCityName: '',
    hoveredRegionCode: '',
    errorMessage: '',
    isCurrentMetricLoading: false,
    currentMetricError: '',
    hasInsetFeature: false,
    currentRenderItems: [
      {
        type: 'region',
        key: 'region:sido:11',
        name: '서울특별시',
        features: [feature],
        fill: '#18aa68',
        displayName: '서울',
        displayValue: '7건',
        showLabel: true,
        labelPosition: { x: 100, y: 80 },
      },
    ],
    getPath: vi.fn(() => 'M0,0L1,1Z'),
    getFeatureTransform: vi.fn(() => undefined),
    getRegionAriaLabel: vi.fn(() => '서울특별시 지도 선택'),
    handleRegionSelect: vi.fn(),
    setHoveredRegion: vi.fn((item) => {
      mocks.map.hoveredRegionCode = item.key;
    }),
    clearHoveredRegion: vi.fn(() => {
      mocks.map.hoveredRegionCode = '';
    }),
    resetToSido: vi.fn(),
    returnToSigungu: vi.fn(),
  });

describe('KoreaRegionMap', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.map = createMap();
  });

  it('시도 SVG path와 두 줄 라벨을 렌더링한다', () => {
    const wrapper = mount(KoreaRegionMap, {
      props: { dataType: 'fraud-damage' },
    });
    const svg = wrapper.get('svg');
    const path = wrapper.get('path');
    const tspans = wrapper.findAll('tspan');

    expect(svg.attributes('viewBox')).toBe('0 0 360 300');
    expect(svg.attributes('aria-label')).toBe('대한민국 시도 지도');
    expect(path.attributes('d')).toBe('M0,0L1,1Z');
    expect(path.attributes('style')).toContain('fill: #18aa68');
    expect(path.attributes('aria-label')).toBe('서울특별시 지도 선택');
    expect(tspans.map((item) => item.text())).toEqual(['서울', '7건']);
  });

  it('click, Enter, Space로 같은 지역 선택 함수를 호출한다', async () => {
    const wrapper = mount(KoreaRegionMap, {
      props: { dataType: 'fraud-damage' },
    });
    const path = wrapper.get('path');

    await path.trigger('click');
    await path.trigger('keydown', { key: 'Enter' });
    await path.trigger('keydown', { key: ' ' });

    expect(mocks.map.handleRegionSelect).toHaveBeenCalledTimes(3);
    expect(mocks.map.handleRegionSelect).toHaveBeenLastCalledWith(
      mocks.map.currentRenderItems[0],
      feature,
    );
  });

  it('hover와 focus 시 active 상태를 설정하고 leave와 blur에서 해제한다', async () => {
    const wrapper = mount(KoreaRegionMap, {
      props: { dataType: 'fraud-damage' },
    });
    const path = wrapper.get('path');

    await path.trigger('mouseenter');
    expect(path.classes()).toContain('region-map__area--active');
    await path.trigger('mouseleave');
    expect(path.classes()).not.toContain('region-map__area--active');
    await path.trigger('focus');
    expect(mocks.map.setHoveredRegion).toHaveBeenCalledTimes(2);
    await path.trigger('blur');
    expect(mocks.map.clearHoveredRegion).toHaveBeenCalledTimes(2);
  });

  it('시군구 breadcrumb에서 전국 버튼으로 돌아간다', async () => {
    mocks.map.currentLevel = 'sigungu';
    mocks.map.selectedSidoDisplayName = '경기';
    mocks.map.selectedSidoName = '경기도';
    const wrapper = mount(KoreaRegionMap, {
      props: { dataType: 'fraud-damage' },
    });

    expect(wrapper.get('svg').attributes('aria-label')).toBe('경기도 시군구 지도');
    expect(wrapper.get('nav').text().replaceAll(' ', '')).toContain('전국>경기');
    await wrapper.get('.region-map__back').trigger('click');
    expect(mocks.map.resetToSido).toHaveBeenCalledOnce();
  });

  it('district breadcrumb에서 시도 버튼으로 시군구 단계에 돌아간다', async () => {
    mocks.map.currentLevel = 'district';
    mocks.map.selectedSidoDisplayName = '경기';
    mocks.map.selectedCityName = '수원시';
    const wrapper = mount(KoreaRegionMap, {
      props: { dataType: 'fraud-damage' },
    });

    expect(wrapper.get('nav').text().replaceAll(' ', '')).toContain(
      '전국>경기>수원시',
    );
    expect(wrapper.get('svg').attributes('aria-label')).toBe('수원시 구 지도');
    await wrapper.findAll('.region-map__back')[1].trigger('click');
    expect(mocks.map.returnToSigungu).toHaveBeenCalledOnce();
  });

  it('현재 metric 로딩과 오류 상태를 지도 대신 표시한다', async () => {
    mocks.map.isCurrentMetricLoading = true;
    const wrapper = mount(KoreaRegionMap, {
      props: { dataType: 'price-index' },
    });

    expect(wrapper.get('.region-map__status').text()).toBe(
      '지도 데이터를 불러오는 중입니다.',
    );
    expect(wrapper.find('svg').exists()).toBe(false);

    mocks.map.isCurrentMetricLoading = false;
    mocks.map.currentMetricError = '지역 데이터를 불러오지 못했습니다.';
    await nextTick();
    expect(wrapper.get('[role="alert"]').text()).toBe(
      '지역 데이터를 불러오지 못했습니다.',
    );
    expect(wrapper.find('svg').exists()).toBe(false);
  });

  it('GeoJSON 오류와 인셋 지역 안내를 렌더링한다', async () => {
    mocks.map.errorMessage = '지도 데이터를 불러올 수 없습니다.';
    const wrapper = mount(KoreaRegionMap, {
      props: { dataType: 'fraud-damage' },
    });

    expect(wrapper.get('.region-map__error').text()).toBe(
      '지도 데이터를 불러올 수 없습니다.',
    );
    expect(wrapper.find('.region-map__notice').exists()).toBe(false);

    mocks.map.errorMessage = '';
    mocks.map.hasInsetFeature = true;
    await nextTick();
    expect(wrapper.get('.region-map__notice').text()).toContain(
      '일부 도서 지역은 가독성을 위해 위치를 조정했습니다.',
    );
  });
});
