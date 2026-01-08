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

const sizeDataInput = document.getElementById('serverSizeData');
let sizes = [];

if (sizeDataInput && sizeDataInput.value) {
    sizes = sizeDataInput.value.split(',').map(s => s.trim());
}
const modal = document.getElementById('optionModal');
const openBtn = document.querySelector('.optionLink');
const closeBtn = document.querySelector('.close');
const sizeGrid = document.getElementById('sizeGrid');
const confirmButton = document.getElementById('confirmSizeButton');
const buyButton = document.querySelector('.buying');
// 수정: .cart 클래스가 헤더 등 다른 곳에도 존재할 수 있으므로 구체적인 선택자 사용
const cartButton = document.querySelector('.cart_Add .cart');


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
        openModal("ERROR", `<p>사이즈를 선택해주세요</p>`, {
            confirmText: '확인'});
    }
});

if (buyButton) {
    buyButton.addEventListener('click', () => {
        if (currentSelectedSize) {
            const urlParams = new URLSearchParams(window.location.search);
            const itemId = urlParams.get('id');
            location.href = `/pay?itemId=${itemId}&size=${currentSelectedSize}`;
        } else {
            openModal("ERROR", `<p>사이즈를 선택해주세요</p>`, {
                confirmText: '확인'});
        }
    });
}

if (cartButton) {
    cartButton.addEventListener('click', (e) => {
        e.preventDefault();
        if (currentSelectedSize) {
            const urlParams = new URLSearchParams(window.location.search);
            const itemId = urlParams.get('id');

            const formData = new FormData();
            formData.append('itemId', itemId);
            formData.append('size', currentSelectedSize);
            formData.append('quantity', 1);

            const xhr = new XMLHttpRequest();
            xhr.open('POST', '/cart');
            xhr.onreadystatechange = () => {
                if (xhr.readyState === XMLHttpRequest.DONE) {
                    if (xhr.status >= 200 && xhr.status < 300) {
                        const response = JSON.parse(xhr.responseText);
                        if (response.result === 'SUCCESS') {
                            openModal("장바구니 담기 성공", `<p>장바구니에 상품이 담겼습니다.</p>`, {
                                confirmText: '장바구니로 이동',
                                cancelText: '계속 쇼핑하기',
                                onConfirm: () => {
                                    location.href = '/cart';
                                },
                                onCancel: () => {
                                    // 계속 쇼핑하기: 아무 동작 안 함 (모달 닫힘)
                                }
                            });
                        } else {
                            openModal("ERROR", `<p>${response.message || '장바구니 담기에 실패했습니다.'}</p>`, {
                                confirmText: '확인'
                            });
                        }
                    } else {
                        openModal("ERROR", `<p>오류가 발생했습니다.</p>`, {
                            confirmText: '확인'
                        });
                    }
                }
            };
            xhr.send(formData);

        } else {
            openModal("ERROR", `<p>사이즈를 선택해주세요</p>`, {
                confirmText: '확인'});
        }
    });
}

const toggleLikeItem = ( shopId, itemId) => {
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('shopId', shopId);
    formData.append('itemId', itemId);
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, {confirmText: '확인'});
            return;
        }
        const response = JSON.parse(xhr.responseText);
        switch (response.result) {
            case "FAILURE_SESSION":
                openModal("FAILURE_SESSION", `<p>세션이 만료되었습니다. 다시 로그인해주세요.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => { location.href = '/login'; }
                });
                break;
            case 'FAILURE':
                openModal("FAILURE", `<p>관심상품 등록을 취소하였습니다.</p>`, {confirmText: '확인'});
                break;
            case 'SUCCESS':
                openModal("SUCCESS", `<p>관심상품으로 저장했습니다.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => {
                        location.href="/my?open=likes-item"
                    }
                });
                break;
            default:
                openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});

        }
     };
     xhr.open('POST', '/item/')
     xhr.send(formData);

}

const toggleLikeShop = (shopId) => {
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('shopId', shopId);
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, {confirmText: '확인'});
            return;
        }
        const response = JSON.parse(xhr.responseText);
        switch (response.result) {
            case "FAILURE_SESSION":
                openModal("FAILURE_SESSION", `<p>세션이 만료되었습니다. 다시 로그인해주세요.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => { location.href = '/login'; }
                });
                break;
            case 'FAILURE':
                openModal("FAILURE", `<p>관심매장 등록을 취소하였습니다.</p>`, {confirmText: '확인'});
                break;
            case 'SUCCESS':
                openModal("SUCCESS", `<p>관심매장에 등록되었습니다..</p>`, {
                    confirmText: '확인',
                    onConfirm: () => {
                        location.href="/my?open=likes-shop"
                    }
                });
                break;
            default:
                openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});

        }
    }
    xhr.open('POST', '/shop/like')
    xhr.send(formData);
}