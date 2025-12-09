// ./scripts/toggle.js

// HTML 요소 가져오기
const $toggle = document.getElementById('toggleNav');
const $nav = document.getElementById('nav');
const $toggleIcon = $toggle?.querySelector('.toggle-icon');

// 이미지 경로 (실제 파일 경로 확인 필요)
const ARROW_LEFT = './images/arrow-left.png'; // 닫힘 상태 (네비바 보임)
const ARROW_RIGHT = './images/arrow-right.png'; // 열림 상태 (네비바 숨김)

$toggle.addEventListener('click', () => {
    $nav.classList.toggle('open');
    $toggle.classList.toggle('nav-open');
    if ($nav.classList.contains('open')) {
        // 네비바가 열렸을 때 -> '닫기' 아이콘 (왼쪽 화살표)
        $toggleIcon.src = ARROW_LEFT;
        $toggleIcon.alt = '닫기';
    } else {
        // 네비바가 닫혔을 때 -> '열기' 아이콘 (오른쪽 화살표)
        $toggleIcon.src = ARROW_RIGHT;
        $toggleIcon.alt = '열기';
    }
});