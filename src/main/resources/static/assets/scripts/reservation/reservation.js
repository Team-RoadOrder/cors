document.addEventListener("DOMContentLoaded", function() {
    flatpickr("#date", {
        enableTime: true,       // 시간 선택 활성화 (datetime-local)
        dateFormat: "Y-m-d H:i", // 날짜 및 시간 형식
        locale: "ko",           // 한국어 설정
        // 여기에 더 많은 커스터마이징 옵션을 추가할 수 있습니다.
        // 예: minDate: "today" (오늘 이전 날짜 선택 불가)
        minDate: "today",
        position: "below"
    });
    renderItems();
});

const $reservation = document.getElementById("reservation");
const $button = $reservation.querySelector(':scope > form >.reservate')
$button.addEventListener("click", (e) => {
        e.preventDefault();
        openModal("answer", "<p>정말로 예약을 확정하시겠습니까?</p>", { confirmText: '확인' ,onConfirm: () => {
               sendItems() ;
            },cancelText: '취소',
            onCancel: () => {
            } });
})
const $cart = $reservation.querySelector(':scope > form >.cart')
 $cart.addEventListener("click", (e) => {
        e.preventDefault();
        openModal("alert", "<p>장바구니에 추가되었습니다.</p>", { confirmText: '확인' ,onConfirm: () => {
                /*emailInput.focus();
                emailInput.select();*/
            }});

})

const sendItems = () => {
    // 1. 필수 데이터 수집
    const dateInput = document.getElementById("date").value; // flatpickr 값 (예: 2025-01-05 14:30)
    const shopId = new URLSearchParams(window.location.search).get('shopId');

    // 2. 유효성 검사: 날짜
    if (!dateInput) {
        openModal("WARN", "<p>방문 예정 시간을 선택해주세요.</p>", { confirmText: '확인' });
        return;
    }

    // 3. 유효성 검사: 상품 및 사이즈 선택
    const checkedBoxes = document.querySelectorAll('.item-checkbox:checked');
    if (checkedBoxes.length === 0) {
        openModal("WARN", "<p>예약할 상품을 최소 하나 이상 선택해주세요.</p>", { confirmText: '확인' });
        return;
    }

    const items = [];
    let isSizeSelected = true; // 사이즈 선택 여부 플래그

    // 체크된 박스를 돌면서 데이터 수집
    checkedBoxes.forEach(checkbox => {
        const itemRow = checkbox.closest('.item-row'); // 체크박스가 있는 줄(div)을 찾음
        const sizeSelect = itemRow.querySelector('.size-select'); // 그 줄에 있는 사이즈 선택창 찾음
        const selectedSize = sizeSelect.value;

        // 사이즈가 선택되지 않았으면(기본값인 경우)
        if (!selectedSize) {
            isSizeSelected = false;
            return; // forEach 내부의 return은 continue와 비슷함
        }

        // DTO 구조에 맞춰서 객체 생성
        items.push({
            itemId: Number(checkbox.value), // Long 타입 대응
            size: selectedSize
        });
    });

    if (!isSizeSelected) {
        openModal("WARN", "<p>선택하신 상품의 사이즈를 모두 골라주세요.</p>", { confirmText: '확인' });
        return;
    }

    // 4. 전송할 데이터 객체 생성 (ReservationDto 구조)
    const dataObj = {
        shopId: Number(shopId),
        // Spring Boot가 인식하기 좋게 ' '를 'T'로 바꿔줌 (ISO-8601 형식)
        visitDate: dateInput.replace(' ', 'T'),
        items: items
    };

    // 5. AJAX 전송
    const xhr = new XMLHttpRequest();
    // ... 기존 xhr 로직 유지 ...
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, { confirmText: '확인' });
            return;
        }

        const response = JSON.parse(xhr.responseText);

        // 결과 처리 로직
        switch (response.result) {
            case "FAILURE_SESSION":
                openModal("FAILURE_SESSION", `<p>세션이 만료되었습니다. 다시 로그인해주세요.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => { location.href = '/login'; }
                });
                break;
            case "FAILURE":
                openModal("FAILURE", `<p>예약 처리에 실패하였습니다.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => { location.reload(); }
                });
                break;
            case "SUCCESS":
                openModal("SUCCESS", `<p>예약이 성공적으로 접수되었습니다!<br>
                                    예약한 상품은 마이페이지에서 확인가능합니다.<br>
                                     확인하시겠습니까?</p>`, {
                    confirmText: '확인',
                    onConfirm: () => {
                        // 성공 후 예약 내역 페이지로 이동하거나 새로고침
                        location.href = '/my?open=reservation'
                    },cancelText: '취소',
                onCancel: () => {
                        location.reload();
                    }
                });
                break;
            default:
                openModal("WARN", `<p>알 수 없는 오류가 발생했습니다.</p>`, { confirmText: '확인' });
        }
    };

    xhr.open('POST', '/reservation/post-items');
    xhr.setRequestHeader('Content-Type', 'application/json'); // JSON 전송 필수 헤더!
    xhr.send(JSON.stringify(dataObj)); // 객체를 문자열로 변환해서 전송
}

