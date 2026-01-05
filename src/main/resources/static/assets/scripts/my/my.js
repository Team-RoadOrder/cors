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
function filterStatus(statusType) {
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
