document.addEventListener("DOMContentLoaded", function() {
    // 1. URL 파라미터 확인
    const urlParams = new URLSearchParams(window.location.search);
    const openTab = urlParams.get('open');


    if (openTab) {
        // 2. data-tab 속성으로 요소를 찾습니다. (훨씬 정확함)
        const targetMenu = document.querySelector(`.menu .item[data-tab="${openTab}"]`);

        console.log("찾은 메뉴 요소:", targetMenu); // [디버깅용] 요소를 잘 찾았나 확인

        if (targetMenu) {
            targetMenu.click();
            setTimeout(() => {
                let targetClass = '';
                switch(openTab) {
                    case 'likes-shop':
                        targetClass = '.tab_likeShop_content';
                        break;
                    case 'likes-item':
                        targetClass = '.tab_likeProduct_content';
                        break;
                    case 'reservation':
                        targetClass = '.tab_reservation_content';
                        break;
                    case 'orders':
                        targetClass = '.tab_purchase_content';
                        break;
                    case 'profile':
                        targetClass = '.tab_profile_content';
                        break;
                    case 'address':
                        targetClass = '.tab_address_content';
                        break;
                    case 'custom':
                        targetClass = '.tab_custom_content';
                        break;
                    default:
                        targetClass = '.tab-content';
                }

                const targetElement = document.querySelector(targetClass);
                if (targetElement) {
                    targetElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
            }, 300);
        } else {
            console.warn("해당하는 탭 메뉴를 찾을 수 없습니다: " + openTab);
        }
    } else {
        // 3. 파라미터 없으면 기본값(홈)
        const homeLabel = document.querySelector('label.my');
        if(homeLabel) homeLabel.click();
    }

// 페이지 로딩 시(탭 이동 시) 기본적으로 '대기' 상태만 보이게 초기화
    setTimeout(() => filterStatus('대기'), 50);

});

// 이벤트 위임용
document.addEventListener('click', function(e) {
    // 1. 클릭한 요소가 'delete-btn' 클래스를 가진 요소인지 확인
    if (e.target && e.target.classList.contains('delete-btn')) {
        e.preventDefault();

        // 2. 버튼에 심어둔 data-id 값을 가져옴
        const reservationId = e.target.getAttribute('data-id');

        if (!reservationId) {
            alert("예약 정보를 찾을 수 없습니다.");
            return;
        }

        // 3. 모달창 띄우기 (기존에 쓰시던 openModal 활용)
        openModal("answer", "<p>정말 예약내역을 삭제하시겠습니까?</p>", {
            confirmText: '확인',
            onConfirm: () => {
                // 4. 서버로 삭제 요청 보내기
                deleteReservation(reservationId);
            },
            cancelText: '취소', onCancel: () => {

            }
        });
        return;
    }
    if (e.target && e.target.id === 'nameChangeButton') {
        e.preventDefault(); // type="button"이라 필수는 아니지만 안전하게
        const inputElement = document.getElementById('name');

        if (inputElement) {
            // 2. 입력값(.value)을 가져옵니다.
            changeName(inputElement.value);
        } else {
            alert("이름 입력창을 찾을 수 없습니다.");
        }
    }
    if (e.target && e.target.id === 'telChangeButton') {
        e.preventDefault(); // type="button"이라 필수는 아니지만 안전하게
        const inputElement = document.getElementById('phone');

        if (inputElement) {
            // 2. 입력값(.value)을 가져옵니다.
            changePhone(inputElement.value);
        } else {
            alert("전화번호 입력창을 찾을 수 없습니다.");
        }
    }
    const addressBtn = e.target.closest('#addressActionBtn');
    if (addressBtn) {
        e.preventDefault(); // type="button"이라도 습관적으로 넣어주면 좋음
        // 2. 요소 찾기 (버튼이 있다는 건 탭이 열려있다는 뜻이므로 안전함)
        const addressContainer = document.querySelector('.tab_address_content');

        // 혹시 모를 에러 방지
        if (!addressContainer) return;

        const inputAddress = addressContainer.querySelector('input[name="address"]');
        const inputDetail = addressContainer.querySelector('input[name="addressDetail"]');

        // 3. 함수 바로 호출 (리스너를 또 등록하지 않음!)
        toggleAddressMode(e.target, inputAddress, inputDetail);
    }
});
// [삭제 요청 함수]
const deleteReservation = (reservationId) => {
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) return;

        if (xhr.status >= 200 && xhr.status < 300) {
            const response = JSON.parse(xhr.responseText);
            if (response.result === 'SUCCESS') {
                openModal("alert", "<p>예약내역이 삭제되었습니다.</p>", {
                    onConfirm: () => {
                        // 성공 시 탭을 새로고침하여 목록 갱신
                        // 현재 열려있는 탭이 예약 탭이라면 리로드
                        loadTab('reservation', document.querySelector('.menu .item[data-tab="reservation"]'));
                    }
                });
            } else {
                openModal("WARN", "<p>삭제에 실패하였습니다.</p>");
            }
        } else {
            openModal("ERROR", "<p>서버 통신 오류</p>");
        }
    };

    // DELETE 메서드나 POST 메서드로 요청
    xhr.open('DELETE', `/reservation/delete?reservationId=${reservationId}`);
    // 만약 POST만 쓴다면: xhr.open('POST', '/reservation/cancel'); xhr.setRequestHeader...
    xhr.send();
}
/**
 * 탭 내용을 비동기로 불러오는 함수
 * @param menuName : 컨트롤러에 요청할 메뉴 파라미터 값 (orders, profile 등)
 * @param element  : 클릭한 메뉴 요소 (this) - 스타일 활성화용
 */
