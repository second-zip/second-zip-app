import { shallowMount } from '@vue/test-utils';
import { ref } from 'vue';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import ChecklistRecorder from '@/components/checklist/ChecklistRecorder.vue';
import ChecklistDetailView from './ChecklistDetailView.vue';

const mocks = vi.hoisted(() => ({ fetchChecklist: vi.fn() }));

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { reportChecklistId: '25' } }),
}));
vi.mock('@/composables/checklist/useChecklistDetail', () => ({
  useChecklistDetail: () => ({
    actionErrorMessage: ref(''),
    address: ref('서울시 마포구'),
    completedCount: ref(0),
    fetchChecklist: mocks.fetchChecklist,
    isLoading: ref(false),
    isResetting: ref(false),
    items: ref([]),
    loadErrorMessage: ref(''),
    progress: ref(0),
    resetItems: vi.fn(),
    toggleItem: vi.fn(),
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
});
