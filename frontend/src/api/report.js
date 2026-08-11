import api from './instance';

// Swagger 기준: 분석 보고서 목록 조회
export const getReports = async () => {
  const response = await api.get('/analysis-reports');

  return response.data;
};

// API 명세서 기준: 분석 보고서 상세 조회
export const getReport = async (analysisReportId) => {
  const response = await api.get(`/analysis-reports/${analysisReportId}`);

  return response.data;
};

// Swagger 기준: 리포트 즐겨찾기 추가
export const addReportFavorite = async (analysisReportId) => {
  const response = await api.post(
    `/analysis-reports/${analysisReportId}/favorite`,
  );

  return response.data;
};

// Swagger 기준: 리포트 즐겨찾기 해제
export const deleteReportFavorite = async (analysisReportId) => {
  const response = await api.delete(
    `/analysis-reports/${analysisReportId}/favorite`,
  );

  return response.data;
};

// Swagger 기준: 전세 위험도 분석 보고서 생성
export const createReport = async (reportData) => {
  const response = await api.post('/analysis-reports/analyze', reportData);

  return response.data;
};

// API 명세서 기준: 분석 보고서 AI 메시지 생성
export const generateAiMessages = async (analysisReportId) => {
  const response = await api.post(
    `/analysis-reports/${analysisReportId}/ai-generate-messages`,
  );

  return response.data;
};

// Swagger 기준: 분석 보고서 삭제
export const deleteReport = async (analysisReportId) => {
  const response = await api.delete(`/analysis-reports/${analysisReportId}`);

  return response.data;
};

// API 명세서 기준: 분석 보고서 공유 링크 생성
export const shareReport = async (analysisReportId) => {
  const response = await api.post(
    `/analysis-reports/${analysisReportId}/share`,
  );

  return response.data;
};

// API 명세서 기준: 공유된 분석 보고서 조회
export const getSharedReport = async (shareToken) => {
  const response = await api.get(`/shared/${shareToken}`);

  return response.data;
};
