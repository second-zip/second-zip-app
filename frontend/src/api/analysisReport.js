import api from './instance';

const EXTERNAL_REQUEST_TIMEOUT_MS = 300_000;

export const getExternalReadiness = async () => {
  const response = await api.get('/analysis-reports/external-readiness');

  return response.data;
};

export const createAnalysisRequest = async (payload) => {
  const response = await api.post('/analysis-reports/requests', payload);

  return response.data;
};

export const getAnalysisRequest = async (requestId) => {
  const response = await api.get(`/analysis-reports/requests/${requestId}`);

  return response.data;
};

export const startAnalysisAuth = async (requestId, payload) => {
  const response = await api.post(
    `/analysis-reports/requests/${requestId}/auth/start`,
    payload,
    { timeout: EXTERNAL_REQUEST_TIMEOUT_MS },
  );

  return response.data;
};

export const continueAnalysisAuth = async (requestId, payload) => {
  const response = await api.post(
    `/analysis-reports/requests/${requestId}/auth/continue`,
    payload,
    { timeout: EXTERNAL_REQUEST_TIMEOUT_MS },
  );

  return response.data;
};

export const completeAnalysis = async (requestId) => {
  const response = await api.post(
    `/analysis-reports/requests/${requestId}/complete`,
    undefined,
    { timeout: EXTERNAL_REQUEST_TIMEOUT_MS },
  );

  return response.data;
};

export const retryAnalysis = async (requestId) => {
  const response = await api.post(
    `/analysis-reports/requests/${requestId}/retry`,
    undefined,
    { timeout: EXTERNAL_REQUEST_TIMEOUT_MS },
  );

  return response.data;
};

export const createSpecialTerms = async (analysisReportId) => {
  const response = await api.post(
    `/analysis-reports/${analysisReportId}/special-terms`,
    undefined,
    { timeout: EXTERNAL_REQUEST_TIMEOUT_MS },
  );

  return response.data;
};
