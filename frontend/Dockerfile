FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ARG VITE_API_BASE_URL=/api
ARG VITE_ANALYSIS_TEST_BIRTH_DATE
ARG VITE_ANALYSIS_TEST_PHONE_NO
ARG VITE_ANALYSIS_TEST_USER_NAME

ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_ANALYSIS_TEST_BIRTH_DATE=${VITE_ANALYSIS_TEST_BIRTH_DATE}
ENV VITE_ANALYSIS_TEST_PHONE_NO=${VITE_ANALYSIS_TEST_PHONE_NO}
ENV VITE_ANALYSIS_TEST_USER_NAME=${VITE_ANALYSIS_TEST_USER_NAME}

RUN npm run build


FROM nginx:alpine

RUN rm /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]