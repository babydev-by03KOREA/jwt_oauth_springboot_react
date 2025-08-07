import React from 'react';
import { useSelector } from 'react-redux';
import { Navigate } from 'react-router-dom';

export default function AdminRoute({ children }) {
    const roles = useSelector(state => state.auth.roles);
    const isAdmin = roles.includes('ROLE_ADMIN');

    if (!isAdmin) {
        // 관리자 권한 없으면 홈으로 보내기
        return <Navigate to="/" replace />;
    }
    return children;
}
