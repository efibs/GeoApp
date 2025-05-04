<template>
  <div class="map">
    <l-map ref="map" v-model:zoom="zoom" v-model:center="center" :useGlobalLeaflet="false">
      <l-tile-layer
        class="leaflet-tile"
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        layer-type="base"
        name="OpenStreetMap"
      />
      <l-polyline
        v-for="(mapDataEntry, index) in mapData"
        :key="index"
        :lat-lngs="mapDataEntry.data"
        :color="colors[mapDataEntry.userId] ?? 'black'"
        :weight="2"
        line-cap="round"
        line-join="round"
      />
      <l-circle-marker
        v-for="(point, index) in mapPoints"
        :key="index"
        :lat-lng="point.data"
        :radius="2"
        :color="colors[point.userId] ?? 'black'"
      />
    </l-map>
  </div>
</template>
<script lang="ts" setup>
import 'leaflet/dist/leaflet.css';
import { LMap, LTileLayer, LPolyline, LCircleMarker } from '@vue-leaflet/vue-leaflet';
import type { DataColors, MapPoint } from '../models';
import { type MapDataEntry } from '../models';
import { useLocalStorage } from '@vueuse/core';
import { computed } from 'vue';

const { mapData } = defineProps<{
  mapData: MapDataEntry[];
  colors: DataColors;
}>();

const zoom = useLocalStorage('map-zoom', 2);
const center = useLocalStorage('map-center', { lat: 0, lng: 0 });

const mapPoints = computed<MapPoint[]>(() =>
  mapData
    .flatMap((d) =>
      d.data.map((dp) => {
        return { data: dp, userId: d.userId };
      }),
    )
    .reverse(),
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
