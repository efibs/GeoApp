import { defineStore } from 'pinia';
import type { JwtTokenResponse, Register, JwtToken } from 'src/components/models';
import { RoleAdmin, type Login } from 'src/components/models';
import { useLocalStorage } from '@vueuse/core';
import { computed } from 'vue';
import axios from 'axios';
import { jwtDecode } from 'jwt-decode';

export const useAuthStore = defineStore('auth', () => {
  const tokenString = useLocalStorage('jwt-token', '');
  const jwtToken = computed(() => jwtDecode<JwtToken>(tokenString.value));

  const isSignedIn = () => {
    if (!(tokenString.value && tokenString.value.length > 0)) {
      return false;
    }

    const expirationNum = jwtToken.value.exp;

    if (!expirationNum) {
      return false;
    }

    const expiry = new Date(0);
    expiry.setUTCSeconds(expirationNum);
    const now = new Date();

    return expiry > now;
  };

  const signInAsync = async (login: Login) => {
    const response = await axios.post<JwtTokenResponse>(
      `${process.env.API_BASE_URL}/users/login`,
      login,
    );
    tokenString.value = response.data.token;
  };

  const registerAsync = async (register: Register, registerToken: string) => {
    const response = await axios.post<JwtTokenResponse>(
      `${process.env.API_BASE_URL}/users`,
      register,
      {
        headers: {
          Authorization: `Bearer ${registerToken}`,
        },
      },
    );
    tokenString.value = response.data.token;
  };

  const logout = () => {
    tokenString.value = '';
  };

  const userId = computed(
    () => jwtToken.value['http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier'],
  );

  const username = computed(
    () => jwtToken.value['http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name'],
  );

  const isAdmin = computed(() => {
    const rolesClaim =
      jwtToken.value['http://schemas.microsoft.com/ws/2008/06/identity/claims/role'];

    return rolesClaim?.includes(RoleAdmin) == true;
  });

  return {
    tokenString,
    jwtToken,
    isSignedIn,
    signInAsync,
    registerAsync,
    logout,
    userId,
    username,
    isAdmin,
  };
});
