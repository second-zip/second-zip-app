<script setup>
import { MYPAGE_ACTIVITY_ITEMS } from '@/constants/mypage';
defineProps({ summary: { type: Object, required: true }, loading: { type: Boolean, default: false }, secretaryLabel: { type: String, default: 'AI 비서' }, secretaryImage: { type: String, required: true } });
</script>

<template>
  <section class="activity-card bg-white">
    <h2 class="activity-card__title fw-bold">활동 요약</h2>
    <div class="activity-card__grid d-grid">
      <div v-for="item in MYPAGE_ACTIVITY_ITEMS" :key="item.key" class="activity-card__item text-center" :class="`is-${item.tone}`">
        <strong>{{ loading ? '-' : summary[item.key] }}</strong>
        <span>{{ item.label }}</span>
      </div>
    </div>
    <div class="activity-card__assistant d-flex align-items-center gap-2">
      <img :src="secretaryImage" :alt="`${secretaryLabel} 캐릭터`" /><div><strong>{{ secretaryLabel }}</strong><small class="d-block">현재 AI 비서</small></div>
    </div>
  </section>
</template>

<style scoped>
.activity-card { padding: 20px; border: 1px solid #e4e8f0; border-radius: 20px; box-shadow: 0 4px 12px rgba(35, 58, 99, .06); }
.activity-card__title { margin-bottom: 14px; font-size: 13px; }
.activity-card__grid { grid-template-columns: repeat(4, 1fr); gap: 8px; }
.activity-card__item { padding: 11px 2px 8px; border-radius: 11px; }
.activity-card__item strong { display: block; font-size: 18px; }
.activity-card__item span { display: block; margin-top: 4px; font-size: 9px; }
.is-total { color: #176cf3; background: #e8f1ff; }.is-safe { color: #16b86b; background: #ddf7e9; }
.is-caution { color: #f18a00; background: #fff0c8; }.is-danger { color: #ff3b3b; background: #ffe0e0; }
.activity-card__assistant { margin-top: 14px; padding: 10px 12px; background: #edf3ff; border-radius: 11px; font-size: 12px; }
.activity-card__assistant > img { width: 28px; height: 28px; flex: 0 0 28px; object-fit: contain; object-position: center; }
.activity-card__assistant small { color: #8a94a6; font-size: 9px; }
</style>
