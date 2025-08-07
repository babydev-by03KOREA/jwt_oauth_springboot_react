import React, {useEffect, useState} from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { v4 as uuidv4 } from 'uuid';
import axios from "axios";
import {useDispatch} from "react-redux";
import {fetchProfile, setCredentials} from "../store/authSlice";

function readCookie(name) {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? match[2] : null;
}

export default function LoginPage() {
    const navigate = useNavigate();
    const dispatch = useDispatch();

    const [form, setForm] = useState({
        userId: '',
        password: '',
    });
    const [error, setError]     = useState('');
    const [loading, setLoading] = useState(false);
    const [deviceId, setDeviceId] = useState('');

    // 초기 로드 시 device_id 쿠키 확인/생성
    useEffect(() => {
        let id = readCookie('device_id');
        if (!id) {
            id = uuidv4();
            // 서버에서도 같은 로직으로 쿠키를 세팅하므로, 프론트에선 임시로만 저장
            document.cookie = `device_id=${id}; Path=/; Max-Age=${60 * 60 * 24 * 365}; SameSite=Strict`;
        }
        setDeviceId(id);
    }, []);

    const handleChange = e => {
        setForm({ ...form, [e.target.name]: e.target.value });
    };

    const handleSubmit = async e => {
        e.preventDefault();
        setError('');
        try {
            setLoading(true);
            const {data} = await axios.post(
                '/api/auth/login',
                {
                    userId: form.userId,
                    password: form.password
                },
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Device-Id': deviceId,
                        'User-Agent': navigator.userAgent
                    },
                    withCredentials: true  // refreshToken 쿠키를 받기 위해
                }
            );
            // localStorage/sessionStorage 금지 → XSS 위험
            dispatch(setCredentials(data.accessToken));
            dispatch(fetchProfile());
            navigate('/');
        } catch (e) {
            console.error(e);
            setError(e.response?.data?.message || '로그인 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
            <div className="max-w-md w-full bg-white rounded-2xl shadow-lg p-8 space-y-6">
                <h2 className="text-3xl font-semibold text-gray-800 text-center">
                    로그인
                </h2>
                <form onSubmit={handleSubmit} className="space-y-5">
                    <div>
                        <label htmlFor="userId" className="block text-sm font-medium text-gray-700">
                            사용자 ID
                        </label>
                        <input
                            id="userId"
                            name="userId"
                            type="text"
                            value={form.userId}
                            onChange={handleChange}
                            required
                            className="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-lg
                         focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"
                        />
                    </div>
                    <div>
                        <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                            비밀번호
                        </label>
                        <input
                            id="password"
                            name="password"
                            type="password"
                            value={form.password}
                            onChange={handleChange}
                            required
                            className="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-lg
                         focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"
                        />
                    </div>
                    <button
                        type="submit"
                        className="w-full py-3 bg-indigo-600 text-white rounded-lg font-semibold
                       hover:bg-indigo-700 transition-colors"
                    >
                        로그인
                    </button>
                </form>
                <a href="/oauth2/authorization/kakao">
                    <img src="../../assets/OAuth/kakao_login_large_wide.png" alt="카카오 로그인"/>
                </a>
                <div className="flex justify-between text-sm text-gray-500">
                    <Link to="/signup" className="text-indigo-600 hover:underline">
                        회원가입
                    </Link>
                    {/*<Link to="/forgot-password" className="hover:underline">*/}
                    {/*    비밀번호 찾기*/}
                    {/*</Link>*/}
                </div>
            </div>
        </div>
    );
}
