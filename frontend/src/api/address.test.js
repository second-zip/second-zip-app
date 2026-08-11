import { afterEach, describe, expect, it, vi } from 'vitest';

const axiosMock = vi.hoisted(() => ({ get: vi.fn() }));

vi.mock('axios', () => ({ default: axiosMock }));

const loadAddressApi = async (apiKey) => {
  vi.resetModules();
  vi.stubEnv('VITE_KAKAO_REST_API_KEY', apiKey);
  return import('./address');
};

describe('Kakao address API', () => {
  afterEach(() => {
    vi.clearAllMocks();
    vi.unstubAllEnvs();
  });

  it('빈 검색어는 API를 호출하지 않고 빈 배열을 반환한다', async () => {
    const { searchKakaoAddress } = await loadAddressApi('TEST_KEY');

    await expect(searchKakaoAddress('   ')).resolves.toEqual([]);
    expect(axiosMock.get).not.toHaveBeenCalled();
  });

  it('API key가 없으면 요청하지 않고 설정 오류를 발생시킨다', async () => {
    const { KAKAO_API_KEY_MISSING_ERROR, searchKakaoAddress } =
      await loadAddressApi('');

    await expect(searchKakaoAddress('테헤란로')).rejects.toMatchObject({
      code: KAKAO_API_KEY_MISSING_ERROR,
    });
    expect(axiosMock.get).not.toHaveBeenCalled();
  });

  it('KakaoAK 헤더와 page·size를 전달하고 documents를 변환한다', async () => {
    const roadDocument = {
      address: { address_name: '서울 강남구 역삼동 1' },
      road_address: {
        address_name: '서울 강남구 테헤란로 1',
        zone_no: '06234',
      },
      x: '127.1',
      y: '37.5',
    };
    axiosMock.get.mockResolvedValue({ data: { documents: [roadDocument] } });
    const { searchKakaoAddress } = await loadAddressApi('TEST_KEY');

    await expect(
      searchKakaoAddress('  테헤란로  ', { page: 2, size: 10 }),
    ).resolves.toEqual([
      {
        id: '서울 강남구 역삼동 1',
        roadAddress: '서울 강남구 테헤란로 1',
        jibunAddress: '서울 강남구 역삼동 1',
        zoneNo: '06234',
        x: '127.1',
        y: '37.5',
        raw: roadDocument,
      },
    ]);
    expect(axiosMock.get).toHaveBeenCalledWith(
      'https://dapi.kakao.com/v2/local/search/address.json',
      {
        params: { query: '테헤란로', page: 2, size: 10 },
        headers: { Authorization: 'KakaoAK TEST_KEY' },
      },
    );
  });

  it('nullable 주소 필드와 비정상 documents에 안전하다', async () => {
    axiosMock.get
      .mockResolvedValueOnce({
        data: { documents: [{ x: '127', y: '37' }] },
      })
      .mockResolvedValueOnce({ data: { documents: null } });
    const { searchKakaoAddress } = await loadAddressApi('TEST_KEY');

    await expect(searchKakaoAddress('주소')).resolves.toMatchObject([
      {
        id: '127-37-0',
        roadAddress: '',
        jibunAddress: '',
        zoneNo: '',
      },
    ]);
    await expect(searchKakaoAddress('주소')).resolves.toEqual([]);
  });
});
