<template>
  <q-item clickable tag="a" target="_blank" @click="onClick">
    <q-item-section v-if="icon" avatar>
      <q-icon :name="icon" />
    </q-item-section>

    <q-item-section>
      <q-item-label>{{ title }}</q-item-label>
      <q-item-label caption>{{ caption }}</q-item-label>
    </q-item-section>
  </q-item>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

export interface EssentialLinkProps {
  title: string;
  route: string;
  caption?: string;
  icon?: string;
}

const props = withDefaults(defineProps<EssentialLinkProps>(), {
  caption: '',
  icon: '',
});

const onClick = async () => {
  await router.push(props.route);
};
</script>
