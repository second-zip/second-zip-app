import { flushPromises, mount } from '@vue/test-utils';
import { reactive } from 'vue';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import ReportDeleteModal from '@/components/report/list/ReportDeleteModal.vue';
import ReportListView from './ReportListView.vue';

const mocks = vi.hoisted(() => ({
  addReportFavorite: vi.fn(),
  authStore: null,
  deleteReport: vi.fn(),
  deleteReportFavorite: vi.fn(),
  getReports: vi.fn(),
  loggerError: vi.fn(),
  modalDispose: vi.fn(),
  modalHide: vi.fn(),
  modalShow: vi.fn(),
}));

vi.mock('@/api/report', () => ({
  addReportFavorite: mocks.addReportFavorite,
  deleteReport: mocks.deleteReport,
  deleteReportFavorite: mocks.deleteReportFavorite,
  getReports: mocks.getReports,
}));
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => mocks.authStore,
}));
vi.mock('@/utils/logger', () => ({
  logger: { error: mocks.loggerError },
}));
vi.mock('bootstrap', () => ({
  Modal: class {
    show() {
      mocks.modalShow();
    }

    hide() {
      mocks.modalHide();
    }

    dispose() {
      mocks.modalDispose();
    }
  },
}));

const reports = [
  {
    analysisReportId: 1,
    favorite: false,
    createdAt: '2026-08-01T10:00:00',
  },
  {
    analysisReportId: 2,
    favorite: true,
    favoritedAt: '2026-08-02T10:00:00',
    createdAt: '2026-07-01T10:00:00',
  },
  {
    analysisReportId: 3,
    favorite: true,
    favoritedAt: '2026-08-03T10:00:00',
    createdAt: '2026-06-01T10:00:00',
  },
];

const ReportListBoxStub = {
  name: 'ReportListBox',
  props: ['reports', 'isLoading', 'errorMessage'],
  emits: ['toggle-favorite', 'delete'],
  template: '<div class="report-list-box-stub"></div>',
};

const mountView = () =>
  mount(ReportListView, {
    global: {
      stubs: {
        ReportCreateBox: true,
        ReportListBox: ReportListBoxStub,
        SecretaryGuide: true,
      },
    },
  });

describe('ReportListView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authStore = reactive({
      isAuthenticated: false,
      loading: false,
      myPage: null,
      fetchMyPage: vi.fn(),
    });
    mocks.getReports.mockResolvedValue({ reports });
    mocks.addReportFavorite.mockResolvedValue(undefined);
    mocks.deleteReportFavorite.mockResolvedValue(undefined);
    mocks.deleteReport.mockResolvedValue(undefined);
  });

  test('즐겨찾기와 즐겨찾기 시각 순서로 목록을 정렬한다', async () => {
    const wrapper = mountView();

    await flushPromises();

    const list = wrapper.getComponent(ReportListBoxStub);
    expect(list.props('reports').map(({ analysisReportId }) => analysisReportId))
      .toEqual([3, 2, 1]);

    list.vm.$emit('toggle-favorite', list.props('reports')[2]);
    await flushPromises();

    expect(mocks.addReportFavorite).toHaveBeenCalledWith(1);
    expect(list.props('reports')[0].analysisReportId).toBe(1);

    wrapper.unmount();
  });

  test('모달 확인 전에는 삭제하지 않고 확인 후 목록에서 제거한다', async () => {
    const wrapper = mountView();

    await flushPromises();

    const list = wrapper.getComponent(ReportListBoxStub);
    const target = list
      .props('reports')
      .find(({ analysisReportId }) => analysisReportId === 2);

    list.vm.$emit('delete', target);
    await wrapper.vm.$nextTick();

    expect(mocks.modalShow).toHaveBeenCalledOnce();
    expect(mocks.deleteReport).not.toHaveBeenCalled();

    await wrapper.get('.btn-danger').trigger('click');
    await flushPromises();

    expect(mocks.deleteReport).toHaveBeenCalledWith(2);
    expect(mocks.modalHide).toHaveBeenCalledOnce();
    expect(
      list.props('reports').some(({ analysisReportId }) => analysisReportId === 2),
    ).toBe(false);

    wrapper.unmount();
  });

  test('삭제 실패 시 모달과 목록을 유지하고 오류를 표시한다', async () => {
    mocks.deleteReport.mockRejectedValueOnce({
      response: { data: { message: '삭제 실패' } },
    });
    const wrapper = mountView();
    await flushPromises();
    const list = wrapper.getComponent(ReportListBoxStub);

    list.vm.$emit('delete', list.props('reports')[0]);
    await wrapper.get('.btn-danger').trigger('click');
    await flushPromises();

    expect(wrapper.get('[role="alert"]').text()).toBe('삭제 실패');
    expect(mocks.modalHide).not.toHaveBeenCalled();
    expect(list.props('reports')).toHaveLength(3);

    wrapper.getComponent(ReportDeleteModal).vm.$emit('close');
    await wrapper.vm.$nextTick();
    expect(wrapper.getComponent(ReportDeleteModal).props('report')).toBeNull();
  });

  test('로그인 사용자 정보를 조회하고 실패를 기록한다', async () => {
    mocks.authStore.isAuthenticated = true;
    mocks.authStore.fetchMyPage.mockRejectedValueOnce(new Error('fail'));

    const wrapper = mountView();
    await flushPromises();

    expect(mocks.authStore.fetchMyPage).toHaveBeenCalledOnce();
    expect(mocks.loggerError).toHaveBeenCalledWith(
      'member-secretary.fetch-user',
      expect.any(Error),
    );
    wrapper.unmount();
  });
});
