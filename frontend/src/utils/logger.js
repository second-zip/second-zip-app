export const logger = {
  error(context, error, metadata = {}) {
    console.error(`[${context}]`, metadata, error);
  },
};
