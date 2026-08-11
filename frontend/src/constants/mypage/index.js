import ProfileIcon from '@/assets/icons/mypage/info-blue-18-1.svg';
import SecretaryIcon from '@/assets/icons/mypage/secretary-blue-18.svg';

export const MYPAGE_ROUTES = Object.freeze({
  profile: 'mypage-profile',
  secretary: 'mypage-secretary',
  withdraw: 'mypage-withdraw',
});

export const MYPAGE_ACTIONS = Object.freeze([
  { label: '회원정보 변경', routeName: MYPAGE_ROUTES.profile, icon: ProfileIcon },
  { label: 'AI 비서 변경하기', routeName: MYPAGE_ROUTES.secretary, icon: SecretaryIcon },
]);

export const MYPAGE_ACTIVITY_ITEMS = Object.freeze([
  { key: 'total', label: '총 리포트', tone: 'total' },
  { key: 'safe', label: '안전', tone: 'safe' },
  { key: 'caution', label: '주의', tone: 'caution' },
  { key: 'danger', label: '위험', tone: 'danger' },
]);

export const PROFILE_EDIT_COPY = Object.freeze({
  title: '회원정보 변경',
  nicknameLabel: '닉네임 변경하기',
  passwordTitle: '비밀번호 변경하기',
  currentPasswordLabel: '현재 비밀번호',
  currentPasswordGuide: '현재 사용하고 계신 비밀번호를 입력해 주세요.',
  newPasswordLabel: '변경할 비밀번호',
  newPasswordGuide: '앞으로 사용하고싶은 비밀번호를 입력해주세요!',
});

export const SECRETARY_OPTIONS = Object.freeze([
  {
    value: 'CAT',
    dictionaryCharacter: 'cat',
    label: '냥냥이',
    message: '걱정됩니다. 냐~옹',
    tone: 'pink',
    image: new URL('../../assets/images/cat.png', import.meta.url).href,
    mainImage: new URL('../../assets/images/cat-main.png', import.meta.url).href,
  },
  {
    value: 'WOMAN',
    dictionaryCharacter: 'woman',
    label: '엘리스',
    message: '제가 해답을 드리겠습니다!',
    tone: 'green',
    image: new URL('../../assets/images/woman.png', import.meta.url).href,
    mainImage: new URL('../../assets/images/woman-main.png', import.meta.url).href,
  },
  {
    value: 'MAN',
    dictionaryCharacter: 'man',
    label: '위장남사친',
    message: '오빠만 믿어!',
    tone: 'purple',
    image: new URL('../../assets/images/man.png', import.meta.url).href,
    mainImage: new URL('../../assets/images/man-main.png', import.meta.url).href,
  },
]);
