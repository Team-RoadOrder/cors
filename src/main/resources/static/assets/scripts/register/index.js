
const customerRadio = document.getElementById('customer');
const ownerRadio = document.getElementById('owner');
/** @type {HTMLFormElement} */
const customerForm = document.getElementById('customerForm');
/** @type {HTMLFormElement} */
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


customerForm.addEventListener('submit',(e) => {
    e.preventDefault();
    // password 랑 confirm같은지 확인하기
    // null 검
    if(customerForm['password'].value !== customerForm['confirm'].value) {
        alert('비밀번호가 일치하지 않습니다.')
        customerForm['confirm'].focus();
        return;
    }
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    const usertypeRadios = document.getElementsByName('usertype');
    let selectedUserType = '';
    for (const radio of usertypeRadios) {
        if (radio.checked) {
            selectedUserType = radio.value;
            break;
        }
    }
    formData.append('usertype', selectedUserType);
    formData.append('email', customerForm['email'].value);
    formData.append('name', customerForm['name'].value);
    formData.append('password', customerForm['password'].value);
    /*formData.append('confirm', customerForm['confirm'].value);*/
    formData.append('phone', customerForm['phone'].value);
    formData.append('address', customerForm['address'].value);
    formData.append('addressDetail', customerForm['addressDetail'].value);
    formData.append('gender', customerForm['gender'].value);
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {

            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            alert('요청을 전송하는 도중 오류가 발생하였습니다. 잠시 후 다시 시도해 주세요.');
            return;
        }
        const response = JSON.parse(xhr.responseText);
        console.log(response);
        if (response.result === 'SUCCESS') {
            alert('회원 등록이 성공적으로 완료되었습니다.');
            window.location.href = '/login';
        } else {
            alert('회원가입에 실패하였습니다. 잠시 후 다시 시도해 주세요.');
        }

     };
     xhr.open('POST', '/register/')
     xhr.send(formData);

})


ownerForm.addEventListener('submit', (e) => {
    e.preventDefault();
    if(ownerForm['password'].value !== ownerForm['confirm'].value) {
        alert('비밀번호가 일치하지 않습니다.')
        customerForm['confirm'].focus();
        return;
    }
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    const usertypeRadios = document.getElementsByName('usertype');
    let selectedUserType = '';
    for (const radio of usertypeRadios) {
        if (radio.checked) {
            selectedUserType = radio.value;
            break;
        }
    }
    formData.append('usertype', selectedUserType);
    formData.append('email', ownerForm['email'].value);
    formData.append('name', ownerForm['name'].value);
    formData.append('storeName', ownerForm['storeName'].value);
    formData.append('businessNum', ownerForm['businessNum'].value);
    formData.append('password', ownerForm['password'].value);
    formData.append('phone', ownerForm['phone'].value);
    formData.append('address', ownerForm['address'].value);
    formData.append('addressDetail', ownerForm['addressDetail'].value);
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {

            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            alert('요청을 전송하는 도중 오류가 발생하였습니다. 잠시 후 다시 시도해 주세요.');
            return;
        }
        const response = JSON.parse(xhr.responseText);
        console.log(response);
        if (response.result === 'SUCCESS') {
            alert('회원 등록이 성공적으로 완료되었습니다.');
            window.location.href = '/login';
        } else {
            alert('회원가입에 실패하였습니다. 잠시 후 다시 시도해 주세요.');
        }

    };
    xhr.open('POST', '/register/')
    xhr.send(formData);
})
