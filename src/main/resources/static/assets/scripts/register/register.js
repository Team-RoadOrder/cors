const customerRadio = document.getElementById('customer');
const ownerRadio = document.getElementById('owner');
/** @type {HTMLFormElement} */
const customerForm = document.getElementById('customerForm');
/** @type {HTMLFormElement} */
const ownerForm = document.getElementById('ownerForm');
/** @type {HTMLLabelElement} */
const customerLabel = document.querySelector('label[for="customer"]');
const ownerLabel = document.querySelector('label[for="owner"]');
/** @type {HTMLButtonElement} */
$customerEmailSendButton = customerForm.querySelector(':scope > .int-area > [name="emailSendButton"]');
/** @type {HTMLInputElement} */
const $customerEmailInput = customerForm.querySelector('input[name="email"]');
/** @type {HTMLInputElement} */
const $customerCodeInput = customerForm.querySelector('input[name="code"]');
/** @type {HTMLInputElement} */
const $customerSaltInput = customerForm.querySelector('input[name="salt"]');
/** @type {HTMLButtonElement} */
const $verifyButton = customerForm.querySelector('button[name="verifyButton"]');
/** @type {HTMLButtonElement} */
$ownerEmailSendButton = ownerForm.querySelector(':scope > .int-area > [name="emailSendButton"]');
/** @type {HTMLInputElement} */
const $ownerEmailInput = ownerForm.querySelector('input[name="email"]');
/** @type {HTMLInputElement} */
const $ownerCodeInput = ownerForm.querySelector('input[name="code"]');
/** @type {HTMLInputElement} */
const $ownerSaltInput = ownerForm.querySelector('input[name="salt"]');
/** @type {HTMLButtonElement} */
const $ownerVerifyButton = ownerForm.querySelector('button[name="verifyButton"]');
/** @type {HTMLInputElement} */
const $globalSocialId = document.getElementById('globalSocialId');
/** @type {HTMLInputElement} */
const $globalSocialTypeCode = document.getElementById('globalSocialTypeCode');

const socialIdVal = $globalSocialId ? $globalSocialId.value : '';
const socialTypeVal = $globalSocialTypeCode ? $globalSocialTypeCode.value : '';
const isSocialRegister = socialIdVal !== '' && socialTypeVal !== '';

// 타이머 관련 변수
let custTimerInterval;
let ownerTimerInterval;

// 타이머 시작 함수
function startTimer(duration, displayElement, onExpire, type) {
    let remaining = duration;
    updateDisplay(displayElement, remaining);
    
    // 타이머와 재전송 버튼을 담을 컨테이너 생성
    let timerContainerId = type === 'customer' ? 'custTimerContainer' : 'ownerTimerContainer';
    let timerContainer = document.getElementById(timerContainerId);
    let verifyBtn = type === 'customer' ? $verifyButton : $ownerVerifyButton;
    let emailInput = type === 'customer' ? $customerEmailInput : $ownerEmailInput;
    let emailSendBtn = type === 'customer' ? $customerEmailSendButton : $ownerEmailSendButton;
    let form = type === 'customer' ? customerForm : ownerForm;

    if (!timerContainer) {
        timerContainer = document.createElement('div');
        timerContainer.id = timerContainerId;
        timerContainer.className = 'timer-container';
        // "확인하기" 버튼을 포함하는 int-area 다음에 삽입
        verifyBtn.parentElement.after(timerContainer);
    }
    
    // 타이머 표시 요소를 컨테이너로 이동
    if (displayElement && displayElement.parentElement !== timerContainer) {
        timerContainer.appendChild(displayElement);
        displayElement.style.marginRight = '10px'; // 간격 조정
    }

    // 재전송 버튼 생성
    let resendBtnId = type === 'customer' ? 'custResendBtn' : 'ownerResendBtn';
    let resendBtn = document.getElementById(resendBtnId);
    if (!resendBtn) {
        resendBtn = document.createElement('input');
        resendBtn.id = resendBtnId;
        resendBtn.type = 'button';
        resendBtn.value = '재전송';
        resendBtn.className = 'resend-btn-bottom'; // CSS 클래스 적용
        timerContainer.appendChild(resendBtn);
        
        resendBtn.addEventListener('click', () => {
            emailInput.disabled = false;
            form['email'].readOnly = false;
            emailSendBtn.disabled = false;
            emailSendBtn.click();
        });
    }

    const interval = setInterval(() => {
        remaining--;
        updateDisplay(displayElement, remaining);
        
        if (remaining <= 0) {
            clearInterval(interval);
            if (displayElement) displayElement.innerText = "시간 초과";
            if (typeof onExpire === 'function') onExpire();
        }
    }, 1000);
    return interval;
}

