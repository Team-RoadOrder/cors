
const customerRadio = document.getElementById('customer');
const ownerRadio = document.getElementById('owner');
const customerForm = document.getElementById('customerForm');
const ownerForm = document.getElementById('ownerForm');
const customerLabel = document.querySelector('label[for="customer"]');
const ownerLabel = document.querySelector('label[for="owner"]');

function updateForm() {
    if (customerRadio.checked) {
        customerForm.classList.remove('hidden');
        ownerForm.classList.add('hidden');
        customerLabel.classList.add('active');
        ownerLabel.classList.remove('active');
    } else {
    ownerForm.classList.remove('hidden');
    customerForm.classList.add('hidden');
    ownerLabel.classList.add('active');
    customerLabel.classList.remove('active');
}
}

customerRadio.addEventListener('change', updateForm);
ownerRadio.addEventListener('change', updateForm);

document.querySelectorAll('.int-area input[placeholder=" "]').forEach(input => {
    if (input.value === "") { // 값이 없을 때만
    input.value = " "; // 공백 한 칸을 넣어 :valid 상태 방지
    input.addEventListener('focus', () => {
        if(input.value === " ") input.value = "";
        });
    input.addEventListener('blur', () => {
    if(input.value === "") input.value = " ";
    });
    }
});
