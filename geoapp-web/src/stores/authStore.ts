import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { useJwt } from '@vueuse/integrations/useJwt';

export const useAuthStore = defineStore('auth', () => {
  const tokenString = ref<string>('');
  const jwtToken = useJwt(tokenString);

  const isSignedIn = computed(() => tokenString.value && tokenString.value.length > 0);

  return {
    tokenString,
    jwtToken,
    isSignedIn,
  };
});
