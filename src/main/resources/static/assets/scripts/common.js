
// 헤더 스크롤 했을때 색바꾸기
document.addEventListener('scroll', function() {
    const header = document.getElementById('header');
    if (window.scrollY > 0) {
        header.classList.add('scroll-active');
    } else {
        header.classList.remove('scroll-active');
    }
});
// 검색창에 있는 헤더를 위한
const searchIcon = document.querySelector('.glass');
const searchInput = document.getElementById('searchInput');

// searchIcon과 searchInput이 모두 화면에 있을 때만 이벤트를 연결합니다.
if (searchIcon && searchInput) {
    searchIcon.addEventListener('click', (event) => {
        // searchInput 요소에 'show' 클래스를 추가하거나 제거합니다 (toggle).
        searchInput.classList.toggle('show');

        // 만약 검색창이 보이는 상태가 되었다면, 바로 입력할 수 있도록 focus를 줍니다.
        if (searchInput.classList.contains('show')) {
            searchInput.focus();
        }
    });
}

// =========================================================
// openModal 함수 (애니메이션, duration 옵션 및 클린업 로직 통합)
// =========================================================

/**
 * 사용자 지정 제목, 내용 및 옵션 버튼을 표시하는 모달을 동적으로 생성하고 엽니다.
 *
 * @param {string} title 모달 헤더에 표시될 제목
 * @param {string} content HTML 문자열 형태의 모달 본문 내용
 * @param {object} options (선택) 버튼 및 자동 닫힘 설정을 위한 객체.
 * { confirmText: string, onConfirm: function, cancelText: string, onCancel: function, duration: number } 형식
 */
function openModal(title, content, options = {}) {
    // 1. 기존 모달이 있다면 제거
    const existingModal = document.getElementById('CustomModal');
    if (existingModal) {
        existingModal.remove();
    }

    // 2. ESC 키 리스너 제거 함수 정의 (모달이 닫힐 때 사용)
    const closeOnEscape = (e) => {
        if (e.key === 'Escape') {
            closeModal();
        }
    };

    // 3. 모달 전체 컨테이너 생성 (#CustomModal)
    const modal = document.createElement('div');
    modal.id = 'CustomModal';
    const modalBox = document.createElement('div');
    modalBox.className = 'modal-box';

    // 4. 모달 닫기 로직 정의 (애니메이션 처리 포함)
    const closeModal = () => {
        // 1단계: 애니메이션 시작을 위해 클래스 제거 (Fade-out 시작)
        modal.classList.remove('is-open');

        // ESC 키 리스너 해제 (중요: 다른 곳에서 이미 removeEventListener를 호출했을 수도 있으므로 중복 방지)
        document.removeEventListener('keydown', closeOnEscape);

        // 2단계: CSS 애니메이션(0.3s)이 끝난 후 DOM에서 완전히 제거
        // transitionend 이벤트 리스너를 한 번만 실행되도록 설정
        modal.addEventListener('transitionend', function handler(e) {
            // opacity 트랜지션이 완료되었을 때만 처리 (다른 속성 트랜지션 무시)
            if (e.propertyName === 'opacity') {
                modal.remove();
                modal.removeEventListener('transitionend', handler);
            }
        });
    };

    // 5. 헤더, 내용, 푸터 생성 로직 (이전과 동일)

    // 헤더
    const header = document.createElement('div');
    header.className = 'header';
    const titleElement = document.createElement('span');
    titleElement.textContent = title;
    header.appendChild(titleElement);
    const closeButton = document.createElement('span');
    closeButton.className = 'close-btn';
    closeButton.textContent = '×';
    header.appendChild(closeButton);

    // 내용
    const contentArea = document.createElement('div');
    contentArea.className = 'content';
    contentArea.innerHTML = content;

    modalBox.appendChild(header);
    modalBox.appendChild(contentArea);

    // 푸터 및 버튼
    const hasConfirm = typeof options.onConfirm === 'function';
    const hasCancel = typeof options.onCancel === 'function';

    if (hasConfirm || hasCancel) {
        const footer = document.createElement('div');
        footer.className = 'footer';

        if (hasCancel) {
            const cancelButton = document.createElement('button');
            cancelButton.className = 'modal-btn cancel';
            cancelButton.textContent = options.cancelText || '취소';
            cancelButton.addEventListener('click', () => {
                options.onCancel();
                closeModal(); // 모달 닫기 함수 호출
            });
            footer.appendChild(cancelButton);
        }

        if (hasConfirm) {
            const confirmButton = document.createElement('button');
            confirmButton.className = 'modal-btn confirm';
            confirmButton.textContent = options.confirmText || '확인';
            confirmButton.addEventListener('click', () => {
                options.onConfirm();
                closeModal(); // 모달 닫기 함수 호출
            });
            footer.appendChild(confirmButton);
        }

        modalBox.appendChild(footer);
    }

    // 6. 요소들 조합 및 body에 추가
    modal.appendChild(modalBox);
    document.body.appendChild(modal);

    // 7. 모달 열기 애니메이션 발동
    // DOM에 추가된 후 짧은 지연 시간 후 클래스를 추가하여 애니메이션을 시작
    setTimeout(() => {
        modal.classList.add('is-open');
    }, 10);

    // 8. 닫기 이벤트 리스너 설정 (closeModal 함수 사용)
    closeButton.addEventListener('click', closeModal);

    // 모달 배경 클릭 시
    modal.addEventListener('click', (e) => {
        if (e.target.id === 'CustomModal') {
            closeModal();
        }
    });

    // ESC 키 입력 시 닫기 활성화
    document.addEventListener('keydown', closeOnEscape);

    // 9. 자동 닫힘 (Duration 옵션 처리)
    if (options.duration && typeof options.duration === 'number' && options.duration > 0) {
        setTimeout(closeModal, options.duration);
    }
}


