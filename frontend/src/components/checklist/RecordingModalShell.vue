<script setup>
import { toRef } from 'vue';

import { useBootstrapModal } from '@/composables/useBootstrapModal';

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, required: true },
  titleId: { type: String, required: true },
});
const emit = defineEmits(['close']);
const { modalElement } = useBootstrapModal(
  toRef(props, 'open'),
  () => emit('close'),
);
</script>

<template>
  <div
    ref="modalElement"
    class="modal fade recording-modal"
    tabindex="-1"
    :aria-labelledby="titleId"
    aria-hidden="true"
  >
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content rounded-4">
        <div class="modal-header border-0 pb-0">
          <h2 :id="titleId" class="modal-title fs-6 fw-bold">{{ title }}</h2>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="닫기"
          ></button>
        </div>
        <div class="modal-body"><slot /></div>
        <div class="modal-footer border-0 pt-0"><slot name="footer" /></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recording-modal .modal-dialog {
  max-width: 354px;
  margin-right: auto;
  margin-left: auto;
}

@media (max-width: 386px) {
  .recording-modal .modal-dialog {
    margin-right: 16px;
    margin-left: 16px;
  }
}
</style>
