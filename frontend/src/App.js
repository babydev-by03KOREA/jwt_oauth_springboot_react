import './App.css';
import {BrowserRouter as Router, Routes, Route} from 'react-router-dom';
import SignupPage from './pages/SignupPage';
import HomePage from './pages/HomePage';
import LoginPage from "./pages/LoginPage";
import AdminPage from "./pages/AdminPage";
import Navbar from "./components/Navbar";
import {useDispatch, useSelector} from "react-redux";
import {useEffect} from "react";
import {fetchProfile, refreshAuth} from "./store/authSlice";
import AdminRoute from "./components/AdminRoute";

function App() {
    const dispatch = useDispatch();
    const {initialized, loading, accessToken} = useSelector(state => state.auth);

    // 앱 처음 마운트될 때 한 번만 토큰 갱신 시도
    useEffect(() => {
        dispatch(refreshAuth());
    }, [dispatch]);

    useEffect(() => {
        // 2) 토큰이 생기면 프로필 조회
        if (accessToken) {
            dispatch(fetchProfile());
        }
    }, [accessToken, dispatch]);

    // 아직 초기화 중이면 로딩 화면 또는 스켈레톤
    if (!initialized || loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <p className="text-gray-500">로딩 중…</p>
            </div>
        );
    }

    return (
        <Router>
            <Navbar/>
            <Routes>
                <Route path="/" element={<HomePage/>}/>
                <Route path="/signup" element={<SignupPage/>}/>
                <Route path="/login" element={<LoginPage/>}/>
                <Route
                    path="/admin"
                    element={
                        <AdminRoute>
                            <AdminPage/>
                        </AdminRoute>
                    }
                />
            </Routes>
        </Router>
    );
}

export default App;
