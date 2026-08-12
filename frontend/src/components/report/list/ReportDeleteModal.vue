<script setup>
import { Modal } from 'bootstrap';
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { REPORT_DELETE_MODAL_TEXT } from '@/constants/report/list';

const props = defineProps({
  report: { type: Object, default: null },
  isDeleting: { type: Boolean, default: false },
  errorMessage: { type: String, default: '' },
});
const emit = defineEmits(['confirm', 'close']);
const modalElement = ref(null);
let modal;

const handleHidden = () => emit('close');

watch(
  () => props.report,
  (report) => (report ? modal?.show() : modal?.hide()),
);

onMounted(() => {
  modal = new Modal(modalElement.value);
  modalElement.value.addEventListener('hidden.bs.modal', handleHidden);
  if (props.report) modal.show();
});

onBeforeUnmount(() => {
  modalElement.value?.removeEventListener('hidden.bs.modal', handleHidden);
  modal?.dispose();
});
</script>

<template>
  <div
    ref="modalElement"
    class="modal fade report-delete-modal"
    tabindex="-1"
    aria-labelledby="report-delete-modal-title"
    aria-hidden="true"
  >
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content rounded-4">
        <div class="modal-header border-0 pb-0">
          <h2 id="report-delete-modal-title" class="modal-title fs-6 fw-bold">
            {{ REPORT_DELETE_MODAL_TEXT.title }}
          </h2>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="닫기"
            :disabled="isDeleting"
          ></button>
        </div>
        <div class="modal-body">
          <p class="mb-0">{{ REPORT_DELETE_MODAL_TEXT.description }}</p>
          <p v-if="errorMessage" class="modal-error mt-2 mb-0" role="alert">
            {{ errorMessage }}
          </p>
        </div>
        <div class="modal-footer border-0 pt-0">
          <button
            type="button"
            class="btn btn-light"
            data-bs-dismiss="modal"
            :disabled="isDeleting"
          >
            {{ REPORT_DELETE_MODAL_TEXT.cancel }}
          </button>
          <button
            type="button"
            class="btn btn-danger"
            :disabled="isDeleting"
            @click="emit('confirm')"
          >
            {{
              isDeleting
                ? REPORT_DELETE_MODAL_TEXT.pending
                : REPORT_DELETE_MODAL_TEXT.confirm
            }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.report-delete-modal .modal-dialog {
  max-width: 354px;
  margin-right: auto;
  margin-left: auto;
}

.modal-error {
  color: var(--red-500);
  font-size: 0.8125rem;
}

@media (max-width: 386px) {
  .report-delete-modal .modal-dialog {
    margin-right: 16px;
    margin-left: 16px;
  }
}
</style>
