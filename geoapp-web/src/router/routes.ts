import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/HomePage.vue') },
      { path: 'map', component: () => import('pages/MapPage.vue') },
    ],
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/login',
    component: () => import('layouts/LoginLayout.vue'),
    children: [
      { path: '', name: 'account-signin', component: () => import('pages/LoginPage.vue') },
    ],
  },
  {
    path: '/register',
    component: () => import('layouts/LoginLayout.vue'),
    children: [
      { path: '', name: 'account-register', component: () => import('pages/RegisterPage.vue') },
    ],
  },
  {
    path: '/generate-token',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'generate-token', component: () => import('pages/GenerateTokenPage.vue') },
    ],
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
];

export default routes;
