<template>
  <div class="map">
    <l-map ref="map" v-model:zoom="zoom" v-model:center="center" :useGlobalLeaflet="false">
      <l-tile-layer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        layer-type="base"
        name="OpenStreetMap"
      ></l-tile-layer>
      <l-circle-marker
        v-for="point in measurements"
        :key="new Date(point.timestamp).getTime()"
        :lat-lng="[point.latitude, point.longitude]"
        :radius="3"
        color="red"
        fill-color="red"
      />
    </l-map>
  </div>
</template>
<script lang="ts" setup>
import 'leaflet/dist/leaflet.css';
import { LMap, LTileLayer, LCircleMarker } from '@vue-leaflet/vue-leaflet';
import { ref } from 'vue';
import { api } from 'boot/axios';
import { type Datapoint } from '../models';
import { useQuasar } from 'quasar';
import { useAuthStore } from 'src/stores/authStore';
import { useLocalStorage } from '@vueuse/core';

const quasar = useQuasar();
const { userId } = useAuthStore();

const zoom = useLocalStorage('map-zoom', 2);
const center = useLocalStorage('map-center', { lat: 0, lng: 0 });
const measurements = ref<Datapoint[]>([]);

quasar.loading.show({ delay: 400 });
api
  .get<Datapoint[]>(`/data/${userId}`)
  .then((data) => {
    measurements.value = data.data;
  })
  .catch((err) => console.error(err))
  .finally(() => quasar.loading.hide());
</script>
<style scoped>
.map {
  width: 100vw;
  height: 100%;
}
</style>
