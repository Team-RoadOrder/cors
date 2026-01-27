
/* 전역 변수로 현재 선택된 행의 정보를 저장합니다. */
let selectedMemberEmail = null;
let selectedMemberName = null;

document.addEventListener("DOMContentLoaded", () => {
    /* --- 추가: [A. 모바일 가이드 오버레이 로직] --- (이 위치로 이동) */
    const overlay = document.getElementById('mobileGuideOverlay');
    const btnTodayClose = document.getElementById('btnTodayClose');
    const btnClose = document.getElementById('btnCloseOverlay');

    //#region : 기존 코드
    // if (overlay && btnTodayClose && btnClose) {
    //     const checkOverlay = () => {
    //         const hideExpiry = localStorage.getItem('mobileGuideHideExpiry');
    //         const now = new Date().getTime();
    //
    //         // 32rem(512px) 이하일 때만 작동
    //         if (window.innerWidth <= 512) {
    //             if (!hideExpiry || now > parseInt(hideExpiry)) {
    //                 overlay.style.display = 'flex';
    //                 document.body.style.overflow = 'hidden';
    //             }
    //         } else {
    //             overlay.style.display = 'none';
    //             document.body.style.overflow = '';
    //         }
    //     };
    //
    //     // 초기 실행
    //     checkOverlay();
    //#endregion
    if (overlay && btnTodayClose && btnClose) {
        /*추가:키워드가있는상태(검색 중)라면 오버레이를 강제로 띄우지 않음*/
        const urlParams = new URLSearchParams(window.location.search);

        const checkOverlay = () => {
            if (urlParams.get('keyword')) return;

            const hideExpiry = localStorage.getItem('mobileGuideHideExpiry');
            const now = new Date().getTime();

            // 32rem(512px) 이하일 때만 작동
            if (window.innerWidth <= 512) {
                if (!hideExpiry || now > parseInt(hideExpiry)) {
                    overlay.style.display = 'flex';
                    document.body.style.overflow = 'hidden';
                }
            } else {
                overlay.style.display = 'none';
                document.body.style.overflow = '';
            }
        };

        // 초기 실행
        checkOverlay();

        // 닫기 버튼
        btnClose.addEventListener('click', () => {
            overlay.style.display = 'none';
            document.body.style.overflow = '';
        });

        // 오늘 하루 그만보기 (24시간)
        btnTodayClose.addEventListener('click', () => {
            const expiryDate = new Date().getTime() + (24 * 60 * 60 * 1000);
            localStorage.setItem('mobileGuideHideExpiry', expiryDate.toString());
            overlay.style.display = 'none';
            document.body.style.overflow = '';
        });

        // 리사이즈 대응
        window.addEventListener('resize', checkOverlay);
    }

    /* --- [1. 커스텀 셀렉트 UI 로직] --- */
    document.addEventListener('click', (e) => {
        const trigger = e.target.closest('.select-trigger');
        const option = e.target.closest('.custom-option');

        if (trigger) {
            const container = trigger.closest('.custom-select-container');
            const hiddenSelect = container.querySelector('.hidden-select');
            if (hiddenSelect && hiddenSelect.disabled) return;
            document.querySelectorAll('.custom-select-container.open').forEach(other => {
                if (other !== container) other.classList.remove('open');
            });
            container.classList.toggle('open');
            e.stopPropagation();
        } else if (option) {
            const container = option.closest('.custom-select-container');
            const hiddenSelect = container.querySelector('.hidden-select');
            const triggerSpan = container.querySelector('.select-trigger span');
            const value = option.getAttribute('data-value');

            triggerSpan.textContent = option.textContent;
            container.querySelectorAll('.custom-option').forEach(opt => opt.classList.remove('selected'));
            option.classList.add('selected');

            if (hiddenSelect) {
                hiddenSelect.value = value;
                if (container.classList.contains('role-select-wrapper')) {
                    const keyword = document.querySelector('.search-input')?.value || '';
                    // level 필터링 시 한글 상호명이 깨지지 않도록 인코딩 처리
                    location.href = `/owner/member?level=${value}&keyword=${encodeURIComponent(keyword)}`;
                }
                hiddenSelect.dispatchEvent(new Event('change'));
            }
            container.classList.remove('open');
        } else {
            document.querySelectorAll('.custom-select-container.open').forEach(c => c.classList.remove('open'));
        }
    });

    const searchInput = document.querySelector('.search-input');

    // 실시간 검색 이벤트
    if (searchInput) {
        searchInput.addEventListener('input', filterTable);

        // 검색창 엔터 시 서버로 넘어가는 기본 동작 방지
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') e.preventDefault();
        });
    }


    /* --- [개선된 기능: 테이블 행 클릭 시 선택 로직] --- */
    const tableBody = document.querySelector('.admin-table tbody');
    if (tableBody) {
        tableBody.addEventListener('click', (e) => {
            // 수정 버튼이나 셀렉트 박스 영역을 클릭한 경우는 행 선택 로직 무시
            if (e.target.closest('.btn-edit') || e.target.closest('.custom-select-container')) return;

            const row = e.target.closest('.member-row');
            if (row) {
                // 기존에 선택된 행들의 강조 효과를 제거
                document.querySelectorAll('.member-row').forEach(r => r.classList.remove('selected'));

                // 현재 클릭한 행에 강조 효과 부여
                row.classList.add('selected');

                // 선택된 행의 정보를 전역 변수에 저장
                selectedMemberEmail = row.getAttribute('data-email');
                selectedMemberName = row.getAttribute('data-name');
            }
        });
    }
});

