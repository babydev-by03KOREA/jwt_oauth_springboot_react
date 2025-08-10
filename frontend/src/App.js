// App.js
import './App.css';
import {BrowserRouter as Router, Routes, Route, useLocation} from 'react-router-dom';
import SignupPage from './pages/SignupPage';
import HomePage from './pages/HomePage';
import LoginPage from "./pages/LoginPage";
import AdminPage from "./pages/AdminPage";
import Navbar from "./components/Navbar";
import {useDispatch, useSelector} from "react-redux";
import {useEffect} from "react";
import {fetchProfile, refreshAuth} from "./store/authSlice";
import AdminRoute from "./components/AdminRoute";
import OAuth2Redirect from "./pages/OAuth2Redirect";

function AppInner() {
    const dispatch = useDispatch();
    const location = useLocation();
    const {accessToken} = useSelector(state => state.auth);

    // 1) 전역 리프레시: skipRefresh=1이면 한 번 건너뜀
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const skip = params.get('skipRefresh') === '1';
        if (skip) return;

        // 이미 토큰 있으면 굳이 갱신하지 않기
        if (!accessToken) {
            dispatch(refreshAuth()); // 내부에서 withCredentials true
        }
    }, [dispatch, location.search, accessToken]);

    // 2) 토큰 생기면 프로필 조회
    useEffect(() => {
        if (accessToken) {
            dispatch(fetchProfile());
        }
    }, [accessToken, dispatch]);

    return (
        <>
            <Navbar/>
            <Routes>
                <Route path="/" element={<HomePage/>}/>
                <Route path="/signup" element={<SignupPage/>}/>
                <Route path="/login" element={<LoginPage/>}/>
                <Route path="/admin" element={<AdminRoute><AdminPage/></AdminRoute>} />
                <Route path="/oauth2/redirect" element={<OAuth2Redirect/>} />
            </Routes>
        </>
    );
}

export default function App() {
    return (
        <Router>
            <AppInner/>
        </Router>
    );
}
