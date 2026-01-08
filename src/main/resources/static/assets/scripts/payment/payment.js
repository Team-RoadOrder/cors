document.addEventListener('DOMContentLoaded', () => {
    const paymentButton = document.getElementById('paymentButton');
    const productElement = document.querySelector('.product');
    const requestSelect = document.getElementById('please');

    if (paymentButton) {
        paymentButton.addEventListener('click', () => {
            const itemId = productElement.dataset.itemId;
            const size = productElement.dataset.size;
            const request = requestSelect.value;

            const xhr = new XMLHttpRequest();
            xhr.open('POST', '/pay');
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
            xhr.send(JSON.stringify({
                itemId: itemId,
                size: size,
                request: request
            }));
        });
    }
});
