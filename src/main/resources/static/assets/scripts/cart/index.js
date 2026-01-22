const $selectAll = document.getElementById('selectAll');
const $cartItemsList = document.querySelector('.cart-items-list');
const $totalProductPrice = document.getElementById('totalProductPrice');
const $deliveryFee = document.getElementById('deliveryFee');
const $totalPrice = document.getElementById('totalPrice');
const $deleteButton = document.querySelector('.delete-btn');
const $orderButton = document.querySelector('.order-btn');

/**
 * 총 결제 금액 계산
 */
const calculateTotal = () => {
    let totalProductPrice = 0;
    let deliveryFee = 0;

    const $checkboxes = document.querySelectorAll('.item-checkbox');
    $checkboxes.forEach($cb => {
        if ($cb.checked) {
            const $cartItem = $cb.closest('.cart-item');
            const $priceElement = $cartItem.querySelector('.price');
            const price = parseInt($priceElement.textContent.replace(/[^0-9]/g, ''));
            totalProductPrice += price;
        }
    });

    if (totalProductPrice > 0) {
        deliveryFee = (totalProductPrice >= 70000) ? 0 : 3000;
    }

    $totalProductPrice.textContent = totalProductPrice.toLocaleString() + '원';
    $deliveryFee.textContent = deliveryFee.toLocaleString() + '원';
    $totalPrice.textContent = (totalProductPrice + deliveryFee).toLocaleString() + '원';
};

/**
 * 개별 아이템 가격 업데이트
 * @param {HTMLElement} $cartItem 
 * @param {number} qty 
 */
const updateItemPrice = ($cartItem, qty) => {
    const $priceElement = $cartItem.querySelector('.price');
    const unitPrice = parseInt($priceElement.dataset.unitPrice);
    $priceElement.textContent = (unitPrice * qty).toLocaleString() + '원';
};

/**
 * 장바구니 삭제 요청
 * @param {string[]} ids 
 * @param {function} onSuccess 
 */
const deleteCartRequest = (ids, onSuccess) => {
    const xhr = new XMLHttpRequest();
    const queryString = `ids=${ids.join(',')}`;
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal('오류', `요청을 처리하는 도중 오류가 발생하였습니다. 잠시 후 다시 시도해 주세요. (${xhr.status})`, { confirmText: '확인' });
            return;
        }
        const response = JSON.parse(xhr.responseText);
        switch (response.result) {
            case 'FAILURE':
                openModal('경고', response.message || '알 수 없는 이유로 삭제하지 못하였습니다.', { confirmText: '확인' });
                break;
            case 'FAILURE_SESSION':
                openModal('경고', '세션이 만료되었습니다. 지금 로그인하러 이동할까요?', {
                    confirmText: '이동',
                    cancelText: '취소',
                    onConfirm: () => location.href = '/login'
                });
                break;
            case 'SUCCESS':
                if (typeof onSuccess === 'function') {
                    onSuccess();
                }
                break;
            default:
                openModal('경고', `서버가 알 수 없는 응답을 반환하였습니다. (${response.result})`, { confirmText: '확인' });
        }
    };
    xhr.open('DELETE', `/cart?${queryString}`);
    xhr.send();
};

/**
 * 장바구니 추가 요청 (주문 시 사용)
 * @param {number} itemId 
 * @param {string} size 
 * @param {number} quantity 
 * @param {function} onSuccess 
 * @param {function} onFailure 
 */
const addCartRequest = (itemId, size, quantity, onSuccess, onFailure) => {
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('itemId', itemId);
    formData.append('size', size);
    formData.append('quantity', quantity);
    
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            if (typeof onFailure === 'function') onFailure('서버 통신 오류');
            return;
        }
        const response = JSON.parse(xhr.responseText);
        switch (response.result) {
            case 'SUCCESS':
                if (typeof onSuccess === 'function') onSuccess(response.cartId);
                break;
            default:
                if (typeof onFailure === 'function') onFailure(response.message || '추가 실패');
        }
    };
    xhr.open('POST', '/cart');
    xhr.send(formData);
};

// 이벤트 리스너 등록
if ($cartItemsList) {
    $cartItemsList.addEventListener('click', function(e) {
        const $target = e.target;
        const $cartItem = $target.closest('.cart-item');

        if ($target.classList.contains('btn-qty-minus')) {
            const $qtyInput = $cartItem.querySelector('.item-qty-input');
            let currentQty = parseInt($qtyInput.value);
            if (currentQty > 1) {
                currentQty--;
                $qtyInput.value = currentQty;
                updateItemPrice($cartItem, currentQty);
                calculateTotal();
            }
        }
        else if ($target.classList.contains('btn-qty-plus')) {
            const $qtyInput = $cartItem.querySelector('.item-qty-input');
            let currentQty = parseInt($qtyInput.value);
            if (currentQty < 99) { // 최대 수량 제한
                currentQty++;
                $qtyInput.value = currentQty;
                updateItemPrice($cartItem, currentQty);
                calculateTotal();
            }
        }
        else if ($target.classList.contains('btn-add-item')) {
            const $newItem = $cartItem.cloneNode(true);
            
            const $checkbox = $newItem.querySelector('.item-checkbox');
            $checkbox.value = ""; // cartId 없음
            $checkbox.checked = false; // 새 아이템은 체크 해제
            $newItem.dataset.isNew = "true";
            
            const $qtyInput = $newItem.querySelector('.item-qty-input');
            $qtyInput.value = 1;
            
            const $priceElement = $newItem.querySelector('.price');
            const unitPrice = parseInt($priceElement.dataset.unitPrice);
            $priceElement.textContent = unitPrice.toLocaleString() + '원';
            
            $cartItemsList.appendChild($newItem);
            
            // 전체 선택 해제
            if ($selectAll) {
                $selectAll.checked = false;
            }
            
            calculateTotal();
        }
    });

    // 수량 입력 필드 유효성 검사
    $cartItemsList.addEventListener('input', function(e) {
        if (e.target.classList.contains('item-qty-input')) {
            let val = e.target.value;
            val = val.replace(/[^0-9]/g, ''); // 숫자만 허용
            
            if (val === '' || parseInt(val) < 1) {
                val = '1';
            }
            if (parseInt(val) > 99) {
                val = '99';
            }
            
            e.target.value = val;
            
            const $cartItem = e.target.closest('.cart-item');
            updateItemPrice($cartItem, parseInt(val));
            calculateTotal();
        }
    });
    
    $cartItemsList.addEventListener('change', function(e) {
        if (e.target.classList.contains('item-checkbox')) {
            const $allCheckboxes = Array.from(document.querySelectorAll('.item-checkbox'));
            const allChecked = $allCheckboxes.every(c => c.checked);
            if ($selectAll) $selectAll.checked = allChecked;
            calculateTotal();
        }
    });
}

