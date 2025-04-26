<template>
  <q-layout view="hHh lpR fFf" class="layout">
    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<style scoped>
.layout {
  width: 100vw;
  height: 100vh;
  background-image: url('../assets/earth_11px.webp');
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
}
</style>
