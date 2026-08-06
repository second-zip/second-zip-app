import ArrowRightIcon from '@/assets/icons/report/arrow-gray-14.svg';
import CautionIcon from '@/assets/icons/report/caution-yellow-22.svg';
import DangerIcon from '@/assets/icons/report/danger-red-22.svg';
import DeleteIcon from '@/assets/icons/report/delete-gray-24.svg';
import DeleteActiveIcon from '@/assets/icons/report/delete-red-24.svg';
import FavoriteIcon from '@/assets/icons/report/favorite-gray-24.svg';
import FavoriteActiveIcon from '@/assets/icons/report/favorite-yellow-24.svg';
import SafeIcon from '@/assets/icons/report/safe-green-22.svg';

export const REPORT_CHARACTER_TYPES = new Set(['CAT', 'MAN', 'WOMAN']);

export const REPORT_LIST_MESSAGE = {
  CAT: `리포트 생성하기 버튼을 누른 뒤,
  주소와 보증금을 입력하면
  얼마나 안전한지 알려 주겠다냥!`,
  MAN: `리포트 생성하기 버튼을 누른 뒤,
  주소와 보증금을 입력하면
  얼마나 안전한지 알려 줄게!`,
  WOMAN: `리포트 생성하기 버튼을 누른 뒤,
  주소와 보증금을 입력하면
  얼마나 안전한지 알려 드리겠습니다.`,
};

export const REPORT_LIST_FEEDBACK = {
  loading: '리포트를 불러오는 중입니다.',
  empty: '생성된 리포트가 없습니다.',
};

export const REPORT_DELETE_MODAL_TEXT = {
  title: '리포트 삭제',
  description: '삭제하시겠습니까?',
  cancel: '취소',
  confirm: '확인',
  pending: '삭제 중...',
};

export const REPORT_STATUS_MAP = {
  SAFE: { icon: SafeIcon, alt: '안전', className: 'safe' },
  CAUTION: { icon: CautionIcon, alt: '주의', className: 'caution' },
  DANGER: { icon: DangerIcon, alt: '위험', className: 'danger' },
};

export const REPORT_STATUS_FALLBACK = {
  icon: SafeIcon,
  alt: '상태 정보 없음',
  className: 'unknown',
};

export const REPORT_ACTION_ICONS = {
  favorite: FavoriteIcon,
  favoriteActive: FavoriteActiveIcon,
  delete: DeleteIcon,
  deleteActive: DeleteActiveIcon,
  detail: ArrowRightIcon,
};

export const REPORT_DETAIL_ROUTE_NAME = 'analysis';
