import { Modal } from 'bootstrap';
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';

export const useBootstrapModal = (open, onClose) => {
  const modalElement = ref(null);
  let modal;

  const hide = () => modal?.hide();
  const handleHidden = () => onClose();

  watch(open, (value) => (value ? modal?.show() : modal?.hide()));

  onMounted(() => {
    modal = new Modal(modalElement.value);
    modalElement.value.addEventListener('hidden.bs.modal', handleHidden);
    if (open.value) modal.show();
  });

  onBeforeUnmount(() => {
    modalElement.value?.removeEventListener('hidden.bs.modal', handleHidden);
    modal?.dispose();
  });

  return { hide, modalElement };
};