function updateDisplay(element, seconds) {
    if (element) {
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        element.innerText = `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    }
}

function stopTimer(interval, displayElement, type) {
    clearInterval(interval);
    let timerContainerId = type === 'customer' ? 'custTimerContainer' : 'ownerTimerContainer';
    const timerContainer = document.getElementById(timerContainerId);
    if (timerContainer) {
        timerContainer.remove();
    }
}

if (isSocialRegister) {
    openModal("SNS-REGISTER", "<p>소셜계정으로 회원가입이 진행됩니다.</p>", { confirmText: '확인',onConfirm: () => {} });
    const $custPw = customerForm.querySelector('input[name="password"]');
    const $custConfirm = customerForm.querySelector('input[name="confirm"]');
    if ($custPw) $custPw.closest('.int-area').style.display = 'none';
    if ($custConfirm) $custConfirm.closest('.int-area').style.display = 'none';

    // 4. 비밀번호 입력창 숨기기 (Owner)
    const $ownerPw = ownerForm.querySelector('input[name="password"]');
    const $ownerConfirm = ownerForm.querySelector('input[name="confirm"]');
    if ($ownerPw) $ownerPw.closest('.int-area').style.display = 'none';
    if ($ownerConfirm) $ownerConfirm.closest('.int-area').style.display = 'none';
}


const autoTabIds = [
    { curr: 'custPhone1', next: 'custPhone2', len: 3 },
    { curr: 'custPhone2', next: 'custPhone3', len: 4 },
    { curr: 'ownerPhone1', next: 'ownerPhone2', len: 3 },
    { curr: 'ownerPhone2', next: 'ownerPhone3', len: 4 }
];

autoTabIds.forEach(obj => {
    const currEl = document.getElementById(obj.curr);
    const nextEl = document.getElementById(obj.next);
    if(currEl && nextEl) {
        currEl.addEventListener('keyup', function() {
            if(this.value.length >= obj.len) {
                nextEl.focus();
            }
        });
    }
});

//해시태그
const selectedStyles = new Set();
const MAX_SELECTION = 2;

const selectedTagsContainer = document.getElementById('selectedTags');
const styleInput = document.getElementById('styleInput');
const hashtagBtns = document.querySelectorAll('.hashtag-btn');

// 해시태그 버튼 클릭 이벤트
hashtagBtns.forEach(btn => {
    btn.addEventListener('click', function() {
        const style = this.dataset.style;

        if (selectedStyles.has(style)) {
            // 이미 선택된 경우 제거
            removeStyle(style);
        } else if (selectedStyles.size < MAX_SELECTION) {
            // 최대 개수 미만이면 추가
            addStyle(style);
        }
    });
});

function addStyle(style) {
    selectedStyles.add(style);
    updateUI();
}

function removeStyle(style) {
    selectedStyles.delete(style);
    updateUI();
}

function updateUI() {
    // 선택된 태그 표시 업데이트
    selectedTagsContainer.innerHTML = '';

    if (selectedStyles.size === 0) {
        selectedTagsContainer.classList.add('empty');
    } else {
        selectedTagsContainer.classList.remove('empty');
        selectedStyles.forEach(style => {
            const tagItem = document.createElement('div');
            tagItem.className = 'tag-item';
            tagItem.innerHTML = `
                        #${style}
                        <span class="remove" data-style="${style}">×</span>
                    `;
            selectedTagsContainer.appendChild(tagItem);

            // 삭제 버튼 이벤트
            tagItem.querySelector('.remove').addEventListener('click', function() {
                removeStyle(this.dataset.style);
            });
        });

    }

    // 버튼 상태 업데이트
    hashtagBtns.forEach(btn => {
        const style = btn.dataset.style;
        if (selectedStyles.has(style)) {
            btn.classList.add('selected');
            btn.disabled = false;
        } else {
            btn.classList.remove('selected');
            // 최대 개수에 도달하면 선택되지 않은 버튼 비활성화
            btn.disabled = selectedStyles.size >= MAX_SELECTION;
        }
    });
    const label = document.querySelector('.style-hashtag-area > label');
    if (selectedStyles.size > 0) {
        label.classList.add('active');
    } else {
        label.classList.remove('active');
    }

    // Hidden input 업데이트
    styleInput.value = Array.from(selectedStyles).join(',');
}

// 초기 상태 설정
updateUI();


$customerEmailSendButton.addEventListener('click', (e) => {
    e.preventDefault();
    const emailValue = customerForm['email'].value;
    const emailRegex = /^(?=.{8,50}$)([\da-zA-Z_.]{4,25})@([\da-z\-]+\.)?([\da-z\-]{2,})\.([a-z]{2,15}\.)?([a-z]{2,3})$/g;

    // 유효성 검사
    if (!emailRegex.test($customerEmailInput.value)) {
        openModal("ValidationError", "<p>유효한 이메일 주소를 입력해 주세요.</p>", { confirmText: '확인',onConfirm: () => {} });
        $customerEmailInput.focus();
        return;
    }
    if (!emailValue) {
        openModal("ValidationError", "<p>이메일을 입력해 주세요</p>", { confirmText: '확인',onConfirm: () => {} });
        return;
    }

    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('email', $customerEmailInput.value);
    formData.append('type','0');

    // [수정됨] 딜레이 없이 즉시 로딩창 표시
    Loading.show("인증 번호를 발송 중입니다...");

    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }

        // [수정됨] 응답을 받자마자 로딩창 닫기 (clearTimeout 제거)
        Loading.hide();

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
                // Loading.hide()가 위에서 이미 실행되었으므로 모달만 뜸
                openModal("WARN",`<p>입력하신 이메일(${$customerEmailInput.value})은 이미 사용 중입니다.</p>`,{confirmText: '확인', onConfirm: () => {
                        $customerEmailInput.focus();
                        $customerEmailInput.select();
                    }});
                break;
            case 'SUCCESS' :
                $customerSaltInput.value = response['salt'];
                $customerEmailInput.disabled = true;
                $customerCodeInput.disabled = false;
                $customerEmailSendButton.disabled = true;
                $verifyButton.disabled = false;
                customerForm['email'].readOnly = true;

                openModal("SUCCESS",`<p>입력하신 이메일(${$customerEmailInput.value})로 인증번호를 전송하였습니다. 인증 번호는 3분간만 유효하니 유의해주세요.</p>`,{confirmText: '확인', onConfirm: () => {
                        $customerCodeInput.focus();
                    }});

                // 타이머 시작
                let timerSpan = document.getElementById('custTimer');
                if (!timerSpan) {
                    timerSpan = document.createElement('span');
                    timerSpan.id = 'custTimer';
                    timerSpan.style.color = 'red';
                    timerSpan.style.fontSize = '12px';
                    // 초기에는 임시 위치에 생성, startTimer에서 컨테이너로 이동됨
                }
                
                clearInterval(custTimerInterval);
                custTimerInterval = startTimer(180, timerSpan, () => {
                    $customerCodeInput.disabled = true;
                    $verifyButton.disabled = true;
                    $customerEmailInput.disabled = false;
                    $customerEmailSendButton.disabled = false;
                    customerForm['email'].readOnly = false;
                    openModal("WARN", "<p>인증 시간이 만료되었습니다. 다시 시도해주세요.</p>", { confirmText: '확인', onConfirm: () => {
                        $customerEmailInput.focus();
                    }});
                }, 'customer');
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
    const code = customerForm['code'].value;
    if( $customerEmailInput.value === '' ) {
        openModal("WARN",`<p>이메일입력이 없습니다.</p>`,{confirmText: '확인', onConfirm: () => {}});
        return;
    }
    if (!/^(\d{6})$/g.test($customerCodeInput.value)) {
        openModal("WARN",`<p>올바른 이메일 인증번호를 입력해주세요.</p>`,{confirmText: '확인', onConfirm: () => {}});
    }
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('email',  $customerEmailInput.value);
    formData.append('code', $customerCodeInput.value);
    formData.append('salt', $customerSaltInput.value);
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
                    $customerCodeInput.focus();
                     isEmailVerified = false;
                    }});
                break;
            case 'FAILURE_EXPIRED' :
                openModal("WARN",`<p>인증세션이 만료되었습니다. 인증절차를 다시 시작해주세요.</p>`,{confirmText: '확인', onConfirm: () => {
                        $customerEmailInput.disabled = false;
                        $customerCodeInput.disabled = true;
                        $customerEmailSendButton.disabled = false;
                        $verifyButton.disabled = true;
                        customerForm['email'].readOnly = false;
                        isEmailVerified = false;
                        stopTimer(custTimerInterval, document.getElementById('custTimer'), 'customer');
                    }});
                break;
            case 'SUCCESS' :
                openModal("SUCCESS",`<p>이메일 인증이 완료되었습니다. 남은 필드들을 채워 회원가입을 완료해주세요.</p>`,{confirmText: '확인', onConfirm: () => {
                       $customerCodeInput.disabled = true;
                       $verifyButton.disabled = true;
                       isEmailVerified = true;
                       customerForm['email'].readOnly = true;
                       customerForm['code'].readOnly = true;
                       stopTimer(custTimerInterval, document.getElementById('custTimer'), 'customer');
                    }});
                break;
            default:
                openModal("WARN",`<p>서버가 알수없는 응답을 반환하였습니다. 잠시후 다시 시도해주세요.</p>`,{confirmText: '확인', onConfirm: () => {}});
        }

    };
     xhr.open('PATCH', '/register/email')
     xhr.send(formData);
});

// 사장님용 이메일 인증번호 발송
$ownerEmailSendButton.addEventListener('click', (e) => {
    e.preventDefault();
    const emailValue = ownerForm['email'].value;
    const emailRegex = /^(?=.{8,50}$)([\da-zA-Z_.]{4,25})@([\da-z\-]+\.)?([\da-z\-]{2,})\.([a-z]{2,15}\.)?([a-z]{2,3})$/g;

    // 유효성 검사
    if (!emailRegex.test($ownerEmailInput.value)) {
        openModal("ValidationError", "<p>유효한 이메일 주소를 입력해 주세요.</p>", { confirmText: '확인', onConfirm: () => {} });
        $ownerEmailInput.focus();
        return;
    }
    if (!emailValue) {
        openModal("ValidationError", "<p>이메일을 입력해 주세요</p>", { confirmText: '확인', onConfirm: () => {} });
        return;
    }

    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('email', $ownerEmailInput.value);
    formData.append('type', '0');

    // [수정됨] 딜레이 없이 즉시 로딩창 표시
    Loading.show("인증 번호를 발송 중입니다...");

    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }

        // [수정됨] 응답을 받자마자 로딩창 닫기 (clearTimeout 제거)
        Loading.hide();

        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR", `<p>요청을 전송하는 도중 오류가 발생하였습니다. 잠시 후 다시 시도해 주세요</p>`, { confirmText: '확인' });
            return;
        }

        const response = JSON.parse(xhr.responseText);
        switch (response.result) {
            case 'FAILURE':
                openModal("WARN", `<p>알수 없는 이유로 실패하였습니다. 잠시 후 다시 시도해 주세요.</p>`, { confirmText: '확인' , onConfirm: () => {}});
                break;
            case 'FAILURE_EMAIL_DUPLICATE':
                // Loading.hide()가 위에서 이미 실행되었으므로 모달만 뜸
                openModal("WARN", `<p>입력하신 이메일(${$ownerEmailInput.value})은 이미 사용 중입니다.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => {
                        $ownerEmailInput.focus();
                        $ownerEmailInput.select();
                    }
                });
                break;
            case 'SUCCESS':
                $ownerSaltInput.value = response['salt'];
                $ownerEmailInput.disabled = true;
                $ownerCodeInput.disabled = false;
                $ownerEmailSendButton.disabled = true;
                $ownerVerifyButton.disabled = false;
                ownerForm['email'].readOnly = true;

                openModal("SUCCESS", `<p>입력하신 이메일(${$ownerEmailInput.value})로 인증번호를 전송하였습니다. 3분간 유효합니다.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => $ownerCodeInput.focus()
                });

                // 타이머 시작
                let timerSpan = document.getElementById('ownerTimer');
                if (!timerSpan) {
                    timerSpan = document.createElement('span');
                    timerSpan.id = 'ownerTimer';
                    timerSpan.style.color = 'red';
                    timerSpan.style.fontSize = '12px';
                    // 초기에는 임시 위치에 생성, startTimer에서 컨테이너로 이동됨
                }
                
                clearInterval(ownerTimerInterval);
                ownerTimerInterval = startTimer(180, timerSpan, () => {
                    $ownerCodeInput.disabled = true;
                    $ownerVerifyButton.disabled = true;
                    $ownerEmailInput.disabled = false;
                    $ownerEmailSendButton.disabled = false;
                    ownerForm['email'].readOnly = false;
                    openModal("WARN", "<p>인증 시간이 만료되었습니다. 다시 시도해주세요.</p>", { confirmText: '확인', onConfirm: () => {
                        $ownerEmailInput.focus();
                    }});
                }, 'owner');
                break;
            default:
                openModal("WARN", `<p>서버가 알수없는 응답을 반환하였습니다.</p>`, { confirmText: '확인' });
        }
    };
    xhr.open('POST', '/register/email');
    xhr.send(formData);
});
// 사장님용 이메일 인증번호 확인
$ownerVerifyButton.addEventListener('click', (e) => {
    e.preventDefault();
    if ($ownerEmailInput.value === '') {
        openModal("WARN", `<p>이메일 입력이 없습니다.</p>`, { confirmText: '확인' });
        return;
    }
    if (!/^(\d{6})$/g.test($ownerCodeInput.value)) {
        openModal("WARN", `<p>올바른 이메일 인증번호 6자리를 입력해주세요.</p>`, { confirmText: '확인' });
        return;
    }

    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('email', $ownerEmailInput.value);
    formData.append('code', $ownerCodeInput.value);
    formData.append('salt', $ownerSaltInput.value);

    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) return;

        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR", `<p>요청 전송 중 오류가 발생하였습니다.</p>`, { confirmText: '확인' });
            isEmailVerified = false;
            return;
        }

        const response = JSON.parse(xhr.responseText);
        switch (response.result) {
            case 'FAILURE':
                openModal("WARN", `<p>인증번호가 올바르지 않습니다.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => {
                        $ownerCodeInput.focus();
                        isEmailVerified = false;
                    }
                });
                break;
            case 'FAILURE_EXPIRED':
                openModal("WARN", `<p>인증세션이 만료되었습니다. 다시 시도해주세요.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => {
                        $ownerEmailInput.disabled = false;
                        $ownerCodeInput.disabled = true;
                        $ownerEmailSendButton.disabled = false;
                        $ownerVerifyButton.disabled = true;
                        ownerForm['email'].readOnly = false;
                        isEmailVerified = false;
                        stopTimer(ownerTimerInterval, document.getElementById('ownerTimer'), 'owner');
                    }
                });
                break;
            case 'SUCCESS':
                openModal("SUCCESS", `<p>이메일 인증이 완료되었습니다.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => {
                        $ownerCodeInput.disabled = true;
                        $ownerVerifyButton.disabled = true;
                        isEmailVerified = true;
                        ownerForm['email'].readOnly = true;
                        ownerForm['code'].readOnly = true;
                        stopTimer(ownerTimerInterval, document.getElementById('ownerTimer'), 'owner');
                    }
                });
                break;
            default:
                openModal("WARN", `<p>서버가 알수없는 응답을 반환하였습니다.</p>`, { confirmText: '확인' });
        }
    };
    xhr.open('PATCH', '/register/email');
    xhr.send(formData);
});

