<template>
  <q-card class="card">
    <q-card-section>
      <div class="text-h6">Generate a token</div>
    </q-card-section>
    <q-separator dark inset />
    <q-card-section class="input-section">
      <q-checkbox v-model="allSelected">All</q-checkbox>
      <q-scroll-area class="checkboxes">
        <q-checkbox v-model="hasReadDataPermission">Read Data</q-checkbox>
        <q-checkbox v-model="hasWriteDataPermission">Write Data</q-checkbox>
      </q-scroll-area>
      <q-input v-model="tokenExpiryString" filled label="Expiry" style="margin-bottom: 10px" />
      <q-btn @click="generateToken" color="primary" class="gen-btn">Generate</q-btn>
    </q-card-section>
  </q-card>

  <q-dialog v-model="showToken">
    <q-card>
      <q-card-section>
        <div class="text-h6">Your Token</div>
      </q-card-section>

      <q-card-section class="q-pt-none">
        Here is your token. Store it somewhere safe. After closing this popup you will not be able
        to access the token again.<br />
        <br />
        <q-input outlined bottom-slots v-model="token" label="Token" :dense="true" :readonly="true">
          <template v-slot:append>
            <q-btn round dense flat icon="content_copy" @click="copyToken" />
          </template>
        </q-input>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn flat label="OK" color="primary" @click="closeTokenPopup" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script lang="ts" setup>
import { copyToClipboard } from 'quasar';
import { api } from 'src/boot/axios';
import { type GenerateToken, PermissionReadData, PermissionWriteData } from 'src/components/models';
import { useAuthStore } from 'src/stores/authStore';
import { computed, ref } from 'vue';

const { userId } = useAuthStore();

const showToken = ref(false);
const token = ref<string>('');

const tokenExpiryString = ref<string>('30.00:00:00');
const hasReadDataPermission = ref(false);
const hasWriteDataPermission = ref(false);

const allSelected = computed({
  get() {
    if (hasReadDataPermission.value && hasWriteDataPermission.value) {
      return true;
    }

    if (hasReadDataPermission.value == false && hasWriteDataPermission.value == false) {
      return false;
    }

    return null;
  },
  set(newValue) {
    hasReadDataPermission.value = newValue!;
    hasWriteDataPermission.value = newValue!;
  },
});

const generateToken = async () => {
  const nullableUserId = userId;

  if (nullableUserId == null) {
    return;
  }

  const permissions: string[] = [];

  if (hasReadDataPermission.value) {
    permissions.push(PermissionReadData);
  }

  if (hasWriteDataPermission.value) {
    permissions.push(PermissionWriteData);
  }

  const generateToken: GenerateToken = {
    expiry: tokenExpiryString.value,
    permissions: permissions,
  };

  const response = await api.post(`/users/${nullableUserId}/tokens`, generateToken);

  token.value = response.data['token'];

  showToken.value = true;
};

const copyToken = async () => {
  await copyToClipboard(token.value);
};

const closeTokenPopup = () => {
  showToken.value = false;
  token.value = '';
};
</script>
<style scoped>
.input-section {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.checkboxes {
  display: flex;
  flex-grow: 1;
  padding-bottom: 10px;
}

.card {
  width: 600px;
  height: 400px;
  margin: 0;
  position: absolute;
  top: 50%;
  left: 50%;
  -ms-transform: translate(-50%, -50%);
  transform: translate(-50%, -50%);
  filter: drop-shadow(7px 7px 5px rgba(0, 0, 0, 0.6));
  display: flex;
  flex-direction: column;
}

.gen-btn {
  width: 100%;
}
</style>
