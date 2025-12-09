const $reservation = document.getElementById("reservation");


    const $button = $reservation.querySelector(':scope > form >.reservate')
    $button.addEventListener("click", (e) => {
        e.preventDefault();
        dialogHandler.ShowSimpleOk('알림', '예약이 확정되었습니다');
})
// 정말 예약을 확정하시겠습니까 ? 확인 , 취소
    const $cart = $reservation.querySelector(':scope > form >.cart')
    $cart.addEventListener("click", (e) => {
        e.preventDefault();
        dialogHandler.ShowSimpleOk('알림', '장바구니에 추가되었습니다.');

})

    document.addEventListener("DOMContentLoaded", function() {
    flatpickr("#date", {
        enableTime: true,       // 시간 선택 활성화 (datetime-local)
        dateFormat: "Y-m-d H:i", // 날짜 및 시간 형식
        locale: "ko",           // 한국어 설정
        // 여기에 더 많은 커스터마이징 옵션을 추가할 수 있습니다.
        // 예: minDate: "today" (오늘 이전 날짜 선택 불가)
        position: "below"
    });
});
