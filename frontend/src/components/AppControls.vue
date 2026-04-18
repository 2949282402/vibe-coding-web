<script setup>
import { computed } from 'vue';
import { usePreferencesStore } from '../stores/preferences';

const preferences = usePreferencesStore();

const themeOptions = computed(() => [
  { value: 'dark', label: preferences.t('controls.themeDark') },
  { value: 'light', label: preferences.t('controls.themeLight') }
]);

const localeOptions = computed(() => [
  { value: 'zh-CN', label: preferences.t('controls.localeZh') },
  { value: 'en-US', label: preferences.t('controls.localeEn') }
]);
</script>

<template>
  <div class="app-controls">
    <div class="control-group">
      <span class="control-caption">{{ preferences.t('controls.themeCaption') }}</span>
      <div class="control-segment" :title="preferences.t('controls.switchTheme')">
        <button
          v-for="option in themeOptions"
          :key="option.value"
          type="button"
          class="segment-button"
          :class="{ active: preferences.theme === option.value }"
          @click="preferences.setTheme(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </div>

    <div class="control-group">
      <span class="control-caption">{{ preferences.t('controls.languageCaption') }}</span>
      <div class="control-segment" :title="preferences.t('controls.switchLanguage')">
        <button
          v-for="option in localeOptions"
          :key="option.value"
          type="button"
          class="segment-button"
          :class="{ active: preferences.locale === option.value }"
          @click="preferences.setLocale(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.control-caption {
  color: var(--text-muted);
  font-size: 0.72rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.control-segment {
  display: inline-flex;
  align-items: center;
  padding: 4px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background:
    linear-gradient(180deg, var(--bg-panel-strong), var(--bg-panel)),
    var(--bg-elevated);
  box-shadow: inset 0 1px 0 var(--glow-soft);
}

.segment-button {
  min-width: 62px;
  padding: 8px 12px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.88rem;
  cursor: pointer;
  transition: 0.2s ease;
}

.segment-button:hover {
  color: var(--text-main);
}

.segment-button.active {
  background: var(--control-bg);
  color: var(--text-main);
  box-shadow:
    inset 0 1px 0 var(--glow-soft),
    0 8px 18px rgba(0, 0, 0, 0.16);
}

@media (max-width: 720px) {
  .app-controls {
    width: 100%;
  }

  .control-group {
    flex: 1 1 180px;
  }

  .control-segment {
    width: 100%;
  }

  .segment-button {
    flex: 1 1 0;
  }
}
</style>
