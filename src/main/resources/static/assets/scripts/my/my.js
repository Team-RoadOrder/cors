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