const loadTab = (menuName, element) => {
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, {confirmText: '확인'});
            return;
        }
        document.getElementById('dynamic-content').innerHTML = xhr.responseText;
        // 2. 메뉴 활성화 스타일 처리 (파란색 글씨 등)
        // 클릭한 요소(element)가 있을 때만 실행
        if (element) {
            // 모든 메뉴의 active 클래스 제거
            var allItems = document.querySelectorAll('.menu .item, .my');
            for (var i = 0; i < allItems.length; i++) {
                allItems[i].classList.remove('active');
            }
            // 클릭한 메뉴에 active 클래스 추가
            element.classList.add('active');
        }

     };
    const timestamp = new Date().getTime();
    xhr.open('GET', '/my/tab?menu=' + menuName + '&t=' + timestamp);
     xhr.send();

}
const filterStatus= (statusType) => {
    const rows = document.querySelectorAll('.res-item-row');
    const noDataMsg = document.getElementById('no-reservation-msg');
    let visibleCount = 0; // 화면에 보이는 아이템 개수 세기

    rows.forEach(row => {
        const itemStatus = row.getAttribute('data-status');
        let isVisible = false;

        if (statusType === '종료') {
            if (itemStatus === '완료' || itemStatus === '취소') {
                isVisible = true;
            }
        } else {
            if (itemStatus === statusType) {
                isVisible = true;
            }
        }

        if (isVisible) {
            // ★ [수정] flex가 아니라 block으로 해야 찌그러지지 않음 (CSS .list가 block이므로)
            row.style.display = 'block';
            visibleCount++;
        } else {
            row.style.display = 'none';
        }
    });

    // ★ [수정] 보이는 아이템이 0개면 "데이터 없음" 메시지를 보여줌
    if (visibleCount === 0) {
        if(noDataMsg) noDataMsg.style.display = 'block';
    } else {
        if(noDataMsg) noDataMsg.style.display = 'none';
    }
}

