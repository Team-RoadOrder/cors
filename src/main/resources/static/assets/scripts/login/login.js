/**@type{HTMLFormElement}*/
$loginForm = document.getElementById('loginForm');
const passwordInput = $loginForm['password'];
const emailInput = $loginForm['email'];
const capsLockWarning = document.getElementById('capsLockWarning');

passwordInput.addEventListener('keyup', (e) => {
    if (e.getModifierState && e.getModifierState('CapsLock')) {
        capsLockWarning.style.display = 'block';
    } else {
        capsLockWarning.style.display = 'none';
    }
});

function validateLoginForm() {
    const email = $loginForm['email'].value.trim().toLowerCase();
    const password = $loginForm['password'].value;

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!emailRegex.test(email)) {
        openModal("Error", "<p>이메일 형식이 올바르지 않습니다.</p>", { confirmText: '확인' ,onConfirm: () => {
                emailInput.focus();
                emailInput.select();
            }});
        return false;
    }

    if (password.length < 5) {
        openModal("Error", "<p>비밀번호는 6자 이상이어야 합니다.</p>", { confirmText: '확인' ,onConfirm: () => {
            passwordInput.focus();
            passwordInput.select();
            }});
        return false;
    }

    if (password.includes(' ')) {
        openModal("Error", "<p>비밀번호에는 공백을 포함할 수 없습니다.</p>", { confirmText: '확인' ,onConfirm: () => {
            passwordInput.focus();
            passwordInput.select();
            }});
        return false;
    }

    return true;
}
$loginForm.addEventListener('submit', (e)=> {
    e.preventDefault();
    if (!validateLoginForm()) {
        return;
    }
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('email', $loginForm['email'].value);
    formData.append('password', $loginForm['password'].value);
    xhr.onreadystatechange = () => {
        // 여기에 이메일이나 비밀번호 필터링 로직작성

        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            alert('요청을 전송하는 도중 오류가 발생하였습니다. 잠시 후 다시 시도해 주세요.');

            return;
        }
        const response = JSON.parse(xhr.responseText);
        console.log(response);
        if( response.status === 'SUCCESS' && response.usertype === 'customer' ) {
            openModal("SUCCESS", "<p>로그인에 성공하였습니다.</p>", { confirmText: '확인', onConfirm: () => {

                    window.location.href = '/main';
                }});

        } else if(response.status === 'SUCCESS' && response.usertype === 'owner'){
            openModal("SUCCESS", "<p>로그인에 성공하였습니다.</p>", { confirmText: '확인', onConfirm: () => {
                    window.location.href = '/owner';
                }});


        } else {
            const title ="Error";
            const content = `<p>로그인에 실패하였습니다. 잠시 후 다시 시도해 주세요.</p>`;
            const options = {
                confirmText: '확인',
                onConfirm: () => {

                }
            }
            openModal(title,content,options);
        }

     };
     xhr.open('POST', '/login')
     xhr.send(formData);

})
// 모달창 수정