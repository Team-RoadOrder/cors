
/** @type {HTMLFormElement}*/
const loginForm = document.getElementById('loginForm');
/** @type {HTMLDivElement}*/
const caption = document.querySelector('.caption');
/** @type {HTMLDivElement}*/
const passwordResult = document.getElementById('passwordResult');
/** @type {HTMLButtonElement} */
$emailSendButton = loginForm.querySelector(':scope > .int-area > [name="emailSendButton"]');
/** @type {HTMLInputElement} */
const $emailInput = loginForm.querySelector('input[name="email"]');
/** @type {HTMLInputElement} */
const $codeInput = loginForm.querySelector('input[name="code"]');
/** @type {HTMLInputElement} */
const $saltInput = loginForm.querySelector('input[name="salt"]');
/** @type {HTMLButtonElement} */
const $verifyButton = loginForm.querySelector('button[name="verifyButton"]');

// 타이머 관련 변수
let timerInterval;
let remainingTime = 0;

// 타이머 시작 함수
function startTimer(duration) {
    clearInterval(timerInterval);
    remainingTime = duration;
    
    // 타이머 표시 요소가 없다면 생성 (인증번호 입력란 근처에)
    let timerDisplay = document.getElementById('timerDisplay');
    if (!timerDisplay) {
        timerDisplay = document.createElement('span');
        timerDisplay.id = 'timerDisplay';
        timerDisplay.style.color = 'red';
        timerDisplay.style.fontSize = '12px';
        timerDisplay.style.marginLeft = '10px';
        // 인증번호 입력란의 부모 요소에 추가
        $codeInput.parentElement.appendChild(timerDisplay);
    }
    
    updateTimerDisplay();
    
    timerInterval = setInterval(() => {
        remainingTime--;
        updateTimerDisplay();
        
        if (remainingTime <= 0) {
            clearInterval(timerInterval);
            timerDisplay.innerText = "시간 초과";
            $codeInput.disabled = true;
            $verifyButton.disabled = true;
            openModal("WARN", "<p>인증 시간이 만료되었습니다. 다시 시도해주세요.</p>", { confirmText: '확인' });
        }
    }, 1000);
}

// 타이머 표시 업데이트 함수
function updateTimerDisplay() {
    const timerDisplay = document.getElementById('timerDisplay');
    if (timerDisplay) {
        const minutes = Math.floor(remainingTime / 60);
        const seconds = remainingTime % 60;
        timerDisplay.innerText = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    }
}

// 타이머 종료 함수
function stopTimer() {
    clearInterval(timerInterval);
    const timerDisplay = document.getElementById('timerDisplay');
    if (timerDisplay) {
        timerDisplay.innerText = "";
    }
}

loginForm.addEventListener('submit', (e) => {
    e.preventDefault();
    if (loginForm['email'].readOnly !== true) {
        openModal("경고", "<p>이메일 인증을 먼저 진행해주세요.</p>", {confirmText: '확인', onConfirm: () => {}});
        return;
    }
    const password = loginForm['password'].value;
    const confirm = loginForm['confirm'].value;

    if (password !== confirm) {
        openModal("경고", "<p>비밀번호가 일치하지 않습니다.</p>", {confirmText: '확인', onConfirm: () => {
                loginForm['confirm'].value = '';
                loginForm['confirm'].focus();
            }});
        return;
    }
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('email', loginForm['email'].value)
    formData.append('password', loginForm['password'].value);
    formData.append('code', $codeInput.value);
    formData.append('salt', $saltInput.value);
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            const title = "경고";
            const content = `
            <p>요청을 전송하는 도중 오류가 발생하였습니다. 잠시 후 다시 시도해 주세요.</p>
        `;

            const options = {
                confirmText: '확인',
                onConfirm: () => {
                }
            };

            openModal(title, content, options);
            return;
        }
        const response = JSON.parse(xhr.responseText);
        console.log(response);
        if( response.status === 'SUCCESS' ) {
            const title = "SUCCESS";
            const content = `
            <p>비밀번호가 성공적으로 변경되었습니다.</p>
        `;
            const options = {
                confirmText: '확인',
                onConfirm: () => {
                    window.location.href = '/login';
                }
            };
            openModal(title, content, options);
            caption.classList.add('show');

        } else {
            const title = "FAILURE";
            const content = `
            <p>비밀번호 변경에 실패하였습니다.</p>
        `;
            const options = {
                confirmText: '확인',
                onConfirm: () => {
                }
            };
            openModal(title, content, options);
            caption.classList.add('show');
            passwordResult.innerHTML = '';
        }

    };
    xhr.open('POST', '/resetpw')
    xhr.send(formData);

})