if ($selectAll) {
    $selectAll.addEventListener('change', () => {
        const $currentCheckboxes = document.querySelectorAll('.item-checkbox');
        $currentCheckboxes.forEach($cb => $cb.checked = $selectAll.checked);
        calculateTotal();
    });
}

if ($deleteButton) {
    $deleteButton.addEventListener('click', () => {
        const selectedIds = [];
        const $itemsToRemove = [];

        const $currentCheckboxes = document.querySelectorAll('.item-checkbox');
        $currentCheckboxes.forEach($cb => {
            if ($cb.checked) {
                if ($cb.value) {
                    selectedIds.push($cb.value);
                } else {
                    $itemsToRemove.push($cb.closest('.cart-item'));
                }
            }
        });

        if (selectedIds.length === 0 && $itemsToRemove.length === 0) {
            openModal("알림", `<p>삭제할 상품을 선택해주세요.</p>`, { confirmText: '확인' });
            return;
        }

        openModal("삭제 확인", `<p>선택한 상품을 삭제하시겠습니까?</p>`, {
            confirmText: '삭제',
            cancelText: '취소',
            onConfirm: () => {
                // 1. 서버에 저장되지 않은(새로 추가된) 아이템은 즉시 DOM에서 제거
                if ($itemsToRemove.length > 0) {
                    $itemsToRemove.forEach($item => $item.remove());
                    calculateTotal();
                }

                // 2. 서버에 저장된 아이템이 있다면 서버에 삭제 요청
                if (selectedIds.length > 0) {
                    deleteCartRequest(selectedIds, () => {
                        openModal("성공", `<p>삭제되었습니다.</p>`, {
                            confirmText: '확인',
                            onConfirm: () => {
                                // DOM에서 삭제된 아이템 제거
                                selectedIds.forEach(id => {
                                    const $chk = document.querySelector(`.item-checkbox[value="${id}"]`);
                                    if ($chk) {
                                        $chk.closest('.cart-item').remove();
                                    }
                                });
                                
                                // 가격 재계산
                                calculateTotal();
                                
                                // 전체 선택 체크박스 상태 업데이트
                                const $remainingCheckboxes = document.querySelectorAll('.item-checkbox');
                                if ($remainingCheckboxes.length > 0 && $selectAll) {
                                    $selectAll.checked = Array.from($remainingCheckboxes).every(c => c.checked);
                                } else if ($remainingCheckboxes.length === 0 && $selectAll) {
                                    $selectAll.checked = false;
                                }
                            }
                        });
                    });
                }
            }
        });
    });
}

if ($orderButton) {
    $orderButton.addEventListener('click', () => {
        const existingIds = [];
        const newItems = [];
        const $currentCheckboxes = document.querySelectorAll('.item-checkbox');
        
        $currentCheckboxes.forEach($cb => {
            if ($cb.checked) {
                const $cartItem = $cb.closest('.cart-item');
                if ($cb.value) {
                    existingIds.push($cb.value);
                } else {
                    const itemId = $cartItem.dataset.itemId;
                    const $sizeSelect = $cartItem.querySelector('.item-size-select');
                    const size = $sizeSelect ? $sizeSelect.value : 'FREE';
                    const $qtyInput = $cartItem.querySelector('.item-qty-input');
                    let quantity = parseInt($qtyInput.value);
                    
                    // 유효성 검사
                    if (!itemId) return;
                    if (isNaN(quantity) || quantity < 1) quantity = 1;
                    if (quantity > 99) quantity = 99;
                    
                    newItems.push({ itemId, size, quantity });
                }
            }
        });

        if (existingIds.length === 0 && newItems.length === 0) {
            openModal("알림", `<p>결제할 상품을 선택해주세요.</p>`, { confirmText: '확인' });
            return;
        }

        if (newItems.length === 0) {
            const queryString = existingIds.map(id => `cartIds=${id}`).join('&');
            location.href = `/pay?${queryString}`;
            return;
        }

        // 새 아이템들을 장바구니에 추가 후 결제 페이지로 이동
        const promises = newItems.map(item => {
            return new Promise((resolve, reject) => {
                addCartRequest(item.itemId, item.size, item.quantity, 
                    (cartId) => resolve(cartId),
                    (error) => reject(error)
                );
            });
        });

        Promise.all(promises)
            .then(newCartIds => {
                const allIds = [...existingIds, ...newCartIds];
                const queryString = allIds.map(id => `cartIds=${id}`).join('&');
                location.href = `/pay?${queryString}`;
            })
            .catch(error => {
                console.error(error);
                openModal("오류", `<p>주문 처리 중 오류가 발생했습니다.</p>`, { confirmText: '확인' });
            });
    });
}

calculateTotal();
