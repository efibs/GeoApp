<template>
  <LeafletMap :map-data="mapData" :colors="dataColors" />

  <!-- Compact footer with toggle button -->
  <q-footer elevated class="bg-grey-8 text-white q-pa-sm row justify-between items-center">
    <div>Options</div>
    <q-btn flat icon="tune" label="Filters" @click="showDrawer = true" />
  </q-footer>

  <!-- Bottom drawer with input fields -->
  <q-drawer
    v-model="showDrawer"
    side="left"
    overlay
    bordered
    behavior="mobile"
    class="bg-grey-9 text-white"
  >
    <div class="q-pa-md">
      <div class="q-mb-md text-h6">Filter Settings</div>
      <div class="column q-gutter-md">
        <ColorInput v-model="ownColor" label="Own color" dense />
        <DateTimePicker v-model="from" label="From" dense />
        <DateTimePicker v-model="to" label="To" dense />
      </div>
    </div>
  </q-drawer>

  <OtherUsersCard v-model="allowedUsers" @add-user="addUser" />
  <AddUserPopup ref="addUserPopup" @user-added="onUserAdded" />
</template>
<script lang="ts" setup>
//import AmChartsMap from 'src/components/map/AmChartsMap.vue';
import { useLocalStorage } from '@vueuse/core';
import { date, useQuasar } from 'quasar';
import { api } from 'src/boot/axios';
import axios from 'axios';
import DateTimePicker from 'src/components/common/DateTimePicker.vue';
import AddUserPopup from 'src/components/map/AddUserPopup.vue';
import LeafletMap from 'src/components/map/LeafletMap.vue';
import type { DataColors } from 'src/components/models';
import {
  type OtherUserDataAllowance,
  type Datapoint,
  type MapDataEntry,
} from 'src/components/models';
import { useAuthStore } from 'src/stores/authStore';
import { computed, ref, useTemplateRef, watchEffect } from 'vue';
import OtherUsersCard from 'src/components/map/OtherUsersCard.vue';
import ColorInput from 'src/components/common/ColorInput.vue';

const quasar = useQuasar();
const { userId } = useAuthStore();
const allowedUsers = useLocalStorage<OtherUserDataAllowance[]>('allowed-user-data', []);
const ownColor = useLocalStorage<string>('own-data-color', '#FF0000');
const addUserPopup = useTemplateRef('addUserPopup');

const showDrawer = ref(false);
const from = ref<Date>(date.subtractFromDate(Date.now(), { days: 1 }));
const to = ref<Date>();
const mapData = ref<MapDataEntry[]>([]);

const dataColors = computed(() => {
  const colors: DataColors = {};

  colors[userId] = ownColor.value;

  allowedUsers.value.forEach((u) => (colors[u.userId] = u.color));

  return colors;
});

const addUser = () => {
  addUserPopup.value?.showAddUserPopup();
};

const onUserAdded = (user: OtherUserDataAllowance) => {
  allowedUsers.value.push(user);
};

watchEffect((onCleanup) => {
  quasar.loading.show({ delay: 100 });

  const controller = new AbortController();

  onCleanup(() => {
    controller.abort();
  });

  mapData.value = [];

  const promises = [];

  // Read the own data
  promises.push(
    api
      .get<Datapoint[]>(`/data/${userId}`, {
        params: { from: from.value, to: to.value },
        signal: controller.signal,
      })
      .then((axiosResponse) =>
        mapData.value.push({
          data: axiosResponse.data.map((d) => {
            return { lat: d.latitude, lng: d.longitude };
          }),
          userId: userId,
        }),
      )
      .catch((err) => console.error(err)),
  );

  allowedUsers.value
    .map((allowedUser) =>
      axios
        .get<Datapoint[]>(`${process.env.API_BASE_URL}/data/${allowedUser.userId}`, {
          params: { from: from.value, to: to.value },
          signal: controller.signal,
          headers: {
            Authorization: `Bearer ${allowedUser.userReadToken}`,
          },
        })
        .then((axiosResponse) =>
          mapData.value.push({
            data: axiosResponse.data.map((d) => {
              return { lat: d.latitude, lng: d.longitude };
            }),
            userId: allowedUser.userId,
          }),
        )
        .catch((err) => console.error(err)),
    )
    .forEach((p) => promises.push(p));
  Promise.allSettled(promises)
    .catch((err) => console.error(err))
    .finally(() => quasar.loading.hide());
});
</script>
<style scoped>
.footer {
  display: flex;
}
</style>
