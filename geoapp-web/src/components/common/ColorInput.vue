<template>
  <q-input filled v-model="color" :label="label">
    <template v-slot:append>
      <q-icon name="colorize" class="cursor-pointer">
        <q-popup-proxy cover transition-show="scale" transition-hide="scale">
          <q-color v-model="color" />
        </q-popup-proxy>
      </q-icon>
    </template>
  </q-input>
</template>
<script lang="ts" setup>
const color = defineModel<string>();

const { label } = defineProps<{ label: string }>();
</script>
