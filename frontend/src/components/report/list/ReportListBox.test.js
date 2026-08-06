import { mount } from '@vue/test-utils';
import { describe, expect, test } from 'vitest';

import ReportListBox from './ReportListBox.vue';

const report = { analysisReportId: 1 };

describe('ReportListBox', () => {
  test.each([
    [{ isLoading: true }, '리포트를 불러오는 중입니다.'],
    [{ errorMessage: '조회 실패' }, '조회 실패'],
    [{}, '생성된 리포트가 없습니다.'],
  ])('목록 피드백 상태를 표시한다', (props, message) => {
    const wrapper = mount(ReportListBox, { props });
    expect(wrapper.text()).toContain(message);
  });

  test('개수와 항목을 렌더링하고 이벤트를 전달한다', async () => {
    const wrapper = mount(ReportListBox, {
      props: { reports: [report] },
      global: {
        stubs: {
          ReportListItem: {
            props: ['report'],
            emits: ['toggle-favorite', 'delete'],
            template:
              '<div><button class="favorite" @click="$emit(\'toggle-favorite\', report)"></button><button class="delete" @click="$emit(\'delete\', report)"></button></div>',
          },
        },
      },
    });

    expect(wrapper.text()).toContain('1건');
    await wrapper.get('.favorite').trigger('click');
    await wrapper.get('.delete').trigger('click');
    expect(wrapper.emitted('toggle-favorite')[0]).toEqual([report]);
    expect(wrapper.emitted('delete')[0]).toEqual([report]);
  });
});
