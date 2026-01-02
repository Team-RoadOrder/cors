document.addEventListener("DOMContentLoaded", () => {
    const dots = document.querySelectorAll('.dot');
    const imageWrapper = document.querySelector('.image-wrapper');

    dots.forEach(dot => {
        dot.addEventListener('click', (e) => {
            e.preventDefault(); // 1. 앵커 태그의 기본 기능(페이지 점프)을 막습니다.

            // 2. href="#splashOne" 에서 id 값만 가져옵니다 ('splashOne')
            const targetId = dot.getAttribute('href').substring(1);
            const targetImage = document.getElementById(targetId);

            if (targetImage && imageWrapper) {
                // 3. imageWrapper(슬라이더 박스)만 해당 이미지 위치로 스크롤합니다.
                // offsetLeft는 컨테이너 기준 이미지의 왼쪽 위치입니다.
                imageWrapper.scrollTo({
                    left: targetImage.offsetLeft,
                    behavior: 'smooth' // 부드럽게 이동
                });
            }
        });
    });
});


const sizes = [
    '90(S)',
    '100(M)',
    '110(L)',
    '120(XL)'
];

const modal = document.getElementById('optionModal');
const openBtn = document.querySelector('.optionLink');
const closeBtn = document.querySelector('.close');
const sizeGrid = document.getElementById('sizeGrid');
const confirmButton = document.getElementById('confirmSizeButton');
const buyButton = document.getElementById('buyButton');

// ✅ 옵션링크 내부의 <b> 태그를 참조합니다.
const selectOptionText = document.querySelector('.selectOption b');

// ❌ Uncaught TypeError: Cannot set properties of null 오류를 피하기 위해 주석 처리하거나 제거합니다.
// const selectedSizeDisplay = document.querySelector('.selectedSizeDisplay');
// const chosenSizeSpan = document.getElementById('chosenSize');

let currentSelectedSize = null;

// --- 1. 사이즈 버튼 생성 및 초기화 ---
function renderSizeButtons() {
    sizeGrid.innerHTML = '';

    sizes.forEach(size => {
        const button = document.createElement('button');
        button.classList.add('size');
        button.textContent = size;
        button.dataset.size = size;

        if (size === currentSelectedSize) {
            button.classList.add('selected');
        }

        // 이벤트 리스너 부착
        button.addEventListener('click', handleSizeSelection);
        sizeGrid.appendChild(button);
    });
}

// --- 2. 사이즈 버튼 클릭 핸들러: 선택 상태 토글 및 값만 저장 ---
function handleSizeSelection(event) {
    // 모든 버튼의 'selected' 클래스 제거
    document.querySelectorAll('.size').forEach(btn => {
        btn.classList.remove('selected');
    });

    // 새로 선택된 버튼에 'selected' 클래스 추가
    event.target.classList.add('selected');
    currentSelectedSize = event.target.dataset.size; // 값만 저장
}

// --- 3. 모달 열기/닫기 및 값 업데이트 ---

// 모달 열기
openBtn.addEventListener('click', (e) => {
    e.preventDefault();
    renderSizeButtons();
    modal.style.display = 'flex';
});

// X 버튼 → 닫기
closeBtn.addEventListener('click', () => {
    modal.style.display = 'none';
});

// 배경 클릭 시 닫기
window.addEventListener('click', (e) => {
    if (e.target === modal) {
        modal.style.display = 'none';
    }
});

// ✅ "선택 완료" 버튼 클릭 시 로직: 옵션링크 텍스트에 반영 (오류 해결 및 기능 구현)
confirmButton.addEventListener('click', () => {
    if (currentSelectedSize) {
        selectOptionText.textContent = currentSelectedSize;
        // 3. 모달 닫기
        modal.style.display = 'none';
    } else {
        alert('사이즈를 선택해주세요.');
    }
});

// 구매하기 버튼 클릭 시 최종 확인 (기존 로직 유지)
buyButton.addEventListener('click', (e) => {
    e.preventDefault();
    if (!currentSelectedSize) {
        alert('구매하시려면 먼저 옵션을 선택해주세요.');
    } else {
        alert(`${currentSelectedSize} 사이즈로 구매를 진행합니다.`);
    }
});

// 초기 버튼 렌더링은 모달 열 때만 하도록 초기 실행 코드는 제거했습니다.