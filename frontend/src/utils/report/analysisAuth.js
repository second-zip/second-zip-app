import { AUTH_SELECTION_ACTIONS } from '@/constants/report/analysisFlow';
import {
  createAnalysisFlowError,
  requireResponseValue,
} from '@/utils/report/analysisFlow';

const normalizeSelectionText = (value) =>
  String(value ?? '')
    .replace(/\s+/g, '')
    .toLowerCase();

const getDetailAddressPart = (detailAddress, suffix) => {
  const match = detailAddress.match(new RegExp(`([^\\s]+)\\s*${suffix}`));
  return match?.[1] ?? '';
};

const getSelectionTarget = (nextAction, analysisRequest) => {
  if (nextAction === 'ADDRESS_SELECTION') return analysisRequest.roadAddress;
  if (nextAction === 'DONG_SELECTION') {
    return getDetailAddressPart(analysisRequest.detailAddress, '동');
  }
  if (nextAction === 'HO_SELECTION') {
    return getDetailAddressPart(analysisRequest.detailAddress, '호');
  }
  return '';
};

const optionMatchesTarget = (option, target, isAddress) => {
  const value = normalizeSelectionText(option.value);
  const label = normalizeSelectionText(option.label);

  if (isAddress) {
    return [value, label]
      .filter(Boolean)
      .some(
        (candidate) => target.includes(candidate) || candidate.includes(target),
      );
  }

  return [value, label]
    .map((candidate) => candidate.replace(/[동호]$/, ''))
    .includes(target);
};

const getSelectionValue = (authResponse, analysisRequest) => {
  const options = Array.isArray(authResponse?.selectionOptions)
    ? authResponse.selectionOptions.filter((option) =>
        String(option?.value ?? '').trim(),
      )
    : [];

  if (options.length === 0) {
    throw createAnalysisFlowError('추가 인증 선택값을 확인하지 못했습니다.');
  }

  const target = normalizeSelectionText(
    getSelectionTarget(authResponse.nextAction, analysisRequest),
  );
  const matched = options.find((option) =>
    optionMatchesTarget(
      option,
      target,
      authResponse.nextAction === 'ADDRESS_SELECTION',
    ),
  );

  return requireResponseValue(
    (matched ?? options[0]).value,
    '추가 인증 선택값을 확인하지 못했습니다.',
  );
};

export const getContinuePayload = (
  authResponse,
  authPayload,
  analysisRequest,
) => {
  const payload = { authentication: authPayload };

  if (AUTH_SELECTION_ACTIONS.has(authResponse?.nextAction)) {
    payload.selectionValue = getSelectionValue(authResponse, analysisRequest);
    return payload;
  }
  if (authResponse?.nextAction === 'CAPTCHA') {
    throw createAnalysisFlowError(
      '보안문자 입력이 필요한 인증은 현재 화면에서 진행할 수 없습니다.',
    );
  }
  if (authResponse?.nextAction !== 'SIMPLE_AUTH') {
    throw createAnalysisFlowError('추가 인증 상태를 확인하지 못했습니다.');
  }

  return payload;
};

export const isRecoverableAuthError = (error) => {
  const status = error?.response?.status;
  return (
    error?.code === 'ECONNABORTED' ||
    !error?.response ||
    [409, 502, 503, 504].includes(status)
  );
};
