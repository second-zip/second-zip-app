import { beforeEach, describe, expect, it, vi } from 'vitest';

import api from './instance';
import { getConsents, getLatestTerms } from './terms';

vi.mock('./instance', () => ({
  default: { get: vi.fn() },
}));

describe('약관 API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('개인정보 동의 상태를 조회한다', async () => {
    const data = { privacyPolicy: true };
    api.get.mockResolvedValue({ data });

    await expect(getConsents()).resolves.toBe(data);
    expect(api.get).toHaveBeenCalledWith('/terms/consents');
  });

  it('최신 약관과 고지를 조회한다', async () => {
    const data = { terms: [], notices: [] };
    api.get.mockResolvedValue({ data });

    await expect(getLatestTerms()).resolves.toBe(data);
    expect(api.get).toHaveBeenCalledWith('/terms/latest');
  });
});
