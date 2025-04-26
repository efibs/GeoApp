<template><div class="map" ref="the-map"></div></template>
<script lang="ts" setup>
import * as am5 from '@amcharts/amcharts5';
import * as am5map from '@amcharts/amcharts5/map';
import am5themes_Animated from '@amcharts/amcharts5/themes/Animated';
import am5geodata_worldUltra from '@amcharts/amcharts5-geodata/worldUltra';
import { onBeforeUnmount, onMounted, useTemplateRef } from 'vue';

const mapDiv = useTemplateRef('the-map');
let root: am5.Root;

onMounted(() => {
  root = am5.Root.new(mapDiv.value!);

  root.setThemes([am5themes_Animated.new(root)]);

  const chart = root.container.children.push(
    am5map.MapChart.new(root, {
      projection: am5map.geoOrthographic(),
      panX: 'rotateX',
      panY: 'rotateY',
    }),
  );

  chart.series.push(am5map.MapPolygonSeries.new(root, { geoJSON: am5geodata_worldUltra }));
});

onBeforeUnmount(() => {
  if (root) {
    root.dispose();
  }
});
</script>
<style scoped>
.map {
  width: 100vw;
  height: 100%;
}
</style>
