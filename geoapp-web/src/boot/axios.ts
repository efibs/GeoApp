import { defineBoot } from '#q-app/wrappers';
import axios, { type AxiosInstance } from 'axios';
import { globalRouter } from 'src/router/globalRouter';
import { useAuthStore } from 'src/stores/authStore';

const authStore = useAuthStore();

declare module 'vue' {
  interface ComponentCustomProperties {
    $axios: AxiosInstance;
    $api: AxiosInstance;
  }
}

// Be careful when using SSR for cross-request state pollution
// due to creating a Singleton instance here;
// If any client changes this (global) instance, it might be a
// good idea to move this instance creation inside of the
// "export default () => {}" function below (which runs individually
// for each client)
const api = axios.create({ baseURL: process.env.API_BASE_URL! });

api.interceptors.request.use((config) => {
  const controller = new AbortController();

  if (authStore.isSignedIn() == false) {
    controller.abort();
    authStore.logout();

    if (globalRouter.router != null) {
      globalRouter.router
        .push({ name: 'account-signin' })
        .then()
        .catch((err) => console.error(err));
    } else {
      console.error('Router not available.');
    }
  } else if (authStore.tokenString) {
    config.headers.Authorization = `Bearer ${authStore.tokenString}`;
  }

  return {
    ...config,
    signal: controller.signal,
  };
});

export default defineBoot(({ app }) => {
  // for use inside Vue files (Options API) through this.$axios and this.$api

  app.config.globalProperties.$axios = axios;
  // ^ ^ ^ this will allow you to use this.$axios (for Vue Options API form)
  //       so you won't necessarily have to import axios in each vue file

  app.config.globalProperties.$api = api;
  // ^ ^ ^ this will allow you to use this.$api (for Vue Options API form)
  //       so you can easily perform requests against your app's API
});

export { api };
