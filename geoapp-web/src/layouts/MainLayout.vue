<template>
  <q-layout class="layout" view="hHh lpR fFf">
    <q-header elevated class="bg-primary text-white">
      <q-toolbar>
        <q-btn dense flat round icon="menu" @click="toggleLeftDrawer" />

        <q-toolbar-title>
          <q-avatar>
            <img src="https://cdn.quasar.dev/logo-v2/svg/logo-mono-white.svg" />
          </q-avatar>
          GeoApp
        </q-toolbar-title>

        <q-space />
        <q-btn flat class="user-btn" round>
          <q-avatar color="primary" icon="person" />
          <q-menu>
            <div class="row no-wrap q-pa-md">
              <div class="column items-center">
                <q-avatar size="72px" icon="person"> </q-avatar>

                <div class="text-subtitle1 q-mt-md q-mb-xs">{{ username }}</div>

                <q-btn
                  @click="userLogout"
                  color="primary"
                  label="Logout"
                  push
                  size="sm"
                  v-close-popup
                />
              </div>
            </div>
          </q-menu>
        </q-btn>
      </q-toolbar>
    </q-header>

    <q-drawer v-model="leftDrawerOpen" side="left" overlay elevated>
      <!-- drawer content -->
      <EssentialLink title="Home" icon="home" route="/" />
      <EssentialLink title="Map" caption="Your map" icon="map" route="/map" />
      <EssentialLink
        title="Generate token"
        caption="Generate tokens to access the API"
        icon="key"
        route="/generate-token"
      />
    </q-drawer>

    <q-page-container class="page">
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script lang="ts" setup>
import EssentialLink from 'src/components/common/EssentialLink.vue';
import { useAuthStore } from 'src/stores/authStore';
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const { username, logout } = useAuthStore();

const leftDrawerOpen = ref(false);

const toggleLeftDrawer = () => {
  leftDrawerOpen.value = !leftDrawerOpen.value;
};

const userLogout = async () => {
  logout();
  await router.push('/login');
};
</script>

<style scoped>
.toggle-drawer-button {
  position: fixed;
  z-index: 999999;
  margin: 10px;
}

.layout {
  width: 100vw;
  height: 100vh;
  background-image: url('../assets/earth_11px.webp');
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  display: flex;
}

.page {
  display: flex;
  width: 100%;
  height: 100%;
}
</style>
