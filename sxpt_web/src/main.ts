import { createApp } from 'vue';
import App from './App.vue';
import { router } from './router';
import { authApi } from './services/trainingApi';
import './styles.css';

authApi.cleanupInvalidSession();

createApp(App).use(router).mount('#app');
