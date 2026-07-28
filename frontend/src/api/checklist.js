import api from './instance';

// API 명세서 기준: 체크리스트 조회
export const getChecklist = async () => {
  const response = await api.get('/checklists');

  return response.data;
};

// API 명세서 기준: 체크리스트 항목 체크 또는 해제
export const toggleChecklistItem = async (checklistItemId) => {
  const response = await api.patch(`/checklists/${checklistItemId}`);

  return response.data;
};

// API 명세서 기준: 체크리스트 초기화
export const clearChecklist = async () => {
  const response = await api.delete('/checklists/clear');

  return response.data;
};
