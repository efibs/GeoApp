<template>
  <q-card class="other-users-card">
    <q-expansion-item icon="perm_identity" label="Other users">
      <div class="other-users-container">
        <div v-for="user in allowedUsers" :key="user.userId" class="other-user-item">
          <span>{{ user.username }}</span>
          <q-space />
          <q-btn flat :style="{ backgroundColor: user.color }" class="select-color-btn">
            <q-popup-proxy cover transition-show="scale" transition-hide="scale">
              <q-color v-model="user.color" />
            </q-popup-proxy>
          </q-btn>
          <q-btn flat round color="red" icon="delete" @click="() => removeUser(user.userId)" />
        </div>
        <div class="add-container">
          <q-btn flat round class="add-user-data-btn" icon="add" @click="emit('addUser')" />
        </div>
      </div>
    </q-expansion-item>
  </q-card>
</template>
<script lang="ts" setup>
import { type OtherUserDataAllowance } from '../models';

const allowedUsers = defineModel<OtherUserDataAllowance[]>({ required: true });

const emit = defineEmits<{
  (event: 'addUser'): void;
}>();

const removeUser = (userId: string) => {
  allowedUsers.value = allowedUsers.value.filter((user) => user.userId != userId);
};
</script>
<style scoped>
.other-users-card {
  position: fixed;
  right: 10px;
  top: 55px;
  z-index: 999;
}

.other-users-container {
  display: flex;
  flex-direction: column;
}

.other-user-item {
  display: flex;
  padding: 5px;
  align-items: center;
}

.other-user-item > span {
  margin-left: 10px;
}

.add-container {
  display: flex;
  justify-content: end;
  padding: 5px;
}

.select-color-btn {
  width: 32px;
  height: 32px;
  min-width: 0;
  min-height: 0;
  border-radius: 0px;
}
</style>
