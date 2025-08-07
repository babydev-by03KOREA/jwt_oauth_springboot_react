import React from 'react';

export default function AdminPage() {
    // TODO: 실제 데이터는 API 호출로 가져와서 useState/useEffect로 관리하세요.
    const stats = {
        users: 1284,
        sessions: 342,
        bookings: 76,
    };

    const recentUsers = [
        { id: 1, name: '홍길동', email: 'hong@example.com', joined: '2025-07-30' },
        { id: 2, name: '김영희', email: 'younghee@example.com', joined: '2025-07-28' },
        { id: 3, name: '이철수', email: 'chulsu@example.com', joined: '2025-07-25' },
    ];

    return (
        <div className="min-h-screen bg-gray-100 p-6">
            <h1 className="text-3xl font-bold text-gray-800 mb-6">관리자 대시보드</h1>

            {/* 통계 카드 */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 mb-8">
                <div className="bg-white p-6 rounded-lg shadow hover:shadow-lg transition">
                    <p className="text-gray-500">전체 사용자 수</p>
                    <p className="text-3xl font-semibold text-indigo-600">{stats.users}</p>
                </div>
                <div className="bg-white p-6 rounded-lg shadow hover:shadow-lg transition">
                    <p className="text-gray-500">활성 세션 수</p>
                    <p className="text-3xl font-semibold text-indigo-600">{stats.sessions}</p>
                </div>
                <div className="bg-white p-6 rounded-lg shadow hover:shadow-lg transition">
                    <p className="text-gray-500">총 예약 건수</p>
                    <p className="text-3xl font-semibold text-indigo-600">{stats.bookings}</p>
                </div>
            </div>

            {/* 최근 가입 사용자 테이블 */}
            <div className="bg-white rounded-lg shadow overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                    <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                            ID
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                            사용자 이름
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                            이메일
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                            가입일
                        </th>
                    </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                    {recentUsers.map(user => (
                        <tr key={user.id} className="hover:bg-gray-50">
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">{user.id}</td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">{user.name}</td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">{user.email}</td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">{user.joined}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
