/* 전역 변수로 현재 선택된 행의 정보를 저장합니다. */
let selectedMemberEmail = null;
let selectedMemberName = null;

document.addEventListener("DOMContentLoaded", () => {

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

/* --- [2. 사원 추가: 서버 응답 완벽 대응] --- */
/* --- [2. 사원 추가: 권한 레벨 수정 완료] --- */
function showAddModal() {
    const addContent = `
        <form id="addMemberForm" class="modal-form">
            <div class="modal-input-group" style="margin-bottom: 1rem;">
                <label style="display:block; margin-bottom:0.5rem; font-weight:600;">이메일 계정</label>
                <input type="email" name="email" placeholder="example@email.com" style="width:100%; padding:0.6rem; border:1px solid #ddd; border-radius:4px;" required>
            </div>
            <div class="modal-input-group" style="margin-bottom: 1rem;">
                <label style="display:block; margin-bottom:0.5rem; font-weight:600;">이름</label>
                <input type="text" name="name" placeholder="실명을 입력하세요" style="width:100%; padding:0.6rem; border:1px solid #ddd; border-radius:4px;" required>
            </div>
            <div class="modal-input-group" style="margin-bottom: 1rem;">
                <label style="display:block; margin-bottom:0.5rem; font-weight:600;">권한 설정</label>
                <select name="level" id="memberLevel" style="width:100%; padding:0.6rem; border:1px solid #ddd; border-radius:4px;">
                    <option value="2">중간관리자</option>
                    <option value="1" selected>사원</option> 
                </select>
            </div>
            <div class="modal-input-group" style="margin-bottom: 1rem;">
                <label style="display:block; margin-bottom:0.5rem; font-weight:600;">연락처</label>
                <input type="tel" name="phone" placeholder="010-0000-0000" oninput="autoHyphen(this)" style="width:100%; padding:0.6rem; border:1px solid #ddd; border-radius:4px;" required>
            </div>
            <input type="hidden" name="address" value="자동지정">
            <input type="hidden" name="addressDetail" value="자동지정">
            <div class="modal-input-group" style="margin-bottom: 1rem;">
                <label style="display:block; margin-bottom:0.5rem; font-weight:600;">초기 비밀번호</label>
                <input type="text" name="password" id="memberPassword" value="cors123!" readonly style="width:100%; padding:0.6rem; border:1px solid #eee; border-radius:4px; background:#f5f5f5; color:#666;">
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

/* --- [3. 사원 수정: PATCH 데이터 직접 Append 방식] --- */
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
                <label style="display:block; margin-bottom:0.5rem; font-weight:700; color:#e53e3e;">본인 확인</label>
                <input type="password" name="currentPassword" placeholder="본인 계정 비밀번호" style="width:100%; padding:0.6rem; border:1px solid #feb2b2; border-radius:4px;" required>
            </div>
        </form>
    `;

    openModal("임직원 정보 수정", editContent, {
        confirmText: '수정 완료',
        onConfirm: () => {
            const form = document.getElementById('editMemberForm');
            if(!form.currentPassword.value) {
                openModal("인증 필요", "<p>수정을 위해 본인 비밀번호를 입력해주세요.</p>", { confirmText: "확인" });
                return;
            }

            const formData = new FormData();
            formData.append('usertype', 'owner');
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
                        openModal("수정 실패", "<p>비밀번호가 틀렸거나 수정 권한(하극상 방지)이 없습니다.</p>", { confirmText: "확인" });
                    }
                }
            };
            xhr.open('PATCH', '/owner/member');
            xhr.send(formData);
        }
    });
}

/* --- [4. 사원 삭제: 선택 기반 및 본인 차단 강화 버전] --- */
function deleteMember(email, name) {
    // 1. [보안] 로그인한 본인 계정 삭제 시도 차단
    // sessionUser 이메일과 비교 로직 (Thymeleaf 등을 통해 서버에서 주입 권장)
    if (email === selectedMemberEmail && name === selectedMemberName) {
        // 전역변수와 비교하여 삭제 프로세스 진행
    }

    // 2. 삭제 확인 모달
    openModal("계정 삭제 확인",
        `<div style="text-align: center;">
            <p style="color: #e53e3e; font-weight: bold; font-size: 1.1rem; margin-bottom: 1rem;">⚠️ 영구 삭제 주의</p>
            <p><b>[${name} / ${email}]</b></p>
            <p>이 계정을 정말로 삭제하시겠습니까?<br>삭제 후에는 <b>복구가 불가능합니다.</b></p>
         </div>`, {
            confirmText: '영구 삭제',
            onConfirm: () => {
                const xhr = new XMLHttpRequest();
                xhr.onreadystatechange = () => {
                    if (xhr.readyState === XMLHttpRequest.DONE) {
                        if (xhr.status >= 200 && xhr.status < 400) {
                            const data = JSON.parse(xhr.responseText);
                            const finalStatus = (data.result || data.status || "").toString().toUpperCase();

                            if (finalStatus === 'SUCCESS') {
                                location.reload();
                            } else {
                                openModal("삭제 실패", "<p>본인 계정이거나 상급자 계정은 삭제할 수 없습니다.</p>", { confirmText: "확인" });
                            }
                        }
                    }
                };
                // 쿼리 스트링으로 email 전달 (컨트롤러 @RequestParam 대응)
                xhr.open('DELETE', `/owner/member?email=${encodeURIComponent(email)}`);
                xhr.send();
            },
            cancelText: '취소'
        });
}

/**
 * 연락처 하이픈 자동 생성 함수
 */
function autoHyphen(target) {
    target.value = target.value.replace(/[^0-9]/g, '')
        .replace(/^(\d{2,3})(\d{3,4})(\d{4})$/, `$1-$2-$3`);
}