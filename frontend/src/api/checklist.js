import api from './instance';

// 체크리스트 목록 조회
export const getChecklists = async () => {
  const response = await api.get('/checklists');

  return response.data;
};

// 체크리스트 상세 조회
export const getChecklist = async (reportChecklistId) => {
  const response = await api.get(`/checklists/${reportChecklistId}`);

  return response.data;
};

// 체크리스트 항목 체크 또는 해제
export const toggleChecklistItem = async (
  reportChecklistId,
  checklistItemId,
) => {
  const response = await api.patch(
    `/checklists/${reportChecklistId}/items/${checklistItemId}`,
  );

  return response.data;
};

// 체크리스트 초기화
export const clearChecklist = async (analysisReportId) => {
  const response = await api.patch(`/checklists/clear/${analysisReportId}`);

  return response.data;
};

// 체크리스트 생성
export const createChecklist = async (analysisReportId) => {
  const response = await api.post(`/checklists/reports/${analysisReportId}`);

  return response.data;
};
