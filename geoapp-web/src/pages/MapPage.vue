<template>
  <LeafletMap :data="data" />
  <q-footer elevated class="footer bg-grey-8 text-white">
    <DateTimePicker v-model="from" label="From" />
    <DateTimePicker v-model="to" label="To" />
  </q-footer>
</template>
<script lang="ts" setup>
//import AmChartsMap from 'src/components/map/AmChartsMap.vue';
import { date, useQuasar } from 'quasar';
import { api } from 'src/boot/axios';
import DateTimePicker from 'src/components/common/DateTimePicker.vue';
import LeafletMap from 'src/components/map/LeafletMap.vue';
import { type Datapoint } from 'src/components/models';
import { useAuthStore } from 'src/stores/authStore';
import { ref, watchEffect } from 'vue';

const quasar = useQuasar();
const { userId } = useAuthStore();

const from = ref<Date>(date.subtractFromDate(Date.now(), { days: 1 }));
const to = ref<Date>();
const data = ref<Datapoint[]>([]);

watchEffect((onCleanup) => {
  quasar.loading.show({ delay: 50 });

  const controller = new AbortController();

  onCleanup(() => {
    controller.abort();
  });

  api
    .get<Datapoint[]>(`/data/${userId}`, {
      params: { from: from.value, to: to.value },
      signal: controller.signal,
    })
    .then((axiosResponse) => (data.value = axiosResponse.data))
    .catch((err) => console.error(err))
    .finally(() => quasar.loading.hide());
});
</script>
<style scoped>
.footer {
  display: flex;
}
</style>
