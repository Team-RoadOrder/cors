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

    // 결제 동의 체크박스 처리
    const agreeCheckbox = document.getElementById("agreeAll");
    const paymentButton = document.getElementById("paymentButton");

    if (agreeCheckbox && paymentButton) {
        // 초기 상태 설정
        paymentButton.disabled = !agreeCheckbox.checked;

        agreeCheckbox.addEventListener("change", function() {
            paymentButton.disabled = !this.checked;
        });
    }
});
