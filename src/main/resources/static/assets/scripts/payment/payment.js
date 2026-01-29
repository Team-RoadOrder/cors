document.addEventListener("DOMContentLoaded", function() {
    // 토스페이먼츠 초기화 (내 클라이언트 키)
    const clientKey = "test_ck_XZYkKL4Mrjjxe4LwBYgLr0zJwlEW"; 
    const tossPayments = TossPayments(clientKey);
    // 로그인한 유저 ID가 있다면 넣고, 없으면 랜덤 문자열 (비회원 결제 시 ANONYMOUS)
    const customerKey = "USER_" + new Date().getTime(); 
    const payment = tossPayments.payment({ customerKey });

    const dialog = document.getElementById("dialog");
    const modal = dialog.querySelector(".modal");
    const closeBtn = dialog.querySelector(".close");
    const btnSearchAddress = document.getElementById("btn-search-address");
    const addressInput = document.getElementById("address_input");
    const addressDetailInput = document.getElementById("address_detail_input");

    function loadAddress() {
        new daum.Postcode({
            oncomplete: function(data) {
                let addr = '';
                if (data.userSelectedType === 'R') {
                    addr = data.roadAddress;
                } else {
                    addr = data.jibunAddress;
                }
                addressInput.value = addr;
                addressDetailInput.focus();
                dialog.style.display = 'none';
            },
            width : '100%',
            height : '100%'
        }).embed(modal);
        dialog.style.display = 'block';
    }

    if (btnSearchAddress) {
        btnSearchAddress.addEventListener("click", loadAddress);
    }
    
    if (addressInput) {
        addressInput.addEventListener("click", loadAddress);
    }

    if (closeBtn) {
        closeBtn.addEventListener("click", function(e) {
            e.preventDefault();
            dialog.style.display = 'none';
        });
    }

    // 배송 요청사항 직접 입력 처리
    const selectRequest = document.querySelector(".select-request");
    const inputRequest = document.querySelector(".input-request");

    if (selectRequest && inputRequest) {
        selectRequest.addEventListener("change", function() {
            if (this.value === "direct") {
                inputRequest.style.display = "block";
                inputRequest.focus();
            } else {
                inputRequest.style.display = "none";
                inputRequest.value = "";
            }
        });
        
        // [추가] 요청사항 글자수 제한 (20자)
        inputRequest.addEventListener("input", function() {
            if (this.value.length > 15) {
                this.value = this.value.slice(0, 15);
                // 선택 사항: 사용자에게 알림을 주고 싶다면 아래 주석 해제
                openModal('알림', '요청사항은 15자까지 입력 가능합니다.', { confirmText: '확인' });
            }
        });
    }

    // 이메일 도메인 선택 처리
    const emailSelect = document.getElementById("email_select");
    const emailDomain = document.getElementById("email_domain");

    if (emailSelect && emailDomain) {
        emailSelect.addEventListener("change", function() {
            if (this.value === "direct") {
                emailDomain.readOnly = false;
                emailDomain.value = "";
                emailDomain.focus();
            } else {
                emailDomain.readOnly = true;
                emailDomain.value = this.value;
            }
        });
    }

    // 전화번호 자동 포커스 이동
    const phoneInputs = document.querySelectorAll('.phone-group input');
    phoneInputs.forEach((input, index) => {
        input.addEventListener('input', function() {
            if (this.value.length >= this.maxLength) {
                if (index < phoneInputs.length - 1) {
                    phoneInputs[index + 1].focus();
                }
            }
        });
    });

    // 구매자와 동일 버튼 처리
    const btnSameAsOrderer = document.getElementById("btn-same-as-orderer");
    if (btnSameAsOrderer) {
        btnSameAsOrderer.addEventListener("click", function() {
            // 주문자 정보 가져오기
            const ordererName = document.getElementById("orderer_name").value;
            const ordererPhone1 = document.getElementById("orderer_phone_1").value;
            const ordererPhone2 = document.getElementById("orderer_phone_2").value;
            const ordererPhone3 = document.getElementById("orderer_phone_3").value;
            
            // 주문자 주소 정보 가져오기 (hidden input에서)
            const ordererAddress = document.getElementById("orderer_address").value;
            const ordererAddressDetail = document.getElementById("orderer_address_detail").value;

            // 받는 분 정보에 넣기
            document.getElementById("receiver_name").value = ordererName;
            
            const receiverPhoneInputs = document.querySelectorAll("#receiver_phone_group input");
            if (receiverPhoneInputs.length === 3) {
                receiverPhoneInputs[0].value = ordererPhone1;
                receiverPhoneInputs[1].value = ordererPhone2;
                receiverPhoneInputs[2].value = ordererPhone3;
            }
            
            // 주소 정보 넣기
            if (addressInput) addressInput.value = ordererAddress;
            if (addressDetailInput) addressDetailInput.value = ordererAddressDetail;
        });
    }

    // 결제 수단 탭 전환 처리
    const paymentMethodGroups = document.querySelectorAll('input[name="paymentMethodGroup"]');
    const simplePaymentContent = document.querySelector('.simple-payment-content');
    const generalPaymentContent = document.querySelector('.general-payment-content');

    if (paymentMethodGroups.length > 0) {
        paymentMethodGroups.forEach(radio => {
            radio.addEventListener('change', function() {
                if (this.value === 'simple') {
                    // 간편결제 선택 시
                    if (simplePaymentContent) simplePaymentContent.style.display = 'block';
                    if (generalPaymentContent) generalPaymentContent.style.display = 'none';
                } else {
                    // 일반결제 선택 시
                    if (simplePaymentContent) simplePaymentContent.style.display = 'none';
                    if (generalPaymentContent) generalPaymentContent.style.display = 'block';
                }
            });
        });
        
        // 초기 상태 설정 (간편결제가 기본 선택되어 있다고 가정)
        // HTML에서 checked 속성으로 제어하지만, JS로 확실하게 처리
        const checkedGroup = document.querySelector('input[name="paymentMethodGroup"]:checked');
        if (checkedGroup) {
            if (checkedGroup.value === 'simple') {
                if (simplePaymentContent) simplePaymentContent.style.display = 'block';
                if (generalPaymentContent) generalPaymentContent.style.display = 'none';
            } else {
                if (simplePaymentContent) simplePaymentContent.style.display = 'none';
                if (generalPaymentContent) generalPaymentContent.style.display = 'block';
            }
        }
    }

    // 포인트 관련 로직
    const pointInput = document.getElementById('point-input');
    const btnUseAllPoints = document.getElementById('btn-use-all-points');
    const maxPointInput = document.getElementById('max-point');
    const summaryDiscountPrice = document.getElementById('summary-discount-price');
    
    let usedPoints = 0;

    if (pointInput && maxPointInput) {
        const maxPoints = parseInt(maxPointInput.value);

        pointInput.addEventListener('input', function() {
            let val = parseInt(this.value);
            if (isNaN(val)) val = 0;
            
            if (val < 0) val = 0;
            if (val > maxPoints) {
                val = maxPoints;
                openModal('알림', '보유 포인트 이상 사용할 수 없습니다.', { confirmText: '확인' });
            }
            
            this.value = val;
            usedPoints = val;
            updateTotalPrice();
        });

        if (btnUseAllPoints) {
            btnUseAllPoints.addEventListener('click', function() {
                pointInput.value = maxPoints;
                usedPoints = maxPoints;
                updateTotalPrice();
            });
        }
    }

    // 수량 조절 및 가격 업데이트 로직
    const orderItemsList = document.querySelector('.order-items-list');
    if (orderItemsList) {
        orderItemsList.addEventListener('click', function(e) {
            // 수량 감소
            if (e.target.classList.contains('btn-qty-minus')) {
                const qtyInput = e.target.parentElement.querySelector('.item-qty-input');
                let currentQty = parseInt(qtyInput.value);
                if (currentQty > 1) {
                    currentQty--;
                    qtyInput.value = currentQty;
                    updateItemPrice(e.target.closest('.item-detail'), currentQty);
                    updateTotalPrice();
                }
            } 
            // 수량 증가
            else if (e.target.classList.contains('btn-qty-plus')) {
                const qtyInput = e.target.parentElement.querySelector('.item-qty-input');
                let currentQty = parseInt(qtyInput.value);
                currentQty++;
                qtyInput.value = currentQty;
                updateItemPrice(e.target.closest('.item-detail'), currentQty);
                updateTotalPrice();
            }
            // 아이템 추가 (+) 버튼
            else if (e.target.classList.contains('btn-add-item')) {
                const currentItem = e.target.closest('.order-item');
                const newItem = currentItem.cloneNode(true);
                
                // 복제된 아이템 초기화
                newItem.removeAttribute('data-cart-id'); // 장바구니 ID 제거
                newItem.setAttribute('data-is-new', 'true'); // 새 아이템 표시
                
                // 수량 1로 초기화
                const qtyInput = newItem.querySelector('.item-qty-input');
                qtyInput.value = 1;
                
                // 가격 초기화
                const priceElement = newItem.querySelector('.price');
                const unitPrice = parseInt(priceElement.dataset.unitPrice);
                priceElement.textContent = unitPrice.toLocaleString() + '원';
                
                // 리스트에 추가
                orderItemsList.appendChild(newItem);
                updateTotalPrice();
            }
        });
    }

    function updateItemPrice(itemDetail, qty) {
        const priceElement = itemDetail.querySelector('.price');
        const unitPrice = parseInt(priceElement.dataset.unitPrice);
        priceElement.textContent = (unitPrice * qty).toLocaleString() + '원';
    }

    function updateTotalPrice() {
        let totalProductPrice = 0;
        const itemDetails = document.querySelectorAll('.item-detail');
        
        itemDetails.forEach(detail => {
            const qty = parseInt(detail.querySelector('.item-qty-input').value);
            const unitPrice = parseInt(detail.querySelector('.price').dataset.unitPrice);
            totalProductPrice += (unitPrice * qty);
        });

        const deliveryFee = (totalProductPrice >= 70000) ? 0 : 3000;
        
        // 포인트 적용 전 총액
        let totalPrice = totalProductPrice + deliveryFee;
        
        // 포인트 적용 (총액보다 많이 쓸 수 없음)
        if (usedPoints > totalPrice) {
            usedPoints = totalPrice;
            if (pointInput) pointInput.value = usedPoints;
        }
        
        totalPrice -= usedPoints;

        document.getElementById('summary-product-price').textContent = totalProductPrice.toLocaleString() + '원';
        document.getElementById('summary-delivery-fee').textContent = deliveryFee.toLocaleString() + '원';
        
        if (summaryDiscountPrice) {
            summaryDiscountPrice.textContent = '- ' + usedPoints.toLocaleString() + '원';
        }
        
        document.getElementById('summary-total-price').textContent = totalPrice.toLocaleString() + '원';
        document.getElementById('btn-payment-text').textContent = totalPrice.toLocaleString() + '원 결제하기';
    }


    // 결제 동의 체크박스 처리
    const agreeCheckbox = document.getElementById("agreeAll");
    const paymentButton = document.getElementById("paymentButton");
    const paymentCompleteModal = document.getElementById("paymentCompleteModal");
    const paymentContainer = document.getElementById("paymentContainer");
    const btnGoToOrders = document.getElementById("btnGoToOrders");

    if (agreeCheckbox && paymentButton) {
        // 초기 상태 설정
        paymentButton.disabled = !agreeCheckbox.checked;

        agreeCheckbox.addEventListener("change", function() {
            paymentButton.disabled = !this.checked;
        });

        // 결제 버튼 클릭 이벤트
        paymentButton.addEventListener("click", function() {
            // 1. 즉시 로딩 표시 및 버튼 비활성화 (가장 먼저 실행)
            Loading.show("결제 준비 중입니다...");
            paymentButton.disabled = true;

            // 2. 동의 여부 확인 (로딩 끄고 리턴)
            if (!agreeCheckbox.checked) {
                Loading.hide();
                paymentButton.disabled = false;
                openModal('알림', '주문 내용 확인 및 정보 제공에 동의해주세요.', { confirmText: '확인' });
                return;
            }

            // 입력값 수집
            const receiverName = document.getElementById('receiver_name').value;
            
            // 연락처 조합
            const phoneParts = document.querySelectorAll('#receiver_phone_group input');
            let receiverPhone = '';
            if (phoneParts.length === 3 && phoneParts[0].value && phoneParts[1].value && phoneParts[2].value) {
                receiverPhone = `${phoneParts[0].value}-${phoneParts[1].value}-${phoneParts[2].value}`;
            }

            const address = addressInput.value;
            const addressDetail = addressDetailInput.value;
            
            let request = selectRequest.value;
            if (request === "direct") {
                request = inputRequest.value;
            } else if (request === "please") {
                request = "요청사항 없음"; // 선택 안 함일 경우 기본값 설정
            }

            // 유효성 검사 (실패 시 로딩 끄고 리턴)
            if (!receiverName || receiverName.trim() === "") {
                Loading.hide();
                paymentButton.disabled = false;
                openModal('알림', '수령인 이름을 입력해주세요.', { 
                    confirmText: '확인',
                    onConfirm: () => document.getElementById('receiver_name').focus()
                });
                return;
            }
            
            // 전화번호 유효성 검사
            const phoneRegex = /^[0-9]+$/;
            if (!phoneParts[0].value.trim() || !phoneRegex.test(phoneParts[0].value.trim()) ||
                !phoneParts[1].value.trim() || !phoneRegex.test(phoneParts[1].value.trim()) ||
                !phoneParts[2].value.trim() || !phoneRegex.test(phoneParts[2].value.trim())) {
                Loading.hide();
                paymentButton.disabled = false;
                openModal('알림', '수령인 연락처를 올바르게 입력해주세요.', {
                    confirmText: '확인',
                    onConfirm: () => phoneParts[0].focus()
                });
                return;
            }

            if (!address || address.trim() === "") {
                Loading.hide();
                paymentButton.disabled = false;
                openModal('알림', '배송지 주소를 입력해주세요.', {
                    confirmText: '확인',
                    onConfirm: () => addressInput.focus()
                });
                return;
            }
            if (!addressDetail || addressDetail.trim() === "") {
                Loading.hide();
                paymentButton.disabled = false;
                openModal('알림', '상세 주소를 입력해주세요.', {
                    confirmText: '확인',
                    onConfirm: () => addressDetailInput.focus()
                });
                return;
            }

            // 결제 수단 수집
            let paymentMethod = 'CARD'; // 기본값
            const paymentGroup = document.querySelector('input[name="paymentMethodGroup"]:checked').value;
            
            if (paymentGroup === 'simple') {
                paymentMethod = 'SIMPLE_PAY';
            } else {
                const generalMethod = document.querySelector('input[name="generalPaymentMethod"]:checked');
                if (generalMethod) {
                    paymentMethod = generalMethod.value;
                }
            }

            // 서버로 전송할 데이터 구성
            const urlParams = new URLSearchParams(window.location.search);
            let itemId = urlParams.get('itemId');
            let size = urlParams.get('size');
            let quantity = 1; // 기본값

            // 장바구니 ID 수집
            const cartIdInputs = document.querySelectorAll('.cart-id-hidden');
            let cartIds = [];
            cartIdInputs.forEach(input => {
                cartIds.push(parseInt(input.value));
            });

            // 아이템 정보 수집
            let cartQuantities = {};
            let newItems = [];
            const orderItems = document.querySelectorAll('.order-item');
            
            orderItems.forEach(item => {
                const cartId = item.dataset.cartId;
                const isNew = item.dataset.isNew === 'true';
                const currentItemId = item.dataset.itemId || itemId; 
                const qtyInput = item.querySelector('.item-qty-input');
                const sizeSelect = item.querySelector('.item-size-select');
                
                const currentQty = parseInt(qtyInput.value);
                const currentSize = sizeSelect ? sizeSelect.value : size;

                if (cartId && !isNew) {
                    cartQuantities[cartId] = currentQty;
                } else if (isNew || (!cartId && currentItemId)) {
                    if (!cartId && !isNew && itemId) {
                        quantity = currentQty;
                        size = currentSize;
                    } else {
                        newItems.push({
                            itemId: parseInt(currentItemId),
                            size: currentSize,
                            quantity: currentQty
                        });
                    }
                }
            });

            const orderData = {
                itemId: itemId ? parseInt(itemId) : null,
                size: size,
                quantity: quantity,
                cartIds: cartIds.length > 0 ? cartIds : null,
                cartQuantities: Object.keys(cartQuantities).length > 0 ? cartQuantities : null,
                newItems: newItems.length > 0 ? newItems : null,
                request: request,
                receiverName: receiverName,
                receiverPhone: receiverPhone,
                address: address,
                addressDetail: addressDetail,
                paymentMethod: paymentMethod,
                usedPoints: usedPoints,
                usedCouponId: null
            };

            // 3. 서버에 '가주문' 요청
            fetch('/pay/prepare', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(orderData)
            })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => { throw new Error(text || "서버 응답 오류"); });
                }
                return response.text();
            }) 
            .then(orderId => {
                // 4. 주문번호를 받으면 토스 결제창 띄우기 (로딩은 유지)
                
                const totalAmountStr = document.getElementById('summary-total-price').textContent;
                const totalAmount = parseInt(totalAmountStr.replace(/[^0-9]/g, ''));

                return payment.requestPayment({
                    method: "CARD",
                    amount: {
                        currency: "KRW",
                        value: totalAmount,
                    },
                    orderId: orderId,
                    orderName: "로드오더 상품 결제",
                    successUrl: window.location.origin + "/payment/success",
                    failUrl: window.location.origin + "/payment/fail",
                    customerEmail: document.getElementById('orderer_email') ? document.getElementById('orderer_email').value : "",
                    customerName: receiverName,
                });
            })
            .catch(error => {
                // 5. 에러 발생 시에만 로딩 끄고 버튼 활성화
                Loading.hide();
                paymentButton.disabled = false;
                
                // 토스 결제창 닫기 등으로 인한 에러는 무시하거나 적절히 처리
                if (error.code === 'USER_CANCEL') {
                    // 사용자가 결제창을 닫은 경우
                    return;
                }

                console.error("결제 준비 중 에러:", error);
                openModal('오류', '결제 시작에 실패했습니다: ' + error.message, { confirmText: '확인' });
            });
        });
    }

    if (btnGoToOrders) {
        btnGoToOrders.addEventListener("click", function() {
            window.location.href = "/my?menu=orders";
        });
    }
});
