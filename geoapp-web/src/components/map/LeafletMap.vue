<template>
  <div class="map">
    <l-map ref="map" v-model:zoom="zoom" v-model:center="center" :useGlobalLeaflet="false">
      <l-tile-layer
        class="leaflet-tile"
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        layer-type="base"
        name="OpenStreetMap"
      />
      <l-polyline :lat-lngs="points" color="red" :weight="2" line-cap="round" line-join="round" />
      <l-circle-marker
        v-for="(point, index) in points"
        :key="index"
        :lat-lng="point"
        :radius="2"
        color="red"
      />
    </l-map>
  </div>
</template>
<script lang="ts" setup>
import 'leaflet/dist/leaflet.css';
import { LMap, LTileLayer, LPolyline, LCircleMarker } from '@vue-leaflet/vue-leaflet';
import { type Datapoint } from '../models';
import { useLocalStorage } from '@vueuse/core';
import { computed } from 'vue';

const { data } = defineProps<{
  data: Datapoint[];
}>();

const zoom = useLocalStorage('map-zoom', 2);
const center = useLocalStorage('map-center', { lat: 0, lng: 0 });

const points = computed(() =>
  data.map((p) => {
    return { lat: p.latitude, lng: p.longitude };
  }),
);
</script>
<style scoped>
.map {
  width: 100vw;
  height: 100%;
}

:deep(.leaflet-tile) {
  filter: brightness(60%) contrast(120%) grayscale(15%);
}
</style>