const changeName = (name) => {
    if (!name || name.trim().length < 2) {
        alert("이름을 2글자 이상 입력해주세요.");
        return;
    }

    const xhr = new XMLHttpRequest();
    // FormData 객체 생성
    const formData = new FormData();
    formData.append('name', name);

    xhr.onreadystatechange = () => {
        // 요청이 완료되지 않았으면 중단
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }

        // 요청 완료 후 상태 코드 확인
        if (xhr.status >= 200 && xhr.status < 300) {
            // ★ 중요: 응답 처리는 여기서 해야 합니다.
            try {
                const response = JSON.parse(xhr.responseText);

                switch (response.result) {
                    case 'SUCCESS':
                        openModal("SUCCESS", "<p>이름이 변경되었습니다.</p>", {
                            confirmText: '확인',
                            onConfirm: () => {
                                // 성공 시 프로필 탭 새로고침
                                loadTab('profile', document.querySelector('.menu .item[data-tab="profile"]'));
                                // 혹은 전체 새로고침: window.location.reload();
                            }
                        });
                        break;
                    case 'FAILURE':
                        openModal("WARN", `<p>이름 변경에 실패하였습니다.</p>`, {confirmText: '확인'});
                        break;
                    default:
                        openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});
                }
            } catch (e) {
                console.error("JSON 파싱 에러:", e);
                openModal("ERROR", "<p>응답 데이터 형식이 올바르지 않습니다.</p>");
            }
        } else {
            // HTTP 에러 처리 (400, 500 등)
            openModal("ERROR", `<p>서버 통신 오류 (${xhr.status})</p>`, {confirmText: '확인'});
        }
    };
    xhr.open('PATCH', '/my/name');
    xhr.send(formData);
}
const changePhone = (phone) => {
    const xhr = new XMLHttpRequest();
    // FormData 객체 생성
    const formData = new FormData();
    formData.append('phone', phone);

    xhr.onreadystatechange = () => {
        // 요청이 완료되지 않았으면 중단
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status >= 200 && xhr.status < 300) {
            try {
                const response = JSON.parse(xhr.responseText);

                switch (response.result) {
                    case 'SUCCESS':
                        openModal("SUCCESS", "<p>전화번호가 변경되었습니다.</p>", {
                            confirmText: '확인',
                            onConfirm: () => {
                                // 성공 시 프로필 탭 새로고침
                                loadTab('profile', document.querySelector('.menu .item[data-tab="profile"]'));
                                // 혹은 전체 새로고침: window.location.reload();
                            }
                        });
                        break;
                    case 'FAILURE':
                        openModal("WARN", `<p>이름 변경에 실패하였습니다.</p>`, {confirmText: '확인'});
                        break;
                    default:
                        openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});
                }
            } catch (e) {
                console.error("JSON 파싱 에러:", e);
                openModal("ERROR", "<p>응답 데이터 형식이 올바르지 않습니다.</p>");
            }
        } else {
            // HTTP 에러 처리 (400, 500 등)
            openModal("ERROR", `<p>서버 통신 오류 (${xhr.status})</p>`, {confirmText: '확인'});
        }
    };
    xhr.open('PATCH', '/my/phone');
    xhr.send(formData);
}

/**
 * 주소 수정 모드 토글 및 저장 처리 함수
 * @param {HTMLElement} btn - 클릭된 버튼 요소
 * @param {HTMLElement} addrInput - 주소 인풋 요소
 * @param {HTMLElement} detailInput - 상세주소 인풋 요소
 */

const  toggleAddressMode = (btn, addrInput, detailInput) => {
    // 현재 버튼 텍스트가 '수정'인지 확인
    if (btn.innerText.trim() === '수정') {
        addrInput.removeAttribute('readonly');
        detailInput.removeAttribute('readonly');
        addrInput.focus();
        btn.innerText = '완료';

    } else {
        const xhr = new XMLHttpRequest();
        const formData = new FormData();
        formData.append('address', addrInput.value);
        formData.append('addressDetail', detailInput.value);
        xhr.onreadystatechange = () => {
            if (xhr.readyState !== XMLHttpRequest.DONE) {
                return;
            }
            if (xhr.status >= 200 && xhr.status < 300) {
                try {
                    const response = JSON.parse(xhr.responseText);

                    switch (response.result) {
                        case 'SUCCESS':
                            openModal("SUCCESS", "<p>주소가 변경되었습니다.</p>", {
                                confirmText: '확인',
                                onConfirm: () => {
                                    // 성공 시 프로필 탭 새로고침
                                    loadTab('address', document.querySelector('.menu .item[data-tab="address"]'));
                                    // 혹은 전체 새로고침: window.location.reload();

                                }
                            });
                            break;
                        case 'FAILURE':
                            openModal("WARN", `<p>이름 변경에 실패하였습니다.</p>`, {confirmText: '확인'});
                            break;
                        default:
                            openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});
                    }
                } catch (e) {
                    console.error("JSON 파싱 에러:", e);
                    openModal("ERROR", "<p>응답 데이터 형식이 올바르지 않습니다.</p>");
                }
            } else {
                // HTTP 에러 처리 (400, 500 등)
                openModal("ERROR", `<p>서버 통신 오류 (${xhr.status})</p>`, {confirmText: '확인'});
            }

        }
        xhr.open('PATCH', '/my/address');
        xhr.send(formData);
    }
}