/**
 * 한글 초성 추출 함수
 */
function getInit(str) {
    const cho = ["ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ","ㅅ","ㅆ","ㅇ","ㅈ","ㅉ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ"];
    let result = "";
    for(let i=0; i<str.length; i++) {
        const code = str.charCodeAt(i) - 44032;
        if(code > -1 && code < 11172) result += cho[Math.floor(code / 588)];
        else result += str.charAt(i);
    }
    return result;
}

//#region:기존 filterTable
/**
 * 실시간 검색 필터링 함수
 */
// function filterTable() {
//     const searchInput = document.querySelector('.search-input');
//     const keyword = searchInput.value.toLowerCase();
//     const rows = document.querySelectorAll('.member-row');
//     const keywordInit = getInit(keyword); // 입력값의 초성
//
//     rows.forEach(row => {
//         const name = row.getAttribute('data-name') || "";
//         const email = row.getAttribute('data-email') || "";
//
//         // 1. 일반 텍스트 포함 여부 (이메일 등)
//         const isMatch = name.toLowerCase().includes(keyword) || email.toLowerCase().includes(keyword);
//
//         // 2. 초성 일치 여부 (김갑수 -> ㄱㄱㅅ)
//         const nameInit = getInit(name);
//         const isInitMatch = nameInit.includes(keywordInit);
//
//         // 결과에 따라 행 표시/숨김
//         if (isMatch || isInitMatch) {
//             row.style.display = "";
//         } else {
//             row.style.display = "none";
//         }
//     });
// }
//#endregion
/**
 * 실시간 검색 필터링 함수 (오류 수정 및 결과 없음 안내 포함)
 */
function filterTable() {
    const searchInput = document.querySelector('.search-input');
    const keyword = searchInput.value.toLowerCase();
    const tableBody = document.querySelector('.admin-table tbody');
    const rows = document.querySelectorAll('.member-row');
    const keywordInit = getInit(keyword); // 입력값의 초성

    let visibleCount = 0; // 현재 화면에 보이는 행의 개수 체크

    // 1. 데이터 행 필터링 (루프 안에서는 검색 결과 카운트만 계산)
    rows.forEach(row => {
        const name = row.getAttribute('data-name') || "";
        const email = row.getAttribute('data-email') || "";

        // 일반 텍스트 및 초성 일치 여부 확인
        const isMatch = name.toLowerCase().includes(keyword) || email.toLowerCase().includes(keyword);
        const nameInit = getInit(name);
        const isInitMatch = nameInit.includes(keywordInit);

        if (isMatch || isInitMatch) {
            row.style.display = ""; // 행 표시
            visibleCount++; // 카운트 증가
        } else {
            row.style.display = "none"; // 행 숨김
        }
    });

    // 2. 검색 결과 없음 처리 (모든 행 검사 후 루프 밖에서 처리)
    let noResultRow = document.getElementById('noResultRow');

    if (visibleCount === 0) {
        // 검색 결과가 하나도 없을 때
        if (!noResultRow) {
            // 안내 행이 없으면 새로 생성
            noResultRow = document.createElement('tr');
            noResultRow.id = 'noResultRow';
            noResultRow.innerHTML = `
                <td colspan="7" style="padding: 6.25rem 0; text-align: center; vertical-align: middle;">
                    <div style="color: #999; font-size: 1rem; display: block;">
                        검색 결과와 일치하는 임직원이 없습니다.
                    </div>
                </td>
            `;
            tableBody.appendChild(noResultRow);
        }
        noResultRow.style.display = "table-row"; // 안내 문구 표시
    } else {
        // 검색 결과가 하나라도 있을 때 안내 행 숨김
        if (noResultRow) {
            noResultRow.style.display = "none";
        }
    }
}

