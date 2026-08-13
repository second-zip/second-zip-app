import { shallowMount } from '@vue/test-utils';
import { ref } from 'vue';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import ChecklistListContent from '@/components/checklist/ChecklistListContent.vue';
import MemberSecretaryGuide from '@/components/common/secretary/MemberSecretaryGuide.vue';
import ChecklistListView from './ChecklistListView.vue';

const mocks = vi.hoisted(() => ({
  createChecklist: vi.fn(), fetchChecklists: vi.fn(), push: vi.fn(),
}));
const state = {
  checklists: ref([{ analysisReportId: 1 }]),
  creatingReportIds: ref([]),
  isLoading: ref(false),
  errorMessage: ref(''),
  creationErrorMessage: ref(''),
  fetchChecklists: mocks.fetchChecklists,
  createChecklist: mocks.createChecklist,
};

vi.mock('vue-router', () => ({ useRouter: () => ({ push: mocks.push }) }));
vi.mock('@/composables/checklist/useChecklistList', () => ({
  useChecklistList: () => state,
}));

const BottomSheetStub = {
  template: '<main><slot name="header" /><slot /></main>',
};
const mountView = () => shallowMount(ChecklistListView, {
  global: { stubs: { BottomSheetLayout: BottomSheetStub } },
});

describe('ChecklistListView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.createChecklist.mockResolvedValue({ reportChecklistId: 21 });
  });

  test('진입 시 목록을 조회하고 상태를 목록 컴포넌트에 전달한다', () => {
    const wrapper = mountView();
    const content = wrapper.findComponent(ChecklistListContent);

    expect(mocks.fetchChecklists).toHaveBeenCalledOnce();
    expect(content.props('checklists')).toEqual(state.checklists.value);
    expect(wrapper.findComponent(MemberSecretaryGuide).props('floating'))
      .toBe(true);
  });

  test('생성 성공 시 생성된 체크리스트 상세로 이동한다', async () => {
    const wrapper = mountView();
    const report = { analysisReportId: 1 };
    wrapper.findComponent(ChecklistListContent).vm.$emit('create', report);
    await vi.waitFor(() => expect(mocks.push).toHaveBeenCalled());

    expect(mocks.createChecklist).toHaveBeenCalledWith(report);
    expect(mocks.push).toHaveBeenCalledWith({
      name: 'checklist-detail', params: { reportChecklistId: 21 },
    });
  });

  test('생성 실패 또는 ID 누락 시 화면을 이동하지 않는다', async () => {
    mocks.createChecklist.mockResolvedValue(false);
    const wrapper = mountView();
    wrapper.findComponent(ChecklistListContent).vm.$emit('create', {
      analysisReportId: 1,
    });
    await wrapper.vm.$nextTick();

    expect(mocks.push).not.toHaveBeenCalled();
  });
});
