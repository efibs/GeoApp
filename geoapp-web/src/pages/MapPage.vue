<template>
  <LeafletMap :map-data="mapData" :colors="dataColors" />

  <!-- Compact footer with toggle button -->
  <q-footer elevated class="bg-grey-8 text-white q-pa-sm row justify-between items-center">
    <q-space />
    <q-btn flat icon="tune" label="Settings" @click="showDrawer = true" />
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
      <div class="q-mb-md text-h6">Settings</div>
      <div class="column q-gutter-md">
        <SectionHeader label="Appearance" />
        <ColorInput v-model="ownColor" label="Own color" dense />
        <NumericInput
          v-model="breakThreshold"
          label="Break at"
          unit="s"
          :min="10"
          :max="100"
          :step="10"
        />

        <SectionHeader label="Time range" />
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
import type { AxiosResponse } from 'axios';
import axios from 'axios';
import DateTimePicker from 'src/components/common/DateTimePicker.vue';
import AddUserPopup from 'src/components/map/AddUserPopup.vue';
import LeafletMap from 'src/components/map/LeafletMap.vue';
import type { DataColors, MapDatapoint } from 'src/components/models';
import {
  type OtherUserDataAllowance,
  type Datapoint,
  type MapDataEntry,
} from 'src/components/models';
import { useAuthStore } from 'src/stores/authStore';
import { computed, ref, useTemplateRef, watchEffect } from 'vue';
import OtherUsersCard from 'src/components/map/OtherUsersCard.vue';
import ColorInput from 'src/components/common/ColorInput.vue';
import NumericInput from 'src/components/common/NumericInput.vue';
import SectionHeader from 'src/components/common/SectionHeader.vue';

const quasar = useQuasar();
const { userId } = useAuthStore();
const allowedUsers = useLocalStorage<OtherUserDataAllowance[]>('allowed-user-data', []);
const ownColor = useLocalStorage<string>('own-data-color', '#FF0000');
const breakThreshold = useLocalStorage<number>('break-line-threshold', 60);
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

const readDataAsync = async (abortController: AbortController) => {
  const requests: { userId: string; promise: Promise<AxiosResponse<Datapoint[]>> }[] = [
    // Add the current users data
    {
      userId,
      promise: api.get<Datapoint[]>(`/data/${userId}`, {
        params: { from: from.value, to: to.value },
        signal: abortController.signal,
      }),
    },
    // Add the allowed users data
    ...allowedUsers.value.map((allowedUser) => ({
      userId: allowedUser.userId,
      promise: axios.get<Datapoint[]>(`${process.env.API_BASE_URL}/data/${allowedUser.userId}`, {
        params: { from: from.value, to: to.value },
        signal: abortController.signal,
        headers: {
          Authorization: `Bearer ${allowedUser.userReadToken}`,
        },
      }),
    })),
  ];

  // Await the responses of all calls
  const responses = await Promise.all(requests.map((r) => r.promise));

  // Map the data
  mapData.value = responses.flatMap((r, idx) =>
    mapGroup(r.data, requests[idx]?.userId ?? '', breakThreshold.value * 1000),
  );
};

const mapGroup = (datapoints: Datapoint[], userId: string, threshold: number): MapDataEntry[] => {
  const groups: MapDataEntry[] = [];
  let currentGroup: MapDatapoint[] = [];

  for (let i = 0; i < datapoints.length; i++) {
    const d = datapoints[i]!;
    const currentTime = new Date(d.timestamp).getTime();

    if (i > 0 && currentTime - new Date(datapoints[i - 1]!.timestamp).getTime() > threshold) {
      // Time gap too large, push current group and start new one

      if (currentGroup.length > 0) {
        groups.push({
          data: currentGroup,
          userId: userId,
        });
      }
      currentGroup = [];
    }

    currentGroup.push({ lat: d.latitude, lng: d.longitude });
  }

  // Push the last group if not empty
  if (currentGroup.length > 0) {
    groups.push({
      data: currentGroup,
      userId: userId,
    });
  }

  return groups;
};

watchEffect((onCleanup) => {
  quasar.loading.show({ delay: 100 });

  const controller = new AbortController();

  onCleanup(() => {
    controller.abort();
  });

  readDataAsync(controller)
    .catch((err) => console.error(err))
    .finally(() => quasar.loading.hide());
});
</script>
<style scoped>
.footer {
  display: flex;
}
</style>