const renderItems = () => {
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, {
                confirmText: '확인'
            });
            return;
        }
        const response = JSON.parse(xhr.responseText);
        switch (response.result) {
            case 'FAILURE_SESSION':

                openModal("FAILURE_SESSION", `<p>세션정보가 만료되었습니다 로그인해주세요</p>`, {confirmText: '확인', onConfirm: () => {
                        location.href ='/login';
                    }});
                break;

            case 'FAILURE':
                openModal("FAILURE", `<p>등록 상품정보를 불러오는데 실패하였습니다.</p>`, {confirmText: '확인'});
                break;
            case 'SUCCESS':
                const container = document.querySelector('#item .container');
                container.innerHTML = '';
                if (!response.items || response.items.length === 0) {
                    container.innerHTML = '<div class="no-item">등록된 상품이 없습니다.</div>';
                    break;
                }
                response.items.forEach(item => {
                    const itemHtml = createItemHtml(item);
                    container.insertAdjacentHTML('beforeend', itemHtml);
                });
                break;
            default:
                openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});

        }
        
        
     };
    const shopId = new URLSearchParams(window.location.search).get('shopId');
    xhr.open('GET', `/reservation/all-items?shopId=${shopId}`);
     xhr.send();
            
}
// 아이템 데이터 객체 1개를 받아서 HTML 문자열로 반환하는 함수
const createItemHtml = (item) => {
    let sizeOptions = `<option value="" disabled selected>SIZE</option>`;

    if (item.size) {

        const sizes = item.size.split(',').map(s => s.trim());
        sizes.forEach(s => {
            sizeOptions += `<option value="${s}">${s}</option>`;
        });
    } else {
        // 사이즈가 없으면 Free 하나만 넣어줌
        sizeOptions += `<option value="Free">Free</option>`;
    }
    const formattedPrice = item.price.toLocaleString();
    const imageSrc = item.imagePath ? `/images/${item.imagePath}` : '/assets/images/default.png';

    // 4. HTML 조립 (작성하신 HTML 구조 그대로 사용)
    return `
        <div class="item-row">
            <input type="checkbox" id="checkbox_${item.id}" class="item-checkbox" value="${item.id}" data-id="${item.id}">
            
            <label for="checkbox_${item.id}" class="label">
                <span class="image-wrapper">
                    <img src="${imageSrc}" alt="상품이미지" draggable="false">
                </span>
                <span class="text-wrapper">
                    <span class="title">${item.itemName}</span>
                    <span class="price">${formattedPrice}원</span>
                    <span class="shop">${item.color}</span>
                    
                    <select class="size-select" onclick="event.stopPropagation()">
                        ${sizeOptions}
                    </select>
                </span>
            </label>
        </div>
    `;
}