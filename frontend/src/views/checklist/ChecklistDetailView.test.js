import { shallowMount } from '@vue/test-utils';
import { ref } from 'vue';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import ChecklistDescriptionModal from '@/components/checklist/ChecklistDescriptionModal.vue';
import ChecklistDetailHeader from '@/components/checklist/ChecklistDetailHeader.vue';
import ChecklistDetailList from '@/components/checklist/ChecklistDetailList.vue';
import ChecklistProgress from '@/components/checklist/ChecklistProgress.vue';
import ChecklistRecorder from '@/components/checklist/ChecklistRecorder.vue';
import MemberSecretaryGuide from '@/components/common/secretary/MemberSecretaryGuide.vue';
import ChecklistDetailView from './ChecklistDetailView.vue';

const mocks = vi.hoisted(() => ({
  fetchChecklist: vi.fn(), resetItems: vi.fn(), toggleItem: vi.fn(),
}));
const item = { id: 1, title: '등기부등본', description: '설명' };

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { reportChecklistId: '25' } }),
}));
vi.mock('@/composables/checklist/useChecklistDetail', () => ({
  useChecklistDetail: () => ({
    actionErrorMessage: ref(''),
    address: ref('서울시 마포구 101호'),
    completedCount: ref(1),
    fetchChecklist: mocks.fetchChecklist,
    isLoading: ref(false),
    isResetting: ref(false),
    items: ref([item]),
    loadErrorMessage: ref(''),
    progress: ref(100),
    resetItems: mocks.resetItems,
    toggleItem: mocks.toggleItem,
  }),
}));

describe('ChecklistDetailView', () => {
  beforeEach(() => vi.clearAllMocks());

  test('상세 ID를 녹음기에 전달하고 분석 완료 후 체크리스트를 재조회한다', async () => {
    const wrapper = shallowMount(ChecklistDetailView);
    const recorder = wrapper.findComponent(ChecklistRecorder);

    expect(mocks.fetchChecklist).toHaveBeenCalledOnce();
    expect(recorder.props('reportChecklistId')).toBe(25);

    recorder.vm.$emit('processed');
    await wrapper.vm.$nextTick();
    expect(mocks.fetchChecklist).toHaveBeenCalledTimes(2);
  });

  test('상세 상태와 사용자 동작을 하위 컴포넌트에 연결한다', async () => {
    const wrapper = shallowMount(ChecklistDetailView);
    const header = wrapper.findComponent(ChecklistDetailHeader);
    const progress = wrapper.findComponent(ChecklistProgress);
    const list = wrapper.findComponent(ChecklistDetailList);

    expect(header.props('address')).toBe('서울시 마포구 101호');
    expect(progress.props()).toMatchObject({
      completedCount: 1, totalCount: 1, progress: 100,
    });
    header.vm.$emit('reset');
    list.vm.$emit('toggle', 1);
    await wrapper.vm.$nextTick();
    expect(mocks.resetItems).toHaveBeenCalledOnce();
    expect(mocks.toggleItem).toHaveBeenCalledWith(1);
  });

  test('설명 모달이 열리면 가이드를 숨기고 닫으면 다시 표시한다', async () => {
    const wrapper = shallowMount(ChecklistDetailView);
    wrapper.findComponent(ChecklistDetailList).vm.$emit(
      'show-description', item,
    );
    await wrapper.vm.$nextTick();

    const modal = wrapper.findComponent(ChecklistDescriptionModal);
    expect(modal.props()).toMatchObject({ item, open: true });
    expect(wrapper.findComponent(MemberSecretaryGuide).exists()).toBe(false);

    modal.vm.$emit('close');
    await wrapper.vm.$nextTick();
    expect(wrapper.findComponent(MemberSecretaryGuide).exists()).toBe(true);
  });
});
