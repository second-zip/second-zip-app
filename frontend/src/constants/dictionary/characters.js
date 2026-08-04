import catMain from '@/assets/images/cat-main.png';
import catWord from '@/assets/images/cat-dict-1.png';
import catFraud from '@/assets/images/cat-dict-2.png';
import catRegister from '@/assets/images/cat-dict-3.png';
import catMoveIn from '@/assets/images/cat-dict-4.png';
import manMain from '@/assets/images/man-main.png';
import manWord from '@/assets/images/man-dict-1.png';
import manFraud from '@/assets/images/man-dict-2.png';
import manRegister from '@/assets/images/man-dict-3.png';
import manMoveIn from '@/assets/images/man-dict-4.png';
import womanMain from '@/assets/images/woman-main.png';
import womanWord from '@/assets/images/woman-dict-1.png';
import womanFraud from '@/assets/images/woman-dict-2.png';
import womanRegister from '@/assets/images/woman-dict-3.png';
import womanMoveIn from '@/assets/images/woman-dict-4.png';

// 비로그인 또는 지원하지 않는 설정값에 사용할 기본 캐릭터입니다.
export const DEFAULT_DICTIONARY_CHARACTER = 'cat';

// 캐릭터별 도감 이미지와 화면별 고정 안내 문구를 관리합니다.
export const DICTIONARY_CHARACTERS = Object.freeze({
  cat: {
    guideImage: catMain,
    cardImages: {
      word: catWord,
      fraud: catFraud,
      register: catRegister,
      'move-in': catMoveIn,
    },
    messages: {
      main: '전세사기는 알아야 막는다냥!\n아는 게 힘, 같이 성장해 보자냥?',
      words: '용어 정리다냥.\n꼼꼼히 읽어 보라냥! 이것만 알아도 반은 완성!',
      fraud: '전세사기 유형이다냥.\n글 읽기 싫으면 숏폼 영상으로 보라냥~',
      comic:
        'Ctrl + 마우스 휠로 필요한 부분을 확대하고\n드래그해서 자세히 볼 수 있다냥!',
    },
  },
  man: {
    guideImage: manMain,
    cardImages: {
      word: manWord,
      fraud: manFraud,
      register: manRegister,
      'move-in': manMoveIn,
    },
    messages: {
      main: '전세사기는 알아야 막을 수 있어!\n아는 게 힘, 같이 성장해 보자!',
      words: '용어를 정리해 뒀어.\n꼼꼼히 읽으면 절반은 완성이야!',
      fraud: '전세사기 유형을 확인해 봐.\n숏폼 영상으로도 쉽게 볼 수 있어!',
      comic:
        'Ctrl + 마우스 휠로 필요한 부분을 확대하고\n드래그해서 자세히 볼 수 있어!',
    },
  },
  woman: {
    guideImage: womanMain,
    cardImages: {
      word: womanWord,
      fraud: womanFraud,
      register: womanRegister,
      'move-in': womanMoveIn,
    },
    messages: {
      main: '전세사기는 알아야 예방할 수 있어요.\n함께 차근차근 알아보겠습니다!',
      words: '용어를 정리해 두었습니다.\n꼼꼼히 읽으시면 절반은 완성입니다!',
      fraud:
        '전세사기 유형을 확인해 보세요.\n숏폼 영상으로도 쉽게 보실 수 있습니다!',
      comic:
        'Ctrl + 마우스 휠로 필요한 부분을 확대하고\n드래그해서 자세히 보실 수 있습니다!',
    },
  },
});
