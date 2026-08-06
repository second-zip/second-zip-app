import { mount } from '@vue/test-utils';
import { describe, expect, test } from 'vitest';

import ReportListStatusIcon from './ReportListStatusIcon.vue';

describe('ReportListStatusIcon', () => {
  test.each([
    ['SAFE', 'safe', '안전'],
    ['CAUTION', 'caution', '주의'],
    ['DANGER', 'danger', '위험'],
    ['UNKNOWN', 'unknown', '상태 정보 없음'],
  ])('%s 상태의 아이콘 정보를 표시한다', (result, className, alt) => {
    const wrapper = mount(ReportListStatusIcon, { props: { result } });

    expect(wrapper.get('.status').classes()).toContain(`status--${className}`);
    expect(wrapper.get('img').attributes('alt')).toBe(alt);
  });
});
