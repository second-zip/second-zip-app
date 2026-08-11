import axios from 'axios';

const KAKAO_ADDRESS_API_URL =
  'https://dapi.kakao.com/v2/local/search/address.json';

const KAKAO_REST_API_KEY = import.meta.env.VITE_KAKAO_REST_API_KEY;

export const KAKAO_API_KEY_MISSING_ERROR = 'KAKAO_API_KEY_MISSING';

const mapAddressDocument = (document, index) => ({
  id:
    document.address?.address_name ??
    document.road_address?.address_name ??
    `${document.x}-${document.y}-${index}`,
  roadAddress: document.road_address?.address_name ?? '',
  jibunAddress: document.address?.address_name ?? '',
  zoneNo: document.road_address?.zone_no ?? '',
  x: document.x,
  y: document.y,
  raw: document,
});

export const searchKakaoAddress = async (
  query,
  { page = 1, size = 30 } = {},
) => {
  const keyword = query.trim();

  if (!keyword) {
    return [];
  }

  if (!KAKAO_REST_API_KEY?.trim()) {
    const error = new Error('Kakao REST API key is not configured.');
    error.code = KAKAO_API_KEY_MISSING_ERROR;
    throw error;
  }

  const response = await axios.get(KAKAO_ADDRESS_API_URL, {
    params: {
      query: keyword,
      page,
      size,
    },
    headers: {
      Authorization: `KakaoAK ${KAKAO_REST_API_KEY}`,
    },
  });

  const documents = Array.isArray(response.data?.documents)
    ? response.data.documents
    : [];

  return documents.map(mapAddressDocument);
};
