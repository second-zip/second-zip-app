<script setup>
import { Modal } from 'bootstrap';
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = defineProps({
  item: {
    type: Object,
    default: null,
  },
  open: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['close']);
const modalElement = ref(null);
let modal;

const handleHidden = () => emit('close');
const hideModal = () => modal?.hide();

watch(
  () => props.open,
  (open) => (open ? modal?.show() : modal?.hide()),
);

onMounted(() => {
  modal = new Modal(modalElement.value);
  modalElement.value.addEventListener('hidden.bs.modal', handleHidden);

  if (props.open) modal.show();
});

onBeforeUnmount(() => {
  modalElement.value?.removeEventListener('hidden.bs.modal', handleHidden);
  modal?.dispose();
});
</script>

<template>
  <div
    ref="modalElement"
    class="modal fade checklist-description-modal"
    tabindex="-1"
    aria-labelledby="checklist-description-modal-title"
    aria-hidden="true"
  >
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content">
        <div class="modal-header border-0 pb-0">
          <h2
            id="checklist-description-modal-title"
            class="modal-title fs-6 fw-bold"
          >
            {{ item?.title }}
          </h2>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="닫기"
          ></button>
        </div>

        <div class="modal-body">
          <p class="checklist-description-modal__text mb-0">
            {{ item?.description }}
          </p>
        </div>

        <div class="modal-footer border-0 pt-0">
          <button
            type="button"
            class="checklist-description-modal__confirm btn w-100 fw-semibold"
            @click="hideModal"
          >
            확인
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.checklist-description-modal .modal-dialog {
  max-width: 354px;
  margin-right: auto;
  margin-left: auto;
}

.checklist-description-modal .modal-content {
  overflow: hidden;
  border: 0;
  border-radius: 20px;
}

.checklist-description-modal__text {
  color: var(--black-500);
  font-size: 0.875rem;
  line-height: 1.65;
}

.checklist-description-modal__confirm {
  height: 44px;
  color: white;
  background-color: var(--blue-900);
  border: 0;
  border-radius: 12px;
}

.checklist-description-modal__confirm:hover,
.checklist-description-modal__confirm:focus {
  color: white;
  background-color: var(--blue-700);
}

@media (max-width: 386px) {
  .checklist-description-modal .modal-dialog {
    margin-right: 16px;
    margin-left: 16px;
  }
}
</style>
