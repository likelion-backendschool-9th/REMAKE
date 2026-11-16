import { create } from "zustand";

type User = {
  id: number;
  nickname: string;
};

type AuthState = {
  user: User | null;
  setUser: (user: User | null) => void;
};

// 서버 데이터는 TanStack Query로 관리하고, 여기에는 로그인 정보 등 최소한만 둔다
export const useAuthStore = create<AuthState>()((set) => ({
  user: null,
  setUser: (user) => set({ user }),
}));
