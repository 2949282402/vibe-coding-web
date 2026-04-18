import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { ElAlert, ElButton, ElForm, ElFormItem, ElInput, ElLoading, ElOption, ElRadioButton, ElRadioGroup, ElSelect, ElSwitch, ElTable, ElTableColumn } from 'element-plus';
import 'element-plus/es/components/alert/style/css';
import 'element-plus/es/components/button/style/css';
import 'element-plus/es/components/form/style/css';
import 'element-plus/es/components/form-item/style/css';
import 'element-plus/es/components/input/style/css';
import 'element-plus/es/components/loading/style/css';
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/message-box/style/css';
import 'element-plus/es/components/option/style/css';
import 'element-plus/es/components/radio-button/style/css';
import 'element-plus/es/components/radio-group/style/css';
import 'element-plus/es/components/select/style/css';
import 'element-plus/es/components/switch/style/css';
import 'element-plus/es/components/table/style/css';
import 'element-plus/es/components/table-column/style/css';

import App from './App.vue';
import router from './router';
import { usePreferencesStore } from './stores/preferences';
import './styles/global.css';

const app = createApp(App);
const pinia = createPinia();

app.component(ElAlert.name, ElAlert);
app.component(ElButton.name, ElButton);
app.component(ElForm.name, ElForm);
app.component(ElFormItem.name, ElFormItem);
app.component(ElInput.name, ElInput);
app.component(ElOption.name, ElOption);
app.component(ElRadioButton.name, ElRadioButton);
app.component(ElRadioGroup.name, ElRadioGroup);
app.component(ElSelect.name, ElSelect);
app.component(ElSwitch.name, ElSwitch);
app.component(ElTable.name, ElTable);
app.component(ElTableColumn.name, ElTableColumn);
app.directive('loading', ElLoading.directive);

app.use(pinia);
usePreferencesStore(pinia).initialize();
app.use(router);

app.mount('#app');
