const selectAll = document.getElementById('selectAll');
const checkboxes = document.querySelectorAll('.item-checkbox');
const totalProductPriceElem = document.getElementById('totalProductPrice');
const deliveryFeeElem = document.getElementById('deliveryFee');
const totalPriceElem = document.getElementById('totalPrice');
const deleteButton = document.querySelector('.delete-btn');
const orderButton = document.querySelector('.order-btn');
const cartItemsList = document.querySelector('.cart-items-list');

function calculateTotal() {
    let totalProductPrice = 0;
    let deliveryFee = 0;

    const currentCheckboxes = document.querySelectorAll('.item-checkbox');
    currentCheckboxes.forEach(cb => {
        if (cb.checked) {
            const cartItem = cb.closest('.cart-item');
            const priceElement = cartItem.querySelector('.price');
            const price = parseInt(priceElement.textContent.replace(/[^0-9]/g, ''));
            totalProductPrice += price;
        }
    });

    if (totalProductPrice > 0) {
        deliveryFee = (totalProductPrice >= 70000) ? 0 : 3000;
    }

    totalProductPriceElem.textContent = totalProductPrice.toLocaleString() + '원';
    deliveryFeeElem.textContent = deliveryFee.toLocaleString() + '원';
    totalPriceElem.textContent = (totalProductPrice + deliveryFee).toLocaleString() + '원';
}

function updateItemPrice(cartItem, qty) {
    const priceElement = cartItem.querySelector('.price');
    const unitPrice = parseInt(priceElement.dataset.unitPrice);
    priceElement.textContent = (unitPrice * qty).toLocaleString() + '원';
}

if (cartItemsList) {
    cartItemsList.addEventListener('click', function(e) {
        const target = e.target;
        const cartItem = target.closest('.cart-item');

        if (target.classList.contains('btn-qty-minus')) {
            const qtyInput = cartItem.querySelector('.item-qty-input');
            let currentQty = parseInt(qtyInput.value);
            if (currentQty > 1) {
                currentQty--;
                qtyInput.value = currentQty;
                updateItemPrice(cartItem, currentQty);
                calculateTotal();
            }
        }
        else if (target.classList.contains('btn-qty-plus')) {
            const qtyInput = cartItem.querySelector('.item-qty-input');
            let currentQty = parseInt(qtyInput.value);
            currentQty++;
            qtyInput.value = currentQty;
            updateItemPrice(cartItem, currentQty);
            calculateTotal();
        }
        else if (target.classList.contains('btn-add-item')) {
            const newItem = cartItem.cloneNode(true);
            
            const checkbox = newItem.querySelector('.item-checkbox');
            checkbox.value = ""; // cartId 없음
            checkbox.checked = false; // 새 아이템은 체크 해제
            newItem.dataset.isNew = "true";
            
            const qtyInput = newItem.querySelector('.item-qty-input');
            qtyInput.value = 1;
            
            const priceElement = newItem.querySelector('.price');
            const unitPrice = parseInt(priceElement.dataset.unitPrice);
            priceElement.textContent = unitPrice.toLocaleString() + '원';
            
            cartItemsList.appendChild(newItem);
            
            // 전체 선택 해제
            if (selectAll) {
                selectAll.checked = false;
            }
            
            calculateTotal();
        }
    });
    
    cartItemsList.addEventListener('change', function(e) {
        if (e.target.classList.contains('item-checkbox')) {
            const allChecked = Array.from(document.querySelectorAll('.item-checkbox')).every(c => c.checked);
            if (selectAll) selectAll.checked = allChecked;
            calculateTotal();
        }
    });
}

if (selectAll) {
    selectAll.addEventListener('change', () => {
        const currentCheckboxes = document.querySelectorAll('.item-checkbox');
        currentCheckboxes.forEach(cb => cb.checked = selectAll.checked);
        calculateTotal();
    });
}

if (deleteButton) {
    deleteButton.addEventListener('click', () => {
        const selectedIds = [];
        const currentCheckboxes = document.querySelectorAll('.item-checkbox');
        const itemsToRemove = [];

        currentCheckboxes.forEach(cb => {
            if (cb.checked) {
                if (cb.value) {
                    selectedIds.push(cb.value);
                } else {
                    itemsToRemove.push(cb.closest('.cart-item'));
                }
            }
        });

        if (selectedIds.length === 0 && itemsToRemove.length === 0) {
            openModal("알림", `<p>삭제할 상품을 선택해주세요.</p>`, { confirmText: '확인' });
            return;
        }

        if (selectedIds.length === 0) {
            itemsToRemove.forEach(item => item.remove());
            calculateTotal();
            return;
        }

        openModal("삭제 확인", `<p>선택한 상품을 삭제하시겠습니까?</p>`, {
            confirmText: '삭제',
            cancelText: '취소',
            onConfirm: () => {
                const xhr = new XMLHttpRequest();
                const queryString = selectedIds.map(id => `ids=${id}`).join('&');
                xhr.open('DELETE', `/cart?${queryString}`);
                xhr.onreadystatechange = () => {
                    if (xhr.readyState === XMLHttpRequest.DONE) {
                        if (xhr.status >= 200 && xhr.status < 300) {
                            const response = JSON.parse(xhr.responseText);
                            if (response.result === 'SUCCESS') {
                                openModal("성공", `<p>삭제되었습니다.</p>`, {
                                    confirmText: '확인',
                                    onConfirm: () => {
                                        location.reload();
                                    }
                                });
                            } else {
                                openModal("오류", `<p>${response.message || '삭제에 실패했습니다.'}</p>`, { confirmText: '확인' });
                            }
                        } else {
                            openModal("오류", `<p>오류가 발생했습니다.</p>`, { confirmText: '확인' });
                        }
                    }
                };
                xhr.send();
            }
        });
    });
}

if (orderButton) {
    orderButton.addEventListener('click', () => {
        const existingIds = [];
        const newItems = [];
        const currentCheckboxes = document.querySelectorAll('.item-checkbox');
        
        currentCheckboxes.forEach(cb => {
            if (cb.checked) {
                const cartItem = cb.closest('.cart-item');
                if (cb.value) {
                    existingIds.push(cb.value);
                } else {
                    const itemId = cartItem.dataset.itemId;
                    const sizeSelect = cartItem.querySelector('.item-size-select');
                    const size = sizeSelect ? sizeSelect.value : 'FREE';
                    const qtyInput = cartItem.querySelector('.item-qty-input');
                    const quantity = parseInt(qtyInput.value);
                    
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

        const promises = newItems.map(item => {
            return new Promise((resolve, reject) => {
                const xhr = new XMLHttpRequest();
                const formData = new FormData();
                formData.append('itemId', item.itemId);
                formData.append('size', item.size);
                formData.append('quantity', item.quantity);
                
                xhr.open('POST', '/cart');
                xhr.onreadystatechange = () => {
                    if (xhr.readyState === XMLHttpRequest.DONE) {
                        if (xhr.status >= 200 && xhr.status < 300) {
                            const response = JSON.parse(xhr.responseText);
                            if (response.result === 'SUCCESS' && response.cartId) {
                                resolve(response.cartId);
                            } else {
                                reject('장바구니 추가 실패');
                            }
                        } else {
                            reject('서버 오류');
                        }
                    }
                };
                xhr.send(formData);
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
