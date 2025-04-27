<template><div class="map" ref="the-map"></div></template>
<script lang="ts" setup>
import * as am5 from '@amcharts/amcharts5';
import * as am5map from '@amcharts/amcharts5/map';
import am5themes_Animated from '@amcharts/amcharts5/themes/Animated';
import am5geodata_worldUltra from '@amcharts/amcharts5-geodata/worldUltra';
import { onBeforeUnmount, onMounted, useTemplateRef } from 'vue';
import { useQuasar } from 'quasar';
import { api } from 'boot/axios';
import { type Datapoint } from 'src/components/models';
import { useAuthStore } from 'src/stores/authStore';

const quasar = useQuasar();
const mapDiv = useTemplateRef('the-map');
const { userId } = useAuthStore();

let root: am5.Root;

quasar.loading.show({ delay: 400 });

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

  api
    .get<Datapoint[]>(`/data/${userId}`)
    .then((data) => {
      const pointSeries = chart.series.push(
        am5map.MapPointSeries.new(root, {
          latitudeField: 'lat',
          longitudeField: 'long',
        }),
      );

      pointSeries.bullets.push(function () {
        return am5.Bullet.new(root, {
          sprite: am5.Circle.new(root, {
            radius: 3,
            fill: am5.color(0xff0000),
          }),
        });
      });

      const mapPoints = data.data.map((datapoint) => {
        return {
          lat: datapoint.latitude,
          long: datapoint.longitude,
        };
      });

      pointSeries.data.setAll(mapPoints);
    })
    .catch((err) => console.error(err))
    .finally(() => quasar.loading.hide());
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