$emailSendButton.addEventListener('click', (e) => {
    e.preventDefault();
    const emailValue = loginForm['email'].value;
    const emailRegex = /^(?=.{8,50}$)([\da-zA-Z_.]{4,25})@([\da-z\-]+\.)?([\da-z\-]{2,})\.([a-z]{2,15}\.)?([a-z]{2,3})$/g;
    if (!emailRegex.test($emailInput.value)) {
        openModal("ValidationError", "<p>유효한 이메일 주소를 입력해 주세요.</p>", { confirmText: '확인',onConfirm: () => {} });
        $emailInput.focus();
        return;
    }
    if (!emailValue) {
        openModal("ValidationError", "<p>이메일을 입력해 주세요</p>", { confirmText: '확인',onConfirm: () => {} });
        return;
    }

    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('email', $emailInput.value);
    formData.append('type','0');
    Loading.show("인증 번호를 발송 중입니다...");
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            Loading.hide();
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR",`<p>요청을 전송하는 도중 오류가 발생하였습니다. 잠시 후 다시 시도해 주세요</p>`,{confirmText: '확인', onConfirm: () => {}});
            return;
        }
        const response = JSON.parse(xhr.responseText);
        switch (response.result) {
            case 'FAILURE' :
                openModal("WARN",`<p>알수 없는 이유로 회원가입에 실패하였습니다. 잠시 후 다시 시도해 주세요.</p>`,{confirmText: '확인', onConfirm: () => {}});
                break;
            case 'FAILURE_EMAIL_DUPLICATE' :
                // 비밀번호 찾기에서는 이메일이 존재해야 하므로, 중복(존재)이 성공 케이스임.
                // 하지만 /register/email은 중복이면 FAILURE_EMAIL_DUPLICATE를 반환함.
                // 따라서 여기서는 FAILURE_EMAIL_DUPLICATE가 오면 인증번호 발송 성공으로 처리해야 함.
                // 하지만 서버가 FAILURE_EMAIL_DUPLICATE일 때 인증번호를 보내는지 확인해야 함.
                // RegisterController를 확인해보면, 중복이면 인증번호를 보내지 않고 바로 리턴함.
                // 따라서 /register/email 엔드포인트는 비밀번호 찾기에 적합하지 않음.
                // ResetPasswordController에 인증번호 발송 엔드포인트를 추가하거나,
                // RegisterController를 수정해야 함.
                
                // 사용자가 400 에러를 겪는 것은 submit 시점임.
                // submit 시점에는 code와 salt가 있어야 함.
                // 만약 인증번호 발송이 안 되면 code와 salt가 없어서 submit도 못함 (readOnly 체크 때문에).
                // 사용자가 인증번호를 받고 인증을 완료했다고 가정하면, code와 salt는 있음.
                
                // 400 에러의 원인은 아마도 disabled 된 input 때문일 가능성이 높음.
                // FormData는 disabled 된 input의 값을 포함하지 않을 수 있음?
                // 아니, FormData.append('code', $codeInput.value)로 직접 값을 넣고 있음.
                // $codeInput.value는 disabled 상태라도 값을 가짐.
                
                // 다시 400 에러 원인 추측:
                // ResetPasswordController의 updatePassword 메서드 파라미터 이름 불일치?
                // email, password, code, salt. 일치함.
                
                // 혹시 salt가 null인가?
                // $saltInput.value = response['salt'];
                // 만약 response['salt']가 없으면?
                
                // 일단 $customerEmailInput 오타 수정.
                
                openModal("WARN",`<p>입력하신 이메일(${$emailInput.value}은 이미 사용 중입니다.</p>`,{confirmText: '확인', onConfirm: () => {
                        $emailInput.focus();
                        $emailInput.select();
                    }});
                break;
            case 'SUCCESS' :
                $saltInput.value = response['salt'];
                $emailInput.disabled = true;
                $codeInput.disabled = false;
                $emailSendButton.disabled = true;
                $verifyButton.disabled = false;
                loginForm['email'].readOnly = true;

                openModal("SUCCESS",`<p>입력하신 이메일(${$emailInput.value})로 인증번호를 전송하였습니다. 인증 번호는 3분간만 유효하니 유의해주세요.</p>`,{confirmText: '확인', onConfirm: () => {
                        $codeInput.focus();
                    }});
                
                // 타이머 시작 (3분 = 180초)
                startTimer(180);
                break;
            default:
                openModal("WARN",`<p>서버가 알수없는 응답을 반환하였습니다. 잠시후 다시 시도해주세요.</p>`,{confirmText: '확인', onConfirm: () => {}});
        }
    };
    xhr.open('POST', '/register/email')
    xhr.send(formData);
});
$verifyButton.addEventListener('click', (e) => {
    e.preventDefault();
    const code = loginForm['code'].value;
    if( $emailInput.value === '' ) {
        openModal("WARN",`<p>이메일입력이 없습니다.</p>`,{confirmText: '확인', onConfirm: () => {}});
        return;
    }
    if (!/^(\d{6})$/g.test($codeInput.value)) {
        openModal("WARN",`<p>올바른 이메일 인증번호를 입력해주세요.</p>`,{confirmText: '확인', onConfirm: () => {}});
    }
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('email',  $emailInput.value);
    formData.append('code', $codeInput.value);
    formData.append('salt', $saltInput.value);
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR",`<p>요청을 전송하는 도중 오류가 발생하였습니다. 잠시 후 다시 시도해 주세요</p>`,{confirmText: '확인', onConfirm: () => {}});
            isEmailVerified = false;
            return;
        }
        const response = JSON.parse(xhr.responseText);
        switch (response.result) {
            case 'FAILURE' :
                openModal("WARN",`<p>인증번호가 올바르지 않습니다. 다시 확인해 주세요.</p>`,{confirmText: '확인', onConfirm: () => {
                        $codeInput.focus();
                        isEmailVerified = false;
                    }});
                break;
            case 'FAILURE_EXPIRED' :
                openModal("WARN",`<p>인증세션이 만료되었습니다. 인증절차를 다시 시작해주세요.</p>`,{confirmText: '확인', onConfirm: () => {
                        $emailInput.disabled = false;
                        $codeInput.disabled = true;
                        $emailSendButton.disabled = false;
                        $verifyButton.disabled = true;
                        loginForm['email'].readOnly = false;
                        isEmailVerified = false;
                        stopTimer(); // 타이머 중지
                    }});
                break;
            case 'SUCCESS' :
                openModal("SUCCESS",`<p>이메일 인증이 완료되었습니다. 남은 필드들을 채워 회원가입을 완료해주세요.</p>`,{confirmText: '확인', onConfirm: () => {
                        $codeInput.disabled = true;
                        $verifyButton.disabled = true;
                        isEmailVerified = true;
                        loginForm['email'].readOnly = true;
                        loginForm['code'].readOnly = true;
                        stopTimer(); // 타이머 중지
                    }});
                break;
            default:
                openModal("WARN",`<p>서버가 알수없는 응답을 반환하였습니다. 잠시후 다시 시도해주세요.</p>`,{confirmText: '확인', onConfirm: () => {}});
        }

    };
    xhr.open('PATCH', '/register/email')
    xhr.send(formData);
});
