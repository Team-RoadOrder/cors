/**
 * 사이트 관리자 전용 신고 관리 스크립트
 * 업데이트: 클라이언트 사이드 페이지네이션 (5개씩 출력, 화살표 가변 노출)
 */

// [추가] 페이지네이션 관련 전역 변수
let allReports = [];       // 서버에서 받아온 전체 데이터 저장
let currentPage = 1;       // 현재 보고 있는 페이지 번호
const itemsPerPage = 8;    // 한 페이지당 출력할 신고 건수

document.addEventListener("DOMContentLoaded", () => {
    // 1. 페이지 로드 시 즉시 신고 목록 조회
    fetchReportList();
});

// 모달 내 처리를 위한 전역 변수
let currentTarget = { type: null, id: null };

/** [신고 사유 한국어 및 뱃지 스타일 매핑] */
const REASON_CONFIG = {
    'INAPPROPRIATE': { text: '부적절한 내용', class: 'reason-inappropriate' },
    'SPAM': { text: '스팸/광고', class: 'reason-spam' },
    'ABUSE': { text: '욕설/비방', class: 'reason-abuse' },
    'ETC': { text: '기타 사유', class: 'reason-etc' }
};

/** [신고 목록 조회] */
function fetchReportList() {
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) return;
        if (xhr.status >= 200 && xhr.status < 400) {
            // [수정] 데이터를 전역 변수에 저장 후 첫 페이지 렌더링
            allReports = JSON.parse(xhr.responseText);
            currentPage = 1;
            renderPage(currentPage);
        } else {
            console.error("데이터 로드 실패: /item 경로 및 세션 권한 확인 필요");
        }
    };
    xhr.open('GET', '/item/admin/report/list');
    xhr.send();
}

