import { defineStore } from 'pinia';
import { useJwt } from '@vueuse/integrations/useJwt';
import type { JwtToken, Register } from 'src/components/models';
import {
  ClaimTypeRoles,
  ClaimTypeUserId,
  ClaimTypeUsername,
  RoleAdmin,
  type Login,
} from 'src/components/models';
import { useLocalStorage } from '@vueuse/core';
import { computed } from 'vue';
import axios from 'axios';

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

  const signInAsync = async (login: Login) => {
    const response = await axios.post<JwtToken>(`${process.env.API_BASE_URL}/users/login`, login);
    tokenString.value = response.data.token;
  };

  const registerAsync = async (register: Register, registerToken: string) => {
    const response = await axios.post<JwtToken>(`${process.env.API_BASE_URL}/users`, register, {
      headers: {
        Authorization: `Bearer ${registerToken}`,
      },
    });
    tokenString.value = response.data.token;
  };

  const logout = () => {
    tokenString.value = '';
  };

  const userId = computed(() => {
    const jwtPayload = jwtToken.payload.value;

    if (jwtPayload == null) {
      return null;
    }

    const payloadAny = jwtPayload as never;

    return payloadAny[ClaimTypeUserId] as string;
  });

  const username = computed(() => {
    const jwtPayload = jwtToken.payload.value;

    if (jwtPayload == null) {
      return null;
    }

    const payloadAny = jwtPayload as never;

    return payloadAny[ClaimTypeUsername] as string;
  });

  const isAdmin = computed(() => {
    const jwtPayload = jwtToken.payload.value;

    if (jwtPayload == null) {
      return null;
    }

    const payloadAny = jwtPayload as never;

    const rolesClaim = payloadAny[ClaimTypeRoles] as string[];

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
