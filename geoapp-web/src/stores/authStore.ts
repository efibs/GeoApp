import { defineStore } from 'pinia';
import { useJwt } from '@vueuse/integrations/useJwt';
import { type Login } from 'src/components/models';
import { api } from 'boot/axios';
import { useLocalStorage } from '@vueuse/core';

export const useAuthStore = defineStore('auth', () => {
  const tokenString = useLocalStorage('jwt-token', '');
  const jwtToken = useJwt(tokenString);

  const isSignedIn = () => {
    if (!(tokenString.value && tokenString.value.length > 0)) {
      return false;
    }

    const expirationNum = jwtToken.payload.value?.exp;

    if (!expirationNum) {
      return false;
    }

    const expiry = new Date(0);
    expiry.setUTCSeconds(expirationNum);
    const now = new Date();

    return expiry > now;
  };

  const setTokenOnAxios = () => {
    api.defaults.headers.common.Authorization = `Bearer ${tokenString.value}`;
  };

  const signInAsync = async (login: Login) => {
    const response = await api.post('/users/login', login);
    tokenString.value = response.data.token;
    setTokenOnAxios();
  };

  if (isSignedIn()) {
    setTokenOnAxios();
  }

  return {
    jwtToken,
    isSignedIn,
    signInAsync,
  };
});