/** [페이지별 데이터 렌더링 - 기존 renderAdminTable 로직 통합] */
function renderPage(page) {
    currentPage = page;
    const tableBody = document.getElementById('tableBody');
    const totalLabel = document.getElementById('totalReports');
    const emptyState = document.getElementById('emptyState');

    // 1. 데이터가 아예 없는 경우 처리
    if (!allReports || allReports.length === 0) {
        if (totalLabel) totalLabel.innerText = '0';
        if (emptyState) emptyState.style.display = 'flex';
        if (tableBody) tableBody.innerHTML = '';
        document.getElementById('paginationContainer').innerHTML = '';
        return;
    }

    // 2. 데이터가 있는 경우 상태 초기화
    if (totalLabel) totalLabel.innerText = allReports.length;
    if (emptyState) emptyState.style.display = 'none';
    tableBody.innerHTML = '';

    // 3. 현재 페이지에 해당하는 데이터만 추출 (slice)
    const startIndex = (page - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const pagedData = allReports.slice(startIndex, endIndex);

    // 4. 추출된 데이터 테이블에 추가
    pagedData.forEach(report => {
        const tr = document.createElement('tr');
        const author = (report.authorEmail && report.authorEmail !== 'unknown') ? report.authorEmail : "삭제된 사용자";
        const content = report.originalContent || "(삭제된 게시물입니다)";
        const reportedDate = report.lastReportedAt ? report.lastReportedAt.replace('T', ' ').substring(0, 16) : '-';
        const config = REASON_CONFIG[report.reasonCode] || { text: "규정 위반", class: 'reason-default' };

        tr.innerHTML = `
            <td class="user-name">${author}</td>
            <td class="content-cell">${content}</td>
            <td class="date">${reportedDate}</td>
            <td><span class="badge ${report.reportCount >= 5 ? 'badge-danger' : 'badge-warning'}">${report.reportCount}건</span></td>
            <td><span class="reason-badge ${config.class}">${config.text}</span></td>
            <td>
                <button class="btn btn-view" onclick='openDetail(${JSON.stringify(report)})'>상세보기</button>
            </td>
        `;
        tableBody.appendChild(tr);
    });

    // 5. 페이지네이션 버튼 생성 호출
    renderPaginationButtons();
}

/** [페이지네이션 버튼 동적 생성 - 화살표 조건부 노출 포함] */
function renderPaginationButtons() {
    const container = document.getElementById('paginationContainer');
    const totalPages = Math.ceil(allReports.length / itemsPerPage);
    container.innerHTML = '';

    if (totalPages <= 1) return; // 1페이지만 있으면 버튼 비표시

    // [이전 버튼] 현재 페이지가 1보다 클 때만 노출
    if (currentPage > 1) {
        const prevBtn = document.createElement('button');
        prevBtn.className = 'page-btn nav-btn';
        prevBtn.innerText = '이전';
        prevBtn.onclick = () => renderPage(currentPage - 1);
        container.appendChild(prevBtn);
    }

    // [숫자 버튼] 전체 페이지 수만큼 반복 생성
    for (let i = 1; i <= totalPages; i++) {
        const pageBtn = document.createElement('button');
        pageBtn.className = `page-btn ${i === currentPage ? 'active' : ''}`;
        pageBtn.innerText = i;
        pageBtn.onclick = () => renderPage(i);
        container.appendChild(pageBtn);
    }

    // [다음 버튼] 현재 페이지가 마지막 페이지보다 작을 때만 노출
    if (currentPage < totalPages) {
        const nextBtn = document.createElement('button');
        nextBtn.className = 'page-btn nav-btn';
        nextBtn.innerText = '다음';
        nextBtn.onclick = () => renderPage(currentPage + 1);
        container.appendChild(nextBtn);
    }
}

/** [상세보기 모달 열기] */
window.openDetail = function(report) {
    currentTarget.type = report.targetType;
    currentTarget.id = report.targetId;

    document.getElementById('modalUserName').innerText = (report.authorEmail && report.authorEmail !== 'unknown') ? report.authorEmail : "삭제된 사용자";
    document.getElementById('modalContent').innerText = report.originalContent || "원본 내용을 확인할 수 없습니다.";

    const createdDate = report.originalCreatedAt ? report.originalCreatedAt.replace('T', ' ').substring(0, 16) : '정보 없음';
    document.getElementById('modalCreatedDate').innerText = createdDate;

    const reportedDate = report.lastReportedAt ? report.lastReportedAt.replace('T', ' ').substring(0, 16) : '-';
    document.getElementById('modalReportedDate').innerText = reportedDate;

    const config = REASON_CONFIG[report.reasonCode] || { text: "사유 미지정", class: 'reason-default' };
    const modalReason = document.getElementById('modalReason');
    modalReason.innerHTML = `<span class="reason-badge ${config.class}">${config.text}</span>`;

    const actionBtn = document.getElementById('modalActionBtn');
    if (actionBtn) {
        actionBtn.innerText = (report.targetType === 'REVIEW') ? '리뷰 삭제' : '댓글 비활성화';
    }

    document.getElementById('detailModal').classList.add('active');
    document.getElementById('detailModal').style.display = 'flex';
};

/** [신고 처리 실행] */
window.executeProcessFromModal = function() {
    const { type, id } = currentTarget;
    if (!type || !id) return;

    window.closeModal();

    const targetName = (type === 'REVIEW') ? "리뷰 삭제" : "댓글 비활성화";
    const detailMsg = (type === 'REVIEW')
        ? "이미지를 포함한 모든 데이터가 영구 삭제됩니다."
        : "댓글이 정책 위반 안내 문구로 치환됩니다.";

    openModal(targetName, `<p>${detailMsg}</p><p>정말 처리를 진행하시겠습니까?</p>`, {
        confirmText: '처리 실행',
        cancelText: '취소',
        onConfirm: () => {
            const xhr = new XMLHttpRequest();
            const formData = new FormData();
            formData.append('targetType', type);
            formData.append('targetId', id);

            xhr.onreadystatechange = () => {
                if (xhr.readyState !== XMLHttpRequest.DONE) return;
                if (xhr.status >= 200 && xhr.status < 400) {
                    const data = JSON.parse(xhr.responseText);
                    if (data.result === 'SUCCESS') {
                        openModal("처리 완료", "<p>정상적으로 완료되었습니다.</p>", {
                            confirmText: '확인',
                            onConfirm: () => { location.reload(); }
                        });
                    }
                }
            };
            xhr.open('POST', '/item/review/report/process');
            xhr.send(formData);
        }
    });
};

/** [신고 유지] */
/** [신고 유지 - 실제 DB 연동 버전] */
window.keepFromModal = function() {
    const { type, id } = currentTarget; // 전역 변수에서 현재 타겟 정보 가져옴
    if (!type || !id) return;

    window.closeModal();

    openModal("신고 유지", "<p>해당 게시물에 문제가 없다고 판단하여 신고 기록을 삭제하시겠습니까?</p>", {
        confirmText: '유지 확정',
        cancelText: '취소',
        onConfirm: () => {
            const xhr = new XMLHttpRequest();
            const formData = new FormData();
            formData.append('targetType', type);
            formData.append('targetId', id);

            xhr.onreadystatechange = () => {
                if (xhr.readyState !== XMLHttpRequest.DONE) return;

                if (xhr.status >= 200 && xhr.status < 400) {
                    const data = JSON.parse(xhr.responseText);
                    if (data.result === 'SUCCESS') {
                        openModal("알림", "<p>신고 기록이 정상적으로 정리되어 리스트에서 제거되었습니다.</p>", {
                            confirmText: '확인',
                            onConfirm: () => {
                                // 페이지 전체 리로드 대신 목록 재조회 가능 (현재는 reload로 처리)
                                location.reload();
                            }
                        });
                    }
                } else {
                    openModal("오류", "<p>처리 중 문제가 발생했습니다.</p>", { confirmText: '확인' });
                }
            };
            // 백엔드 ItemController에 추가한 /keep 엔드포인트 호출
            xhr.open('POST', '/item/review/report/keep');
            xhr.send(formData);
        }
    });
};

/** [모달 닫기] */
window.closeModal = function() {
    const modal = document.getElementById('detailModal');
    if (modal) {
        modal.classList.remove('active');
        modal.style.display = 'none';
    }
};