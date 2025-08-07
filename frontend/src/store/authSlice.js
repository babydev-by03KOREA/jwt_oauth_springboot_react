// src/store/authSlice.js
import {createSlice, createAsyncThunk} from '@reduxjs/toolkit';
import axios from 'axios';
import {jwtDecode} from 'jwt-decode';

const initialState = {
    accessToken: null,
    roles: [],
    profile: null,
    initialized: false,
    loading: false,
    error: null,
};

export const refreshAuth = createAsyncThunk(
    'auth/refresh',
    async () => {
        const {data} = await axios.post(
            '/api/auth/refresh',
            {},
            {withCredentials: true}
        );
        return data.accessToken;
    }
);

// 프로필 조회 Thunk
export const fetchProfile = createAsyncThunk(
    'auth/fetchProfile',
    async (_, thunkAPI) => {
        const token = thunkAPI.getState().auth.accessToken;
        const { data } = await axios.get('/api/auth/me', {
            headers: { Authorization: `Bearer ${token}` },
            withCredentials: true
        });
        return data;
    }
);

const authSlice = createSlice({
    name: 'auth',
    initialState,
    reducers: {
        setCredentials(state, action) {
            const token = action.payload;
            state.accessToken = token;
            try {
                const decoded = jwtDecode(token);
                state.roles = Array.isArray(decoded.roles) ? decoded.roles : [];
            } catch {
                state.roles = [];
            }
            state.profile = null;
            state.error = null;
        },
        clearCredentials(state) {
            state.accessToken = null;
            state.roles = [];
            state.profile = null;
            state.initialized = true;
            state.error = null;
        },
    },
    extraReducers: builder => {
        builder
            // --- 토큰 복원 ---
            .addCase(refreshAuth.pending, state => {
                state.loading = true;
                state.error   = null;
            })
            .addCase(refreshAuth.fulfilled, (state, action) => {
                authSlice.caseReducers.setCredentials(state, action);
                state.initialized = true;
                state.loading     = false;
            })
            .addCase(refreshAuth.rejected, (state, action) => {
                state.accessToken = null;
                state.roles       = [];
                state.profile     = null;
                state.initialized = true;
                state.loading     = false;
                state.error       = action.error.message;
            })

            // --- 프로필 조회 ---
            .addCase(fetchProfile.pending, state => {
                state.loading = true;
            })
            .addCase(fetchProfile.fulfilled, (state, action) => {
                state.profile = action.payload;
                state.loading = false;
            })
            .addCase(fetchProfile.rejected, (state, action) => {
                state.error   = action.error.message;
                state.loading = false;
            });
    }
});

export const {setCredentials, clearCredentials} = authSlice.actions;
export default authSlice.reducer;