// =========================================================
// 단순화된 모달 사용 예시
// =========================================================

const $projectDetailBtn = document.getElementById('project-detail-btn');
const $deleteAccountBtn = document.getElementById('delete-account-btn');
const $loginErrorBtn = document.getElementById('login-error-btn'); // 새로운 버튼 추가

// 예시 1: 단순 정보 모달 (버튼 없음)
if ($projectDetailBtn) {
    $projectDetailBtn.addEventListener('click', () => {
        const title = "프로젝트 A - 상세 정보";
        const content = `
            <h3>프로젝트 개요</h3>
            <p>React를 활용한 최신 웹 서비스 개발 프로젝트입니다.</p>
            <ul>
                <li>사용 기술: React, Node.js, MongoDB</li>
                <li>배포 환경: AWS S3</li>
            </ul>
        `;
        openModal(title, content);
    });
}


// 예시 2: 확인/취소 모달 (버튼 있음)
if ($deleteAccountBtn) {
    $deleteAccountBtn.addEventListener('click', () => {
        const title = "❗계정 삭제 확인";
        const content = `
            <p style="color: red; font-weight: bold;">정말로 계정을 삭제하시겠습니까?</p>
            <p>삭제된 계정 정보는 복구할 수 없습니다.</p>
        `;

        const options = {
            confirmText: '영구 삭제',
            onConfirm: () => {
                alert("계정 삭제를 진행합니다!");
                // 실제 계정 삭제 API 호출 로직
            },
            cancelText: '취소 (유지)',
            onCancel: () => {
                console.log("계정 삭제 취소됨");
            }
        };

        openModal(title, content, options);
    });
}

// 예시 3: 자동 닫힘 모달 (duration 옵션 사용)
if ($loginErrorBtn) {
    $loginErrorBtn.addEventListener('click', () => {
        const title = "로그인 오류";
        const content = `<p>아이디 또는 비밀번호가 일치하지 않습니다.</p><p>3초 후 자동으로 닫힙니다.</p>`;

        const options = {
            duration: 3000 // 3초 후 자동 닫힘
        };

        openModal(title, content, options);
    });
}

// 로딩영역

const Loading = {
    show: function(msg) {
        let el = document.querySelector('[data-object="loading"]');
        if (!el) {
            el = document.createElement('div');
            el.setAttribute('data-object', 'loading');
            el.innerHTML = `
                <div data-component="loading.icon"></div>
                <div data-component="loading.message"></div>
            `;
            document.body.appendChild(el);
        }

        const msgEl = el.querySelector('[data-component~="loading.message"]');
        if (msg) {
            msgEl.innerText = msg;
        } else {
            msgEl.innerText = ""; // CSS의 ::before 내용이 나올 수 있게 비움
        }

        // 렌더링 동기화를 위해 setTimeout 사용
        setTimeout(() => el.setAttribute('data-visible', ''), 10);
    },

    hide: function() {
        const el = document.querySelector('[data-object="loading"]');
        if (el) {
            el.removeAttribute('data-visible');
        }
    }
};

///‼️‼️‼️‼️ 아래에 사용 예시있음 ‼️‼️‼️‼️‼️

/*

// 이메일 인증 버튼 클릭 시 예시
$ownerEmailSendButton.addEventListener('click', (e) => {
    e.preventDefault();

    // 1. 로딩창 띄우기 (사용자 클릭 방지 및 상태 알림)
    Loading.show("인증 번호를 발송 중입니다...");

    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/register/email-code');

    xhr.onreadystatechange = () => {
        if (xhr.readyState === XMLHttpRequest.DONE) {
            // 2. 서버 응답이 오면 결과(성공/실패)에 상관없이 로딩을 숨김
            Loading.hide();

            if (xhr.status >= 200 && xhr.status < 400) {
                const response = JSON.parse(xhr.responseText);
                // 모달 띄우기 등 후속 처리...
            }
        }
    };
    xhr.send(formData);
});*/
