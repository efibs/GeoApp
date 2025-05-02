<template>
  <q-card class="card">
    <q-card-section>
      <div class="text-h6">Android App</div>
    </q-card-section>
    <q-card-section class="download-section">
      <a href="android-app/GeoApp.apk" download class="link">
        <img src="icons/favicon-128x128.png" class="app-icon" />
        <div class="download-text">Download</div>
      </a>
    </q-card-section>
  </q-card>
</template>
<style scoped>
.card {
  width: 400px;
  display: flex;
  flex-direction: column;
}
.download-section {
  width: 100%;
  height: 100%;
  justify-content: center;
}

.link {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: start;
  align-items: center;
  padding: 25px;
  text-decoration: none;
}

.app-icon {
  width: 128px;
}

.download-text {
  color: white;
  font-size: larger;
  font-weight: bold;
}
</style>
