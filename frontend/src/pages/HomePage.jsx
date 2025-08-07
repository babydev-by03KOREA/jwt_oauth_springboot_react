import React from 'react';

const destinations = [
    {
        name: '파리, 프랑스',
        img: 'https://static.independent.co.uk/2025/04/25/13/42/iStock-1498516775.jpg',
    },
    {
        name: '교토, 일본',
        img: 'https://www.mensjournal.com/.image/t_share/MTk2MTM2NjY0Mzg4MzQ3MDI1/kyoto.jpg',
    },
    {
        name: '몰디브',
        img: 'https://media-cdn.tripadvisor.com/media/photo-s/1b/f4/56/f1/aerial-view.jpg',
    },
    {
        name: '뉴욕, 미국',
        img: 'https://www.flightgift.com/media/wp/FG/2023/12/shutterstock_248799484-scaled.webp',
    },
];

export default function HomePage() {
    return (
        <div className="space-y-16">
            {/* Hero 섹션 */}
            <section
                className="h-96 bg-cover bg-center flex items-center justify-center"
                style={{
                    backgroundImage:
                        'url(https://source.unsplash.com/1600x900/?travel,landscape)',
                }}
            >
                <div className="bg-white bg-opacity-70 p-8 rounded-xl text-center max-w-xl">
                    <h1 className="text-4xl font-bold text-gray-800 mb-4">
                        나만의 여행을 시작하세요
                    </h1>
                    <p className="text-gray-600 mb-6">
                        전 세계 숨겨진 명소부터 인기 여행지까지, 간편하게 검색해 보세요.
                    </p>
                    <div className="flex">
                        <input
                            type="text"
                            placeholder="어디로 떠나고 싶으신가요?"
                            className="flex-1 px-4 py-2 border border-gray-300 rounded-l-lg focus:outline-none focus:ring-2 focus:ring-indigo-400"
                        />
                        <button className="px-6 bg-indigo-600 text-white rounded-r-lg hover:bg-indigo-700 transition-colors">
                            검색
                        </button>
                    </div>
                </div>
            </section>

            {/* 인기 여행지 섹션 */}
            <section className="max-w-7xl mx-auto px-4">
                <h2 className="text-2xl font-semibold text-gray-800 mb-6">
                    인기 여행지 추천
                </h2>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                    {destinations.map(dest => (
                        <div
                            key={dest.name}
                            className="bg-white rounded-lg shadow hover:shadow-lg transition-shadow overflow-hidden"
                        >
                            <img
                                src={dest.img}
                                alt={dest.name}
                                className="w-full h-48 object-cover"
                            />
                            <div className="p-4">
                                <h3 className="text-lg font-medium text-gray-700">
                                    {dest.name}
                                </h3>
                            </div>
                        </div>
                    ))}
                </div>
            </section>

            {/* 푸터 콜투액션 */}
            <section className="bg-indigo-600 py-12">
                <div className="max-w-3xl mx-auto text-center text-white space-y-4 px-4">
                    <h2 className="text-3xl font-bold">특별한 여행을 지금 예약하세요</h2>
                    <p>최신 할인 혜택과 다양한 여행 정보를 만나보세요.</p>
                    <button className="mt-4 px-8 py-3 bg-white text-indigo-600 font-semibold rounded-lg hover:bg-gray-100 transition">
                        지금 예약하기
                    </button>
                </div>
            </section>
        </div>
    );
}