// 라디오 버튼 교체 로직

function updateForm() {
    // 공통 초기화 로직 함수
    const resetTargetForm = ($form, type) => {
        $form.reset();
        isEmailVerified = false;
        selectedStyles.clear();
        updateUI();

        if (type === 'customer') {
            $customerEmailInput.disabled = false;
            $customerEmailInput.readOnly = false;
            $customerEmailSendButton.disabled = false;
            $customerCodeInput.disabled = true;
            $verifyButton.disabled = true;
            stopTimer(custTimerInterval, document.getElementById('custTimer'), 'customer');
        } else {
            $ownerEmailInput.disabled = false;
            $ownerEmailInput.readOnly = false;
            $ownerEmailSendButton.disabled = false;
            $ownerCodeInput.disabled = true;
            $ownerVerifyButton.disabled = true;
            stopTimer(ownerTimerInterval, document.getElementById('ownerTimer'), 'owner');
        }
    };

    if (customerRadio.checked) {
        customerForm.classList.remove('hidden');
        ownerForm.classList.add('hidden');
        customerLabel.classList.add('active');
        ownerLabel.classList.remove('active');

        resetTargetForm(ownerForm, 'owner');
    } else {
        ownerForm.classList.remove('hidden');
        customerForm.classList.add('hidden');
        ownerLabel.classList.add('active');
        customerLabel.classList.remove('active');

        resetTargetForm(customerForm, 'customer');
    }
}

