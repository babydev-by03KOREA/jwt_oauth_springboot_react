import React, {useState} from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

export default function SignupPage() {
    const navigate = useNavigate()

    const [form, setForm] = useState({
        userId: '',
        nickname: '',
        email: '',
        password: '',
        confirm: ''
    });
    const [error, setError]     = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = e => {
        setForm({...form, [e.target.name]: e.target.value});
    };

    const handleSubmit = async e => {
        e.preventDefault();
        if (form.password !== form.confirm) {
            setError('비밀번호가 일치하지 않습니다.');
            return;
        }

        try {
            setLoading(true);
            await axios.post(
                '/api/auth/signup',
                {
                    displayName: form.nickname,
                    email: form.email,
                    password: form.password,
                    userId: form.userId,
                },
                {
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    withCredentials: false,  // signup은 쿠키 사용 안 함
                }
            );
            navigate('/login');
        } catch (e) {
            console.error(e);
            // 서버가 내려준 에러 메시지를 보여주거나, 기본 메시지
            setError(e.response?.data?.message || '회원가입 중 오류가 발생했습니다.');
            alert(error.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
            <div className="max-w-md w-full bg-white rounded-2xl shadow-lg p-8 space-y-6">
                <h2 className="text-3xl font-semibold text-gray-800 text-center">
                    회원가입
                </h2>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label htmlFor="userId" className="block text-sm font-medium text-gray-700">
                            아이디
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
                        <label htmlFor="nickname" className="block text-sm font-medium text-gray-700">
                            닉네임
                        </label>
                        <input
                            id="nickname"
                            name="nickname"
                            type="text"
                            value={form.nickname}
                            onChange={handleChange}
                            required
                            className="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-lg
                         focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"
                        />
                    </div>
                    <div>
                        <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                            이메일 주소
                        </label>
                        <input
                            id="email"
                            name="email"
                            type="email"
                            value={form.email}
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
                            minLength={8}
                            className="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-lg
                         focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"
                        />
                    </div>
                    <div>
                        <label htmlFor="confirm" className="block text-sm font-medium text-gray-700">
                            비밀번호 확인
                        </label>
                        <input
                            id="confirm"
                            name="confirm"
                            type="password"
                            value={form.confirm}
                            onChange={handleChange}
                            required
                            minLength={8}
                            className="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-lg
                         focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"
                        />
                    </div>
                    <button
                        type="submit"
                        className="w-full py-3 bg-indigo-600 text-white rounded-lg font-semibold
                       hover:bg-indigo-700 transition-colors"
                    >
                        가입하기
                    </button>
                </form>
                <p className="text-center text-sm text-gray-500">
                    이미 계정이 있으신가요?{' '}
                    <a href="/login" className="text-indigo-600 hover:underline">
                        로그인
                    </a>
                </p>
            </div>
        </div>
    );
}
