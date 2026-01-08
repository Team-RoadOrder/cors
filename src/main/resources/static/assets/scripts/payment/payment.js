document.addEventListener('DOMContentLoaded', () => {
    const paymentButton = document.getElementById('paymentButton');
    const requestSelect = document.getElementById('please');
    const isCartOrderInput = document.getElementById('isCartOrder');

    if (paymentButton) {
        paymentButton.addEventListener('click', () => {
            const request = requestSelect.value;
            const isCartOrder = isCartOrderInput.value === 'true';
            
            let url = '';
            let data = {};

            if (isCartOrder) {
                // 장바구니 주문
                const productElements = document.querySelectorAll('.product');
                const cartIds = [];
                productElements.forEach(el => {
                    const cartId = el.dataset.cartId;
                    if (cartId) cartIds.push(cartId);
                });
                
                url = '/cart/order';
                data = { cartIds: cartIds }; // request는 현재 CartOrderDto에 없으므로 추가 필요할 수 있음
            } else {
                // 단일 상품 주문
                const productElement = document.querySelector('.product');
                const itemId = productElement.dataset.itemId;
                const size = productElement.dataset.size;
                
                url = '/pay';
                data = {
                    itemId: itemId,
                    size: size,
                    request: request
                };
            }

            const xhr = new XMLHttpRequest();
            xhr.open('POST', url);
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.onreadystatechange = () => {
                if (xhr.readyState === XMLHttpRequest.DONE) {
                    if (xhr.status >= 200 && xhr.status < 300) {
                        const response = JSON.parse(xhr.responseText);
                        if (response.result === 'SUCCESS') {
                            openModal("성공", `<p>결제가 완료되었습니다.</p>`, {
                                confirmText: '확인',
                                onConfirm: () => {
                                    location.href = '/my'; // 마이페이지로 이동
                                }
                            });
                        } else {
                            openModal("오류", `<p>${response.message || '결제에 실패했습니다.'}</p>`, {
                                confirmText: '확인'
                            });
                        }
                    } else {
                        openModal("오류", `<p>서버 통신 중 오류가 발생했습니다.</p>`, {
                            confirmText: '확인'
                        });
                    }
                }
            };
            xhr.send(JSON.stringify(data));
        });
    }
});
