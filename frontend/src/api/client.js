// 토큰을 Axios 인터셉터 등에 붙여서 보호된 API를 호출
import axios from 'axios';
import { store } from '../store';  // Redux store
import { clearAccessToken, setAccessToken } from '../store/authSlice';
import { rotateToken } from '../auth/refreshToken';  // 앞서 만든 토큰 재발급 유틸

// 1) Axios 인스턴스 생성
const client = axios.create({
    baseURL: '/api',            // /api/** 로 시작하는 엔드포인트
    withCredentials: true,      // 쿠키 기반 refreshToken 전송
    headers: {
        'Content-Type': 'application/json'
    }
});

// 2) 요청 인터셉터: 모든 요청에 Authorization 헤더 자동 부착
client.interceptors.request.use(config => {
    const token = store.getState().auth.accessToken;
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// 3) 응답 인터셉터: 401 Unauthorized 시 토큰 재발급 시도
client.interceptors.response.use(
    response => response,
    async error => {
        const status = error.response?.status;
        const originalRequest = error.config;

        // 401 에러 && 아직 retry 안 한 요청이면
        if (status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            try {
                // 토큰 재발급
                const newToken = await rotateToken();
                // Redux에 저장
                store.dispatch(setAccessToken(newToken));
                // 헤더에 갱신된 토큰 붙이고 재요청
                originalRequest.headers.Authorization = `Bearer ${newToken}`;
                return client(originalRequest);
            } catch (refreshError) {
                // 재발급 실패 시 → 로그아웃 처리
                store.dispatch(clearAccessToken());
                // 리다이렉트하거나 로그인 페이지로 안내
                window.location.href = '/login';
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    }
);

export default client;
