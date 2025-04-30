<template>
  <router-view />
  <div class="background-image"></div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { globalRouter } from './router/globalRouter';

const router = useRouter();
globalRouter.router = router;
</script>
<style scoped>
.background-image {
  position: fixed;
  top: 0;
  left: 0;
  z-index: -9999999999999999999999999999999999999999999999999;
  width: 100vw;
  height: 100vh;
  background-image: url('./assets/earth_11px.webp');
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  filter: brightness(0.65);
}
</style>
