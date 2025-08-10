import React, {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {useDispatch} from 'react-redux';
import { fetchProfile, refreshAuth} from '../store/authSlice';

export default function OAuth2Redirect() {
    const navigate = useNavigate();
    const dispatch = useDispatch();

    useEffect(() => {
        let timer = setTimeout(async () => {
            try {
                // 1) 쿠키가 막 세팅된 직후라 200ms 정도 대기
                await dispatch(refreshAuth());         // 새 accessToken 갱신
                await dispatch(fetchProfile());        // 프로필 로드
                // 2) URL 정리 (skipRefresh 제거) + 홈으로
                navigate('/', { replace: true });
            } catch (e) {
                alert('SNS 로그인 세션 확인에 실패했습니다. 다시 시도해 주세요.');
                navigate('/login', { replace: true });
            }
        }, 200);

        return () => clearTimeout(timer);
    }, [dispatch, navigate]);

    return (
        <div className="min-h-screen flex items-center justify-center">
            <p className="text-gray-500">로그인 처리 중…</p>
        </div>
    );
}