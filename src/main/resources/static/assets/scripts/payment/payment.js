document.addEventListener("DOMContentLoaded", function() {
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
            if (!agreeCheckbox.checked) {
                alert("주문 내용 확인 및 정보 제공에 동의해주세요.");
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

            // 유효성 검사
            if (!receiverName || !receiverPhone || !address || !addressDetail) {
                alert("배송지 정보를 모두 입력해주세요.");
                return;
            }

            // 서버로 전송할 데이터 구성
            const urlParams = new URLSearchParams(window.location.search);
            const itemId = urlParams.get('itemId');
            const size = urlParams.get('size');
            
            // 장바구니 ID 수집
            const cartIdInputs = document.querySelectorAll('.cart-id-hidden');
            let cartIds = [];
            cartIdInputs.forEach(input => {
                cartIds.push(parseInt(input.value));
            });

            const orderData = {
                itemId: itemId ? parseInt(itemId) : null,
                size: size,
                cartIds: cartIds.length > 0 ? cartIds : null,
                request: request,
                receiverName: receiverName,
                receiverPhone: receiverPhone,
                address: address,
                addressDetail: addressDetail
            };

            fetch('/pay', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(orderData)
            })
            .then(response => response.json())
            .then(data => {
                if (data.result === 'SUCCESS') {
                    // 결제 성공 시 화면 처리
                    if (paymentContainer) {
                        paymentContainer.style.display = 'none'; // 기존 결제 화면 숨김
                    }
                    if (paymentCompleteModal) {
                        paymentCompleteModal.style.display = 'flex'; // 완료 모달 표시
                    }
                } else {
                    alert("결제 실패: " + data.message);
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert("결제 처리 중 오류가 발생했습니다.");
            });
        });
    }

    if (btnGoToOrders) {
        btnGoToOrders.addEventListener("click", function() {
            window.location.href = "/my?menu=orders";
        });
    }
});
