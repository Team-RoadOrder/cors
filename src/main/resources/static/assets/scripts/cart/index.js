const selectAll = document.getElementById('selectAll');
const checkboxes = document.querySelectorAll('.item-checkbox');
const totalProductPriceElem = document.getElementById('totalProductPrice');
const deliveryFeeElem = document.getElementById('deliveryFee');
const totalPriceElem = document.getElementById('totalPrice');
const deleteButton = document.querySelector('.delete'); // 삭제 버튼 선택

function calculateTotal() {
    let totalProductPrice = 0;
    let deliveryFee = 0;

    checkboxes.forEach(cb => {
        if (cb.checked) {
            const cartItem = cb.closest('.cart-item');
            const priceText = cartItem.querySelector('.item-price').textContent;
            const price = parseInt(priceText.replace(/[^0-9]/g, ''));
            totalProductPrice += price;
        }
    });

    if (totalProductPrice > 0) {
        deliveryFee = 3000; // 배송비 예시 (조건에 따라 변경 가능)
    }

    totalProductPriceElem.textContent = totalProductPrice.toLocaleString() + '원';
    deliveryFeeElem.textContent = deliveryFee.toLocaleString() + '원';
    totalPriceElem.textContent = (totalProductPrice + deliveryFee).toLocaleString() + '원';
}

if (selectAll) {
    selectAll.addEventListener('change', () => {
        checkboxes.forEach(cb => cb.checked = selectAll.checked);
        calculateTotal();
    });
}

checkboxes.forEach(cb => {
    cb.addEventListener('change', () => {
        const allChecked = Array.from(checkboxes).every(c => c.checked);
        if (selectAll) selectAll.checked = allChecked;
        calculateTotal();
    });
});

// 삭제 버튼 클릭 이벤트 핸들러
if (deleteButton) {
    deleteButton.addEventListener('click', () => {
        const selectedIds = [];
        checkboxes.forEach(cb => {
            if (cb.checked) {
                selectedIds.push(cb.value); // 체크박스의 value에 cartId가 있다고 가정
            }
        });

        if (selectedIds.length === 0) {
            openModal("알림", `<p>삭제할 상품을 선택해주세요.</p>`, {
                confirmText: '확인'
            });
            return;
        }

        openModal("삭제 확인", `<p>선택한 상품을 삭제하시겠습니까?</p>`, {
            confirmText: '삭제',
            cancelText: '취소',
            onConfirm: () => {
                // 서버로 삭제 요청 전송 (AJAX 사용)
                const xhr = new XMLHttpRequest();
                // 배열을 쿼리 파라미터로 변환 (예: ids=1&ids=2&ids=3)
                const queryString = selectedIds.map(id => `ids=${id}`).join('&');
                xhr.open('DELETE', `/cart?${queryString}`);
                xhr.onreadystatechange = () => {
                    if (xhr.readyState === XMLHttpRequest.DONE) {
                        if (xhr.status >= 200 && xhr.status < 300) {
                            const response = JSON.parse(xhr.responseText);
                            if (response.result === 'success') {
                                openModal("성공", `<p>삭제되었습니다.</p>`, {
                                    confirmText: '확인',
                                    onConfirm: () => {
                                        location.reload(); // 페이지 새로고침하여 목록 갱신
                                    }
                                });
                            } else {
                                openModal("오류", `<p>${response.message || '삭제에 실패했습니다.'}</p>`, {
                                    confirmText: '확인'
                                });
                            }
                        } else {
                            openModal("오류", `<p>오류가 발생했습니다.</p>`, {
                                confirmText: '확인'
                            });
                        }
                    }
                };
                xhr.send();
            }
        });
    });
}

// 초기 계산
calculateTotal();