/**
 * 상단 '- 삭제' 버튼 클릭 시 호출되는 함수
 */
function handleDeleteSelected() {
    if (!selectedMemberEmail) {
        openModal("선택 오류", "<p>삭제할 임직원 줄을 먼저 클릭하여 선택해 주세요.</p>", { confirmText: "확인" });
        return;
    }
    // 선택된 정보를 바탕으로 기존 삭제 함수 호출
    deleteMember(selectedMemberEmail, selectedMemberName);
}

/* --- [2. 사원 추가: 서버 응답 완벽 대응,권한 레벨 수정 완료] --- */
function showAddModal() {
    const addContent = `
        <form id="addMemberForm" class="modal-form">
            <div class="modal-input-group" style="margin-bottom: 1rem;">
                <label style="display:block; margin-bottom:0.5rem; font-weight:600;">이메일 계정</label>
                <input type="email" name="email" placeholder="example@email.com" style="width:100%; padding:0.6rem; border:0.0625rem solid #ddd; border-radius:0.25rem;" required>
            </div>
            <div class="modal-input-group" style="margin-bottom: 1rem;">
                <label style="display:block; margin-bottom:0.5rem; font-weight:600;">이름</label>
                <input type="text" name="name" placeholder="실명을 입력하세요" style="width:100%; padding:0.6rem; border:0.0625rem solid #ddd; border-radius:0.25rem;" required>
            </div>
            <div class="modal-input-group" style="margin-bottom: 1rem;">
                <label style="display:block; margin-bottom:0.5rem; font-weight:600;">권한 설정</label>
                <select name="level" id="memberLevel" style="width:100%; padding:0.6rem; border:0.0625rem solid #ddd; border-radius:0.25rem;">
                    <option value="2">중간관리자</option>
                    <option value="1" selected>사원</option> 
                </select>
            </div>
            <div class="modal-input-group" style="margin-bottom: 1rem;">
                <label style="display:block; margin-bottom:0.5rem; font-weight:600;">연락처</label>
                <input type="tel" name="phone" placeholder="010-0000-0000" oninput="autoHyphen(this)" style="width:100%; padding:0.6rem; border:0.0625rem solid #ddd; border-radius:0.25rem;" required>
            </div>
            <input type="hidden" name="address" value="자동지정">
            <input type="hidden" name="addressDetail" value="자동지정">
            <div class="modal-input-group" style="margin-bottom: 1rem;">
                <label style="display:block; margin-bottom:0.5rem; font-weight:600;">초기 비밀번호</label>
                <input type="password" name="password" id="memberPassword"  style="width:100%; padding:0.6rem; border:0.0625rem solid #eee; border-radius:0.25rem; background:#f5f5f5; color:#666;">
            </div>
        </form>
    `;

    openModal("신규 임직원 등록", addContent, {
        confirmText: '등록하기',
        cancelText: '취소',
        onConfirm: () => {
            const form = document.getElementById('addMemberForm');
            if (!form.email.value || !form.name.value || !form.phone.value) {
                openModal("입력 오류", "<p>필수 항목을 모두 입력해주세요.</p>", { confirmText: "확인" });
                return;
            }

            const formData = new FormData();
            formData.append('email', form.email.value);
            formData.append('name', form.name.value.trim());
            formData.append('level', form.level.value);
            formData.append('phone', form.phone.value.replace(/[^0-9]/g, ''));
            formData.append('password', form.password.value);
            formData.append('address', form.address.value);
            formData.append('addressDetail', form.addressDetail.value);
            formData.append('usertype', 'owner');

            const xhr = new XMLHttpRequest();
            xhr.onreadystatechange = () => {
                if (xhr.readyState !== XMLHttpRequest.DONE) return;
                if (xhr.status >= 200 && xhr.status < 400) {
                    const data = JSON.parse(xhr.responseText);
                    const finalStatus = (data.result || data.status || "").toString().toUpperCase();
                    if (finalStatus === 'SUCCESS') {
                        openModal("등록 완료", "<p>신규 임직원이 성공적으로 등록되었습니다.</p>", {
                            confirmText: "확인", onConfirm: () => location.reload()
                        });
                    } else {
                        const errorMsg = (finalStatus === 'FAILURE_EMAIL_DUPLICATE') ? '이미 존재하는 이메일입니다.' : '등록 권한이 없거나 형식이 잘못되었습니다.';
                        openModal("등록 실패", `<p>${errorMsg}</p>`, { confirmText: "확인" });
                    }
                }
            };
            xhr.open('POST', '/owner/member');
            xhr.send(formData);
        }
    });
}

function showEditModal(element) {
    const { email, name, level } = element.dataset;
    const editContent = `
       <form id="editMemberForm" class="modal-form">
            <input type="hidden" name="email" value="${email}">
            <div class="modal-input-group" style="margin-bottom: 1rem;">
                <label style="display:block; margin-bottom:0.5rem; font-weight:600;">이름</label>
                <input type="text" name="name" value="${name}" style="width:100%; padding:0.6rem; border:1px solid #ddd; border-radius:4px;">
            </div>
            <div class="modal-input-group" style="margin-bottom: 1rem;">
                <label style="display:block; margin-bottom:0.5rem; font-weight:600;">권한 변경</label>
                <select name="level" style="width:100%; padding:0.6rem; border:1px solid #ddd; border-radius:4px;">
                    <option value="3" ${level === '3' ? 'selected' : ''}>최고관리자(위임)</option>
                    <option value="2" ${level === '2' ? 'selected' : ''}>관리자</option>
                    <option value="1" ${level === '1' ? 'selected' : ''}>사원</option>
                </select>
            </div>
            <div style="background:#fff5f5; padding:1rem; border-radius:6px; border:1px dashed #feb2b2; margin-top:1rem;">
                <label style="display:block; margin-bottom:0.5rem; font-weight:700; color:#e53e3e;">비밀번호 확인</label>
                <input type="password" name="currentPassword" placeholder="사원 계정 비밀번호" style="width:100%; padding:0.6rem; border:1px solid #feb2b2; border-radius:4px;" required>
            </div>
        </form>
    `;

    openModal("권한그룹", editContent, {
        confirmText: '수정 완료',
        onConfirm: () => {
            const form = document.getElementById('editMemberForm');
            if(!form.currentPassword.value) {
                openModal("인증 필요", "<p >수정을 위해 사원의 비밀번호를 입력해주세요.</p>", { confirmText: "확인" });
                return;
            }

            const formData = new FormData();
            formData.append('email', form.email.value);
            formData.append('name', form.name.value);
            formData.append('level', form.level.value);
            formData.append('currentPassword', form.currentPassword.value);

            const xhr = new XMLHttpRequest();
            xhr.onreadystatechange = () => {
                if (xhr.readyState !== XMLHttpRequest.DONE) return;
                if (xhr.status >= 200 && xhr.status < 400) {
                    const data = JSON.parse(xhr.responseText);
                    const finalStatus = (data.result || data.status || "").toString().toUpperCase();
                    if (finalStatus === 'SUCCESS') {
                        openModal("수정 완료", "<p>정보가 성공적으로 수정되었습니다.</p>", {
                            confirmText: "확인", onConfirm: () => location.reload()
                        });
                    } else {
                        openModal("수정 실패", "<p>비밀번호가 틀렸거나 수정 권한이 없습니다.</p>", { confirmText: "확인" });
                    }
                }
            };
            xhr.open('PATCH', '/owner/member');
            xhr.send(formData);
        }
    });
}

/* --- [4. 사원 삭제: 선택 기반 및 본인 차단 강화 버전] --- */
/**
 * 구성원 삭제 함수
 * @param {string} email
 * @param {string} name
 * @param {string} loginUserEmail
 */
function deleteMember(email, name, loginUserEmail) {
    // 1. 클라이언트 단 본인 삭제 방지
    if (email === loginUserEmail) {
        openModal("삭제 불가", `
            <div style="text-align: center; padding: 0.625rem;">
                <p style="color: #e53e3e; font-weight: bold;">본인 계정은 삭제할 수 없습니다.</p>
                <p style="font-size: 0.9rem; color: #666;">폐업 또는 계정 탈퇴 메뉴를 이용해 주세요.</p>
            </div>`, { confirmText: "확인" });
        return;
    }

    // 2. 관리자 UI 스타일의 모달 컨텐츠 구성
    const modalContent = `
        <div style="margin-top: 0.625rem;">
            <table style="width: 100%; border-collapse: collapse; border: 0.0625rem solid #eee; font-size: 0.95rem;">
                <tr>
                    <th style="width: 35%; background: #f8f9fa; padding: 0.75rem; text-align: left; border-bottom: 0.0625rem solid #eee;">이름</th>
                    <td style="padding: 0.75rem; border-bottom: 0.0625rem solid #eee;">${name}</td>
                </tr>
                <tr>
                    <th style="background: #f8f9fa; padding: 0.75rem; text-align: left; border-bottom: 0.0625rem solid #eee;">이메일</th>
                    <td style="padding: 0.75rem; border-bottom: 0.0625rem solid #eee;">${email}</td>
                </tr>
            </table>
               <p style="margin-top: 0.9375rem; font-size: 0.85rem; color: #666; text-align: center;">
            ※  삭제된 임직원 정보는 복구할 수 없습니다. 삭제를 진행하시겠습니까?
        </p>
        </div>
    `;

    // 3. 모달 오픈 및 삭제 요청
    openModal("구성원 영구 삭제", modalContent, {
        confirmText: '삭제 실행',
        cancelText: '취소',
        onConfirm: () => {
            const xhr = new XMLHttpRequest();
            // 컨트롤러의 DELETE /member 경로와 매칭
            xhr.open('DELETE', `/owner/member?email=${encodeURIComponent(email)}`);

            xhr.onreadystatechange = () => {
                if (xhr.readyState === XMLHttpRequest.DONE) {
                    if (xhr.status >= 200 && xhr.status < 300) {
                        const response = JSON.parse(xhr.responseText);

                        if (response.result === 'SUCCESS') {
                            // 성공 시 즉시 리로드하여 목록 갱신
                            location.reload();
                        } else {
                            // 서비스 단에서 실패 리턴 시 (예: 본인 삭제, 권한 없음 등)
                            let errorMsg = "삭제 처리에 실패했습니다.";
                            if (response.result === 'FAILURE') errorMsg = "권한이 없거나 삭제할 수 없는 대상입니다.";

                            openModal("오류 발생", `<p style="text-align:center;">${errorMsg}</p>`, { confirmText: "확인" });
                        }
                    } else {
                        openModal("서버 오류", "<p style=\"text-align:center;\">서버와 통신 중 오류가 발생했습니다.</p>", { confirmText: "확인" });
                    }
                }
            };
            xhr.send();
        }
    });
}


/**
 * 연락처 하이픈 자동 생성 함수
 * 입력된 값에서 숫자가 아닌 모든 문자(하이픈 등)를 지우고 순수 숫자만 서버로 넘겨줍니다.
 */
function autoHyphen(target) {
    target.value = target.value.replace(/[^0-9]/g, '')
        .replace(/^(\d{2,3})(\d{3,4})(\d{4})$/, `$1-$2-$3`);
}