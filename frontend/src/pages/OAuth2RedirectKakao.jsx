import React, {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {useDispatch} from 'react-redux';
import {setCredentials, fetchProfile} from '../store/authSlice';

export default function OAuth2RedirectKakao() {
    const navigate = useNavigate();
    const dispatch = useDispatch();

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const accessToken = params.get('accessToken');

        if (accessToken) {
            // 1) Redux에 저장하고
            dispatch(setCredentials(accessToken));
            // 2) 프로필도 바로 로드
            dispatch(fetchProfile());
            // 3) 홈으로 이동
            navigate('/', {replace: true});
        } else {
            // 실패 시
            navigate('/login', {replace: true});
        }
    }, [dispatch, navigate]);

    return (
        <div className="min-h-screen flex items-center justify-center">
            <p className="text-gray-500">로그인 처리 중…</p>
        </div>
    );
}