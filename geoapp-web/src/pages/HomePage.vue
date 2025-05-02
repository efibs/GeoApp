<template>
  <div class="content">
    <h2 class="welcome-header">Welcome {{ username }}!</h2>
    <div class="widgets-galery">
      <StepsWidget :step-data="data" />
      <TotalStepsWidget :step-data="data" />
      <AndroidAppWidget />
    </div>
  </div>
</template>

<script setup lang="ts">
import { date, useQuasar } from 'quasar';
import { api } from 'src/boot/axios';
import AndroidAppWidget from 'src/components/home/AndroidAppWidget.vue';
import StepsWidget from 'src/components/home/StepsWidget.vue';
import TotalStepsWidget from 'src/components/home/TotalStepsWidget.vue';
import { type Datapoint } from 'src/components/models';
import { useAuthStore } from 'src/stores/authStore';
import { ref, watchEffect } from 'vue';

const quasar = useQuasar();
const { username, userId } = useAuthStore();

const data = ref<Datapoint[]>([]);

watchEffect((onCleanup) => {
  quasar.loading.show({ delay: 100 });

  const controller = new AbortController();

  onCleanup(() => {
    controller.abort();
  });

  api
    .get<Datapoint[]>(`/data/${userId}`, {
      params: { from: date.subtractFromDate(Date.now(), { hours: 12 }) },
      signal: controller.signal,
    })
    .then((axiosResponse) => (data.value = axiosResponse.data))
    .catch((err) => console.error(err))
    .finally(() => quasar.loading.hide());
});
</script>
<style scoped>
.content {
  width: 100%;
  height: 100%;
}

.welcome-header {
  text-align: center;
}

.widgets-galery {
  width: 100%;
  display: flex;
  padding-left: 10%;
  padding-right: 10%;
  gap: 15px;
  flex-direction: row;
  flex-wrap: wrap;
}
</style>
