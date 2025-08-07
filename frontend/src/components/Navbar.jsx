import React, {useEffect, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useSelector, useDispatch} from 'react-redux';
import {clearCredentials} from '../store/authSlice';
import {GiHamburgerMenu} from 'react-icons/gi';
import {IoMdClose} from 'react-icons/io';
import axios from 'axios';

function readCookie(name) {
    const m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return m ? decodeURIComponent(m[1]) : null;
}

export default function Navbar() {
    const [isOpen, setIsOpen] = useState(false);
    const navigate = useNavigate();
    const dispatch = useDispatch();

    const {accessToken, roles, profile} = useSelector((state) => state.auth);
    const isLoggedIn = Boolean(accessToken);
    const isAdmin = roles.includes('ROLE_ADMIN');
    const nickname = profile?.displayName ?? profile?.nickname ?? '';

    const [deviceId, setDeviceId] = useState('');
    useEffect(() => {
        setDeviceId(readCookie('device_id') || '');
    }, []);

    const onLogout = async () => {
        try {
            await axios.post(
                '/api/auth/logout',
                {},
                {
                    headers: {
                        Authorization: `Bearer ${accessToken}`,
                        'X-Device-Id': deviceId,
                    },
                    withCredentials: true,
                }
            );
            alert('정상적으로 로그아웃 되었습니다.');
        } catch (e) {
            console.error('로그아웃 API 실패:', e);
        } finally {
            dispatch(clearCredentials());
            navigate('/');
        }
    };

    return (
        <nav className="bg-white shadow-md">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between h-16 items-center">
                    {/* 로고 */}
                    <Link to="/" className="text-2xl font-bold text-indigo-600">
                        MyApp
                    </Link>

                    {/* 데스크탑 메뉴 */}
                    <div className="hidden md:flex md:items-center md:space-x-4">
                        {isAdmin && (
                            <Link
                                to="/admin"
                                className="text-gray-700 hover:text-indigo-600 font-medium"
                            >
                                관리자 페이지
                            </Link>
                        )}

                        {!isLoggedIn ? (
                            <>
                                <Link
                                    to="/login"
                                    className="text-gray-700 hover:text-indigo-600 font-medium"
                                >
                                    로그인
                                </Link>
                                <Link
                                    to="/signup"
                                    className="text-gray-700 hover:text-indigo-600 font-medium"
                                >
                                    회원가입
                                </Link>
                            </>
                        ) : (
                            <>
                <span className="text-gray-800 font-medium">
                  {nickname}님, 환영합니다
                </span>
                                <button
                                    onClick={onLogout}
                                    className="text-gray-700 hover:text-indigo-600 font-medium"
                                >
                                    로그아웃
                                </button>
                            </>
                        )}
                    </div>

                    {/* 모바일 토글 */}
                    <div className="flex items-center md:hidden">
                        <button
                            onClick={() => setIsOpen((o) => !o)}
                            className="p-2 rounded-md text-gray-700 hover:text-indigo-600"
                        >
                            {isOpen ? <IoMdClose size={24}/> : <GiHamburgerMenu size={24}/>}
                        </button>
                    </div>
                </div>
            </div>

            {/* 모바일 메뉴 */}
            {isOpen && (
                <div className="md:hidden bg-white shadow-inner space-y-1">
                    <Link
                        to="/"
                        onClick={() => setIsOpen(false)}
                        className="block px-4 py-2 text-gray-700 hover:text-indigo-600"
                    >
                        홈
                    </Link>
                    {isAdmin && (
                        <Link
                            to="/admin"
                            onClick={() => setIsOpen(false)}
                            className="block px-4 py-2 text-gray-700 hover:text-indigo-600"
                        >
                            관리자 페이지
                        </Link>
                    )}
                    {!isLoggedIn ? (
                        <>
                            <Link
                                to="/login"
                                onClick={() => setIsOpen(false)}
                                className="block px-4 py-2 text-gray-700 hover:text-indigo-600"
                            >
                                로그인
                            </Link>
                            <Link
                                to="/signup"
                                onClick={() => setIsOpen(false)}
                                className="block px-4 py-2 text-gray-700 hover:text-indigo-600"
                            >
                                회원가입
                            </Link>
                        </>
                    ) : (
                        <button
                            onClick={() => {
                                setIsOpen(false);
                                onLogout();
                            }}
                            className="w-full text-left px-4 py-2 text-gray-700 hover:text-indigo-600"
                        >
                            로그아웃
                        </button>
                    )}
                </div>
            )}
        </nav>
    );
}