customerRadio.addEventListener('change', updateForm);
ownerRadio.addEventListener('change', updateForm);
/*라디오 버튼을 눌러 옵션 변경시 작성내용 초기화되어야함*/

customerForm.addEventListener('submit',(e) => {
    e.preventDefault();
    const form = customerForm;
    const emailRegex = /^(?=.{8,50}$)([\da-zA-Z_.]{4,25})@([\da-z\-]+\.)?([\da-z\-]{2,})\.([a-z]{2,15}\.)?([a-z]{2,3})$/g;
    const nameRegex = /^[가-힣A-Za-z]{2,10}$/;
    const p1 = customerForm.querySelector('[name="phone1"]').value;
    const p2 = customerForm.querySelector('[name="phone2"]').value;
    const p3 = customerForm.querySelector('[name="phone3"]').value;
    const combinedPhone = p1 + p2 + p3;

    const phoneRegex = /^\d{11}$/;
    const passwordRegex = /^[\da-zA-Z`~!@#$%^&*()\-_=+\[{\]}\\|;:'",<.>\/?]{6,50}$/g;

    const emailValue = customerForm['email'].value;
    if (!emailValue) {
        openModal("ValidationError", "<p>이메일을 입력해 주세요</p>", { confirmText: '확인',onConfirm: () => {} });
        return;
    }

    if (!isEmailVerified) {
        e.preventDefault();
        openModal("ValidationError", "<p>이메일 인증을 완료해 주세요.</p>", { confirmText: '확인' });
        return;
    }
    if (!emailRegex.test(form['email'].value)) {
        openModal("ValidationError", "<p>유효한 이메일 주소를 입력해 주세요.</p>", { confirmText: '확인',onConfirm: () => {} });
        form['email'].focus();
        return;
    }

    if (!nameRegex.test(form['name'].value)) {
        openModal("ValidationError", "<p>이름은 한글 또는 영문으로 2자 이상 10자 이하로 입력해 주세요.</p>", { confirmText: '확인' ,onConfirm: () => {}});
        form['name'].focus();
        return;
    }

    if (!isSocialRegister) {
        const password = form['password'].value;
        if (!passwordRegex.test(password)) {
            openModal("ValidationError", "<p>비밀번호는 영문, 숫자, 특수문자 중 2가지 이상을 포함하여 6자 이상 20자 이하로 입력해야 합니다.</p>", { confirmText: '확인',onConfirm: () => {} });
            form['password'].focus();
            return;
        }

        if (password !== form['confirm'].value) {
            openModal("PasswordError", "<p>비밀번호가 일치하지 않습니다.</p>", { confirmText: '확인' ,onConfirm: () => {}});
            form['confirm'].focus();
            return;
        }
    }
    if (!phoneRegex.test(combinedPhone)) {
        openModal("ValidationError", "<p>유효한 전화번호 11자리(숫자만)를 입력해 주세요.</p>", { confirmText: '확인' ,onConfirm: () => {}});
        form['phone'].focus();
        return;
    }
    if (!form['style'].value.trim()) {
        openModal("ValidationError", "<p>스타일을 입력해주세요. 선호하는 상품을 추천받지못할수도있습니다.</p>", { confirmText: '확인',onConfirm: () => {} });
        form['address'].focus();
        return;
    }
    if (!form['address'].value.trim()) {
        openModal("ValidationError", "<p>주소를 입력해 주세요. '입력하기' 버튼을 눌러 검색할 수 있습니다.</p>", { confirmText: '확인',onConfirm: () => {} });
        form['address'].focus();
        return;
    }

    if (!form['addressDetail'].value.trim()) {
        openModal("ValidationError", "<p>상세 주소를 입력해 주세요.</p>", { confirmText: '확인' ,onConfirm: () => {}});
        form['addressDetail'].focus();
        return;
    }

    if (!form.querySelector('input[name="gender"]:checked')) {
        openModal("ValidationError", "<p>성별을 선택해 주세요.</p>", { confirmText: '확인' ,onConfirm: () => {}});
        return;
    }

    const requiredCheckboxes = form.querySelectorAll('.checkboxes input[type="checkbox"][required]');
    for (const checkbox of requiredCheckboxes) {
        if (!checkbox.checked) {
            openModal("ValidationError", "<p>필수 약관 및 동의 사항에 체크해 주세요.</p>", { confirmText: '확인' ,onConfirm: () => {}});
            checkbox.focus();
            return;
        }
    }
    if(customerForm['password'].value !== customerForm['confirm'].value) {
        openModal("PasswordError",`<p>비밀번호가 같지않습니다</p>`,{confirmText: '확인', onConfirm: () => {}});
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
    formData.append('code', $customerCodeInput.value);
    formData.append('salt', $customerSaltInput.value);
    formData.append('name', customerForm['name'].value);
    formData.append('password', customerForm['password'].value);
    formData.append('style', customerForm['style'].value);
    formData.append('phone', combinedPhone);
    formData.append('address', customerForm['address'].value);
    formData.append('addressDetail', customerForm['addressDetail'].value);
    formData.append('gender', customerForm['gender'].value);
    if (isSocialRegister) {
        formData.append('socialId', socialIdVal);
        formData.append('socialTypeCode', socialTypeVal);
    }
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {

            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR",`<p>요청을 전송하는 도중 오류가 발생하였습니다. 잠시 후 다시 시도해 주세요</p>`,{confirmText: '확인', onConfirm: () => {}});
            return;
        }
        const response = JSON.parse(xhr.responseText);
        switch (response.result) {
            case 'FAILURE' :
                openModal("FAILURE",`<p>회원가입에 실패하였습니다. 잠시후 다시 시도해주세요</p>`,{confirmText: '확인', onConfirm: () => {}});
             break;
            case 'FAILURE_EMAIL_DUPLICATE'   :
                openModal("FAILURE_EMAIL_DUPLICATE",`<p>가입된 이메일이 있습니다. 다른 이메일로 시도해주세요</p>`,{confirmText: '확인', onConfirm: () => {}});
             break;
            case 'SUCCESS' :
                openModal("SUCCESS",`<p>가입에 성공하였습니다.</p>`,{confirmText: '확인', onConfirm: () => {
                    window.location.href = '/login';}});
             break;
             default :
                 openModal("WARN",`<p>서버가 알수없는 응답을 반환하였습니다. 잠시후 다시 시도해주세요.</p>`,{confirmText: '확인', onConfirm: () => {}});
        }

     };
     xhr.open('POST', '/register')
     xhr.send(formData);

})

ownerForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const form = ownerForm;
    const nameRegex = /^[가-힣A-Za-z]{2,10}$/;
    const p1 = ownerForm.querySelector('[name="phone1"]').value;
    const p2 = ownerForm.querySelector('[name="phone2"]').value;
    const p3 = ownerForm.querySelector('[name="phone3"]').value;
    const combinedPhone = p1 + p2 + p3;
    const phoneRegex = /^\d{11}$/;
    const passwordRegex = /^[\da-zA-Z`~!@#$%^&*()\-_=+\[{\]}\\|;:'",<.>\/?]{6,50}$/g;
    const storeNameRegex = /^[가-힣A-Za-z0-9]{2,20}$/;
    const businessNumRegex = /^\d{3}-\d{2}-\d{5}$/;

    if (!/^(?=.{8,50}$)([\da-zA-Z_.]{4,25})@([\da-z\-]+\.)?([\da-z\-]{2,})\.([a-z]{2,15}\.)?([a-z]{2,3})$/g.test(form['email'].value)) {
        openModal("ValidationError", "<p>유효한 이메일 주소를 입력해 주세요.</p>", { confirmText: '확인', onConfirm: () => {
                form['email'].focus();
            } });
        return;
    }
    if (!isEmailVerified) {
        e.preventDefault();
        openModal("ValidationError", "<p>이메일 인증을 완료해 주세요.</p>", { confirmText: '확인' });
        return;
    }

    if (!nameRegex.test(form['name'].value)) {
        openModal("ValidationError", "<p>이름은 한글 또는 영문으로 2자 이상 10자 이하로 입력해 주세요.</p>", { confirmText: '확인' ,onConfirm: () => {
                form['name'].focus();
            }});
        return;
    }

    if (!isSocialRegister) {
        const password = form['password'].value;
        if (!passwordRegex.test(password)) {
            openModal("ValidationError", "<p>비밀번호는 영문, 숫자, 특수문자 중 2가지 이상을 포함하여 6자 이상 20자 이하로 입력해야 합니다.</p>", { confirmText: '확인',onConfirm: () => {} });
            form['password'].focus();
            return;
        }

        if (password !== form['confirm'].value) {
            openModal("PasswordError", "<p>비밀번호가 일치하지 않습니다.</p>", { confirmText: '확인' ,onConfirm: () => {}});
            form['confirm'].focus();
            return;
        }
    }

    if (!phoneRegex.test(combinedPhone)) {
        openModal("ValidationError", "<p>유효한 전화번호 11자리(숫자만)를 입력해 주세요.</p>", { confirmText: '확인' ,onConfirm: () => {
                form['phone'].focus();
            }});
        return;
    }
    if (!storeNameRegex.test(form['storeName'].value)) {
        openModal("ValidationError", "<p>가게 이름은 한글, 영문, 숫자로 2자 이상 20자 이하로 입력해 주세요.</p>", { confirmText: '확인' ,onConfirm: () => {
                form['storeName'].focus();
            }});
        return;
    }

    if (!businessNumRegex.test(form['businessNum'].value)) {
        openModal("ValidationError", "<p>유효한 사업자등록번호 (예: 123-45-67890) 10자리를 형식에 맞게 입력해 주세요.</p>", { confirmText: '확인' ,onConfirm: () => {
                form['businessNum'].focus();
            }});
        return;
    }

    if (!form['address'].value.trim()) {
        openModal("ValidationError", "<p>주소를 입력해 주세요. '입력하기' 버튼을 눌러 검색할 수 있습니다.</p>", { confirmText: '확인',onConfirm: () => {
            form['address'].focus();
            } });
        return;
    }

    if (!form['addressDetail'].value.trim()) {
        openModal("ValidationError", "<p>상세 주소를 입력해 주세요.</p>", { confirmText: '확인' ,onConfirm: () => {
            form['addressDetail'].focus();
            }});
        return;
    }
    const requiredCheckboxes = form.querySelectorAll('.checkboxes input[type="checkbox"][required]');
    for (const checkbox of requiredCheckboxes) {
        if (!checkbox.checked) {
            openModal("ValidationError", "<p>필수 약관 및 동의 사항에 체크해 주세요.</p>", { confirmText: '확인' ,onConfirm: () => {
                    checkbox.focus();
                }});
            return;
        }
    }
    if(ownerForm['password'].value !== ownerForm['confirm'].value) {
        openModal("PasswordError",`<p>비밀번호가 같지않습니다</p>`,{confirmText: '확인', onConfirm: () => {
                ownerForm['confirm'].focus();
            }});
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
    formData.append('code', $ownerCodeInput.value);
    formData.append('salt', $ownerSaltInput.value);
    formData.append('name', ownerForm['name'].value);
    formData.append('storeName', ownerForm['storeName'].value);
    formData.append('businessNum', ownerForm['businessNum'].value);
    formData.append('password', ownerForm['password'].value);
    formData.append('phone',combinedPhone);
    formData.append('address', ownerForm['address'].value);
    formData.append('addressDetail', ownerForm['addressDetail'].value);
    if (isSocialRegister) {
        formData.append('socialId', socialIdVal);
        formData.append('socialTypeCode', socialTypeVal);
    }
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {

            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR",`<p>요청을 전송하는 도중 오류가 발생하였습니다. 잠시 후 다시 시도해 주세요</p>`,{confirmText: '확인', onConfirm: () => {}});
            return;
        }
        const response = JSON.parse(xhr.responseText);
        console.log(response);
        switch (response.result) {
            case 'FAILURE' :
                openModal("FAILURE",`<p>회원가입에 실패하였습니다. 잠시후 다시 시도해주세요</p>`,{confirmText: '확인', onConfirm: () => {}});
                break;
            case 'FAILURE_EMAIL_DUPLICATE'   :
                openModal("FAILURE_EMAIL_DUPLICATE",`<p>가입된 이메일이 있습니다. 다른 이메일로 시도해주세요</p>`,{confirmText: '확인', onConfirm: () => {}});
                break;
            case 'SUCCESS' :
                openModal("SUCCESS",`<p>가입에 성공하였습니다.</p>`,{confirmText: '확인', onConfirm: () => { window.location.href = '/login';}});

                break;
            default :
                openModal("WARN",`<p>서버가 알수없는 응답을 반환하였습니다. 잠시후 다시 시도해주세요.</p>`,{confirmText: '확인', onConfirm: () => {}});
        }
    };
    xhr.open('POST', '/register')
    xhr.send(formData);
})