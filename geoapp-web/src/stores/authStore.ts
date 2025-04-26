import { defineStore } from 'pinia';
import { ref } from 'vue';
import { useJwt } from '@vueuse/integrations/useJwt';
import { type Login } from 'src/components/models';
import { api } from 'boot/axios';

export const useAuthStore = defineStore('auth', () => {
  const tokenString = ref<string>('');
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

  const signInAsync = async (login: Login) => {
    const response = await api.post('/users/login', login);
    tokenString.value = response.data.token;
    api.defaults.headers.common.Authorization = `Bearer: ${tokenString.value}`;
  };

  return {
    jwtToken,
    isSignedIn,
    signInAsync,
  };
});
