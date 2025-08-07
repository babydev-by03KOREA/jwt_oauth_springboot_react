import axios from 'axios';
import { store } from '../store';
import { setAccessToken } from '../store/authSlice';

export async function rotateToken() {
    const { data } = await axios.post(
        '/api/auth/refresh',
        {},
        { withCredentials: true }
    );
    store.dispatch(setAccessToken(data.accessToken));
    return data.accessToken;
}
