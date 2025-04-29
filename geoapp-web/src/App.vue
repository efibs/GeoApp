<template>
  <router-view />
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { globalRouter } from './router/globalRouter';

const router = useRouter();
globalRouter.router = router;
</script>
