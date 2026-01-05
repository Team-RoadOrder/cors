document.addEventListener("DOMContentLoaded", function() {
    // 1. URL 파라미터 확인
    const urlParams = new URLSearchParams(window.location.search);
    const openTab = urlParams.get('open');

    console.log("현재 URL 파라미터 open 값:", openTab); // [디버깅용] 값이 잘 찍히나 확인

    if (openTab) {
        // 2. data-tab 속성으로 요소를 찾습니다. (훨씬 정확함)
        const targetMenu = document.querySelector(`.menu .item[data-tab="${openTab}"]`);

        console.log("찾은 메뉴 요소:", targetMenu); // [디버깅용] 요소를 잘 찾았나 확인

        if (targetMenu) {
            targetMenu.click();
        } else {
            console.warn("해당하는 탭 메뉴를 찾을 수 없습니다: " + openTab);
        }
    } else {
        // 3. 파라미터 없으면 기본값(홈)
        const homeLabel = document.querySelector('label.my');
        if(homeLabel) homeLabel.click();
    }

// 페이지 로딩 시(탭 이동 시) 기본적으로 '대기' 상태만 보이게 초기화
    setTimeout(() => filterStatus('대기'), 50);



});
// [삭제 버튼 클릭 이벤트 위임]
document.addEventListener('click', function(e) {
    // 1. 클릭한 요소가 'delete-btn' 클래스를 가진 요소인지 확인
    if (e.target && e.target.classList.contains('delete-btn')) {
        e.preventDefault();

        // 2. 버튼에 심어둔 data-id 값을 가져옴
        const reservationId = e.target.getAttribute('data-id');

        if (!reservationId) {
            alert("예약 정보를 찾을 수 없습니다.");
            return;
        }

        // 3. 모달창 띄우기 (기존에 쓰시던 openModal 활용)
        openModal("answer", "<p>정말 예약내역을 삭제하시겠습니까?</p>", {
            confirmText: '확인',
            onConfirm: () => {
                // 4. 서버로 삭제 요청 보내기
                deleteReservation(reservationId);
            },
            cancelText: '취소', onCancel: () => {

            }
        });
    }
});

// [삭제 요청 함수]
const deleteReservation = (reservationId) => {
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) return;

        if (xhr.status >= 200 && xhr.status < 300) {
            const response = JSON.parse(xhr.responseText);
            if (response.result === 'SUCCESS') {
                openModal("alert", "<p>예약내역이 삭제되었습니다.</p>", {
                    onConfirm: () => {
                        // 성공 시 탭을 새로고침하여 목록 갱신
                        // 현재 열려있는 탭이 예약 탭이라면 리로드
                        loadTab('reservation', document.querySelector('.menu .item[data-tab="reservation"]'));
                    }
                });
            } else {
                openModal("WARN", "<p>삭제에 실패하였습니다.</p>");
            }
        } else {
            openModal("ERROR", "<p>서버 통신 오류</p>");
        }
    };

    // DELETE 메서드나 POST 메서드로 요청
    xhr.open('DELETE', `/reservation/delete?reservationId=${reservationId}`);
    // 만약 POST만 쓴다면: xhr.open('POST', '/reservation/cancel'); xhr.setRequestHeader...
    xhr.send();
}
/**
 * 탭 내용을 비동기로 불러오는 함수
 * @param menuName : 컨트롤러에 요청할 메뉴 파라미터 값 (orders, profile 등)
 * @param element  : 클릭한 메뉴 요소 (this) - 스타일 활성화용
 */
const loadTab = (menuName, element) => {
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, {confirmText: '확인'});
            return;
        }
        document.getElementById('dynamic-content').innerHTML = xhr.responseText;
        // 2. 메뉴 활성화 스타일 처리 (파란색 글씨 등)
        // 클릭한 요소(element)가 있을 때만 실행
        if (element) {
            // 모든 메뉴의 active 클래스 제거
            var allItems = document.querySelectorAll('.menu .item, .my');
            for (var i = 0; i < allItems.length; i++) {
                allItems[i].classList.remove('active');
            }
            // 클릭한 메뉴에 active 클래스 추가
            element.classList.add('active');
        }

     };
     xhr.open('GET', '/my/tab?menu=' + menuName)
     xhr.send();

}
const filterStatus= (statusType) => {
    const rows = document.querySelectorAll('.res-item-row');
    const noDataMsg = document.getElementById('no-reservation-msg');
    let visibleCount = 0; // 화면에 보이는 아이템 개수 세기

    rows.forEach(row => {
        const itemStatus = row.getAttribute('data-status');
        let isVisible = false;

        if (statusType === '종료') {
            if (itemStatus === '완료' || itemStatus === '취소') {
                isVisible = true;
            }
        } else {
            if (itemStatus === statusType) {
                isVisible = true;
            }
        }

        if (isVisible) {
            // ★ [수정] flex가 아니라 block으로 해야 찌그러지지 않음 (CSS .list가 block이므로)
            row.style.display = 'block';
            visibleCount++;
        } else {
            row.style.display = 'none';
        }
    });

    // ★ [수정] 보이는 아이템이 0개면 "데이터 없음" 메시지를 보여줌
    if (visibleCount === 0) {
        if(noDataMsg) noDataMsg.style.display = 'block';
    } else {
        if(noDataMsg) noDataMsg.style.display = 'none';
    }
}
