<template>
  <div class="content">
    <h2 class="welcome-header">Welcome {{ username }}!</h2>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from 'src/stores/authStore';

const { username } = useAuthStore();
</script>
<style scoped>
.content {
  width: 100%;
  height: 100%;
}

.welcome-header {
  text-align: center;
}
</style>
