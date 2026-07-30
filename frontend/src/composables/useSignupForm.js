import { reactive } from 'vue';

import { AUTH_MESSAGE } from '@/constants/auth/authMessage';
import {
  isValidNickname,
  isValidEmail,
  isValidPassword,
  isPasswordConfirmed,
} from '@/utils/authValidator';

export const useSignupForm = () => {
  const form = reactive({
    characterType: 'CAT',
    email: '',
    password: '',
    passwordConfirm: '',
    nickname: '',
    termConsents: [
      {
        agreed: true,
        termId: 1,
      },
      {
        agreed: true,
        termId: 2,
      },
    ],
  });

  const started = reactive({
    nickname: false,
    email: false,
    password: false,
    passwordConfirm: false,
  });

  const validationConfig = {
    nickname: {
      validator: () => isValidNickname(form.nickname),
      message: AUTH_MESSAGE.NICKNAME,
    },

    email: {
      validator: () => isValidEmail(form.email),
      message: {
        default: AUTH_MESSAGE.DEF_EMAIL,
        correct: AUTH_MESSAGE.COR_EMAIL,
        wrong: AUTH_MESSAGE.WRO_EMAIL,
      },
    },

    password: {
      validator: () => isValidPassword(form.password),
      message: AUTH_MESSAGE.COND_PW,
    },

    passwordConfirm: {
      validator: () => isPasswordConfirmed(form.password, form.passwordConfirm),
      message: {
        default: AUTH_MESSAGE.CONF_PW,
        correct: AUTH_MESSAGE.SAME_PW,
        wrong: AUTH_MESSAGE.DIFF_PW,
      },
    },
  };

  const getStatus = (field) => {
    const config = validationConfig[field];

    if (!config || !started[field]) {
      return 'default';
    }

    return config.validator() ? 'correct' : 'wrong';
  };

  const getMessage = (field) => {
    const config = validationConfig[field];

    if (!config) {
      return '';
    }

    if (typeof config.message === 'string') {
      return config.message;
    }

    return config.message[getStatus(field)] ?? '';
  };

  const handleFieldInput = (field, value) => {
    if (!started[field] && value.length > 0) {
      started[field] = true;
    }
  };

  const startAllFields = () => {
    Object.keys(started).forEach((field) => {
      started[field] = true;
    });
  };

  const isFormValid = () => {
    return Object.values(validationConfig).every(({ validator }) =>
      validator(),
    );
  };

  return {
    form,
    getStatus,
    getMessage,
    handleFieldInput,
    startAllFields,
    isFormValid,
  };
};