const toggleLikeShop = (e, shopId) => {
    // [중요] 부모 <a> 태그로 클릭 이벤트가 전파되는 것을 막음
    if(e) {
        e.preventDefault();
        e.stopPropagation();
    }

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

        try {
            const response = JSON.parse(xhr.responseText);
            switch (response.result) {
                case "FAILURE_SESSION":
                    openModal("FAILURE_SESSION", `<p>세션이 만료되었습니다. 다시 로그인해주세요.</p>`, {
                        confirmText: '확인',
                        onConfirm: () => { location.href = '/login'; }
                    });
                    break;
                case 'FAILURE':
                    openModal("FAILURE", `<p>관심매장 등록을 취소하였습니다.</p>`, {confirmText: '확인', onConfirm: () => {
                            if (window.location.search.includes('open=likes-shop')) {
                                window.location.reload();
                            } else {
                                // 다른 페이지(예: 상품상세)에서 온 거라면? -> 이동
                                location.href = "/my?open=likes-shop";
                            }
                        }});
                    break;
                case 'SUCCESS':
                    openModal("SUCCESS", `<p>관심매장에 등록되었습니다.</p>`, {
                        confirmText: '확인',
                        onConfirm: () => {
                            if (window.location.search.includes('open=likes-shop')) {
                                window.location.reload();
                            } else {
                                // 다른 페이지(예: 상품상세)에서 온 거라면? -> 이동
                                location.href = "/my?open=likes-shop";
                            }
                        }
                    });
                    break;
                default:
                    openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});
            }
        } catch (error) {
            console.error(error);
            openModal("ERROR", `<p>응답 처리 중 오류가 발생했습니다.</p>`, {confirmText: '확인'});
        }
    }
    xhr.open('POST', '/shop/like')
    xhr.send(formData);
}

const toggleLikeItems = (shopId, itemId) => {
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

        try {
            const response = JSON.parse(xhr.responseText);
            switch (response.result) {
                case "FAILURE_SESSION":
                    openModal("FAILURE_SESSION", `<p>세션이 만료되었습니다. 다시 로그인해주세요.</p>`, {
                        confirmText: '확인',
                        onConfirm: () => { location.href = '/login'; }
                    });
                    break;
                case 'FAILURE':
                    openModal("FAILURE", `<p>관심매장 등록을 취소하였습니다.</p>`, {confirmText: '확인', onConfirm: () => {
                            if (window.location.search.includes('open=likes-item')) {
                                window.location.reload();
                            } else {
                                // 다른 페이지(예: 상품상세)에서 온 거라면? -> 이동
                                location.href = "/my?open=likes-item";
                            }
                        }});
                    break;
                case 'SUCCESS':
                    openModal("SUCCESS", `<p>관심매장에 등록되었습니다.</p>`, {
                        confirmText: '확인',
                        onConfirm: () => {
                            if (window.location.search.includes('open=likes-item')) {
                                window.location.reload();
                            } else {
                                // 다른 페이지(예: 상품상세)에서 온 거라면? -> 이동
                                location.href = "/my?open=likes-item";
                            }
                        }
                    });
                    break;
                default:
                    openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});
            }
        } catch (error) {
            console.error(error);
            openModal("ERROR", `<p>응답 처리 중 오류가 발생했습니다.</p>`, {confirmText: '확인'});
        }
    }
    xhr.open('POST', '/item/like')
    xhr.send(formData);
}


const deleteMember = (email) => {
    // 1. 탈퇴 확인 모달 띄우기
    openModal("answer", "<p>정말 탈퇴하시겠습니까?<br>삭제된 계정은 복구할 수 없습니다.</p>", {
        confirmText: '탈퇴',
        cancelText: '취소',
        onConfirm: () => {
            // 2. 확인 버튼 클릭 시 AJAX 요청 전송
            const xhr = new XMLHttpRequest();

            // 응답 처리 리스너
            xhr.onreadystatechange = () => {
                if (xhr.readyState !== XMLHttpRequest.DONE) {
                    return;
                }

                if (xhr.status >= 200 && xhr.status < 300) {
                    try {
                        const response = JSON.parse(xhr.responseText);
                        if (response.result === 'SUCCESS') {
                            // 3. 성공 시 알림 후 메인/로그인 페이지로 이동
                            openModal("SUCCESS", "<p>회원 탈퇴가 완료되었습니다.<br>이용해 주셔서 감사합니다.</p>", {
                                confirmText: '확인',
                                onConfirm: () => {
                                    // 탈퇴했으므로 로그아웃 처리 후 이동
                                    location.href = '/logout';
                                }
                            });
                        } else {
                            openModal("WARN", "<p>탈퇴 처리에 실패하였습니다.</p>");
                        }
                    } catch (e) {
                        console.error(e);
                        openModal("ERROR", "<p>응답 처리 중 오류가 발생했습니다.</p>");
                    }
                } else {
                    openModal("ERROR", `<p>서버 통신 오류 (${xhr.status})</p>`);
                }
            };

            // 서버로 요청 전송 (Body 없이 DELETE 메서드만 호출해도, 서버에서 Session으로 유저를 찾음)
            xhr.open('DELETE', '/my/delete-user');
            xhr.send();
        }
    });
}