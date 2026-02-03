
document.addEventListener('DOMContentLoaded', () => {

    /** @type{HTMLFormElement} */
    const $loginForm = document.getElementById('loginForm');

    // 요소가 없을 경우를 대비한 방어 코드
    if (!$loginForm) return;

    const passwordInput = $loginForm['password'];
    const emailInput = $loginForm['email'];


    // 2. 유효성 검사 함수 (기존 로직 및 모달 호출 훼손 없음)
    function validateLoginForm() {
        const email = $loginForm['email'].value.trim().toLowerCase();
        const password = $loginForm['password'].value;

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        if (!emailRegex.test(email)) {
            openModal("Error", "<p>이메일 형식이 올바르지 않습니다.</p>", {
                confirmText: '확인',
                onConfirm: () => {
                    emailInput.focus();
                    emailInput.select();
                }
            });
            return false;
        }

        if (password.length < 5) {
            openModal("Error", "<p>비밀번호는 6자 이상이어야 합니다.</p>", {
                confirmText: '확인',
                onConfirm: () => {
                    passwordInput.focus();
                    passwordInput.select();
                }
            });
            return false;
        }

        if (password.includes(' ')) {
            openModal("Error", "<p>비밀번호에는 공백을 포함할 수 없습니다.</p>", {
                confirmText: '확인',
                onConfirm: () => {
                    passwordInput.focus();
                    passwordInput.select();
                }
            });
            return false;
        }

        return true;
    }
    $loginForm.addEventListener('submit', (e) => {
        e.preventDefault();

        if (!validateLoginForm()) {
            return;
        }

        const xhr = new XMLHttpRequest();
        const formData = new FormData();
        formData.append('email', $loginForm['email'].value);
        formData.append('password', $loginForm['password'].value);

        xhr.onreadystatechange = () => {
            if (xhr.readyState !== XMLHttpRequest.DONE) {
                return;
            }

            if (xhr.status < 200 || xhr.status >= 400) {
                openModal("FAILURE", `<p>서버 통신 오류가 발생했습니다.<br>잠시 후 다시 시도해주세요.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => {
                        $loginForm['password'].value = '';
                        $loginForm['password'].focus();
                    }
                });
                return;
            }

            const response = JSON.parse(xhr.responseText);
            console.log(response);

            if (response.status === 'SUCCESS' && response.usertype === 'customer') {
                openModal("SUCCESS", "<p>로그인에 성공하였습니다.</p>", {
                    confirmText: '확인',
                    onConfirm: () => {
                        window.location.href = '/main';
                    }
                });
            } else if (response.status === 'SUCCESS' && response.usertype === 'owner') {
                openModal("SUCCESS", "<p>로그인에 성공하였습니다.</p>", {
                    confirmText: '확인',
                    onConfirm: () => {
                        window.location.href = '/owner';
                    }
                });
            }else if (response.status === 'SUCCESS' && response.usertype === 'admin') {
                openModal("SUCCESS", "<p>로그인에 성공하였습니다.</p>", {
                    confirmText: '확인',
                    onConfirm: () => {
                        window.location.href = '/admin';
                    }
                });
            } else {
                const title = "Error";
                const content = `<p>로그인에 실패하였습니다. 잠시 후 다시 시도해 주세요.</p>`;
                const options = {
                    confirmText: '확인',
                    onConfirm: () => {
                        // 실패 시 추가 동작이 필요하다면 여기에 작성
                    }
                };
                openModal(title, content, options);
            }
        };

        xhr.open('POST', '/login');
        xhr.send(formData);
    });


    const loginSection = document.querySelector('.login-layout-container');
    window.addEventListener('scroll', () => {
        const scrollY = window.scrollY;
        const vh = window.innerHeight;

        // 스크롤 0 ~ vh 구간에서만 반응
        const progress = Math.min(scrollY / vh, 1);

        // 아래에서 위로 끌어올림
        loginSection.style.transform = `translateY(${100 - progress * 100}%)`;
    });
});

