import { beforeEach, describe, expect, it, vi } from 'vitest';

import api from './instance';
import { searchAddresses } from './address';

vi.mock('./instance', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('address API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('빈 검색어는 API를 호출하지 않고 빈 배열을 반환한다', async () => {
    await expect(searchAddresses('   ')).resolves.toEqual([]);
    expect(api.get).not.toHaveBeenCalled();
  });

  it('트림한 검색어로 백엔드 주소 API를 호출한다', async () => {
    const addresses = [
      {
        addressId: 'address-id',
        roadAddress: '서울 강남구 테헤란로 1',
        jibunAddress: '서울 강남구 역삼동 1',
        zoneNo: '06234',
        placeName: '',
      },
    ];
    api.get.mockResolvedValue({ data: { addresses } });

    await expect(searchAddresses('  테헤란로  ')).resolves.toBe(addresses);
    expect(api.get).toHaveBeenCalledWith('/addresses', {
      params: { query: '테헤란로' },
    });
  });

  it('addresses가 배열이 아니면 빈 배열을 반환한다', async () => {
    api.get.mockResolvedValue({ data: { addresses: null } });

    await expect(searchAddresses('주소')).resolves.toEqual([]);
  });
});
