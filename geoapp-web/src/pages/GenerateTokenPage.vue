<template>
  <div class="container">
    <q-card class="card token-card">
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

        <q-input v-model="tokenExpiryString" filled label="Expiry" style="margin-bottom: 10px">
          <q-tooltip anchor="top left" self="center left"> dd.HH:mm:ss </q-tooltip>
        </q-input>
        <q-btn @click="generateToken" color="primary" class="gen-btn">Generate</q-btn>
      </q-card-section>
    </q-card>

    <q-card v-if="isAdmin" class="card register-token-card">
      <q-card-section>
        <div class="text-h6">Generate a register token</div>
      </q-card-section>
      <q-separator dark inset />
      <q-card-section class="input-section">
        <q-btn @click="generateRegisterToken" color="primary" class="gen-btn">Generate</q-btn>
      </q-card-section>
    </q-card>
  </div>

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
import {
  type JwtToken,
  type GenerateToken,
  PermissionReadData,
  PermissionWriteData,
} from 'src/components/models';
import { useAuthStore } from 'src/stores/authStore';
import { computed, ref } from 'vue';

const { userId, isAdmin } = useAuthStore();

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

  const response = await api.post<JwtToken>(`/users/${nullableUserId}/tokens`, generateToken);

  token.value = response.data.token;

  showToken.value = true;
};

const generateRegisterToken = async () => {
  const response = await api.post<JwtToken>(`/users/tokens/register`);

  token.value = response.data.token;

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
.container {
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: column;
  gap: 15px;
  justify-content: center;
  align-items: center;
  padding: 20px;
  overflow-y: scroll;
}

.input-section {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.checkboxes {
  display: flex;
  flex-grow: 1;
  padding-bottom: 10px;
  min-height: 50px;
}

.card {
  margin: 0;
  filter: drop-shadow(7px 7px 5px rgba(0, 0, 0, 0.6));
  display: flex;
  flex-direction: column;
}

.register-token-card {
  width: 50%;
}

.token-card {
  height: 400px;
  width: 50%;
}

@media screen and (max-width: 600px) {
  .register-token-card {
    width: 75%;
  }

  .token-card {
    height: 400px;
    width: 75%;
  }
}

@media screen and (max-width: 400px) {
  .register-token-card {
    width: 100%;
  }

  .token-card {
    height: 400px;
    width: 100%;
  }
}

.gen-btn {
  width: 100%;
}
</style>
