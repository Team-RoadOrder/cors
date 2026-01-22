let originProfileSrc = '';
let originBgSrc = '';

document.addEventListener('DOMContentLoaded', () => {

    //추가
    const tabButtons = document.querySelectorAll('.tab-nav-button');
    const sections = {
        'item-list': document.querySelector('.item-list'),
        'res-list': document.querySelector('.res-list'),
        'order-list': document.querySelector('.order-list')
    };

    const profileImg = document.getElementById('profileImg');
    const bgImageDisplay = document.getElementById('backgroundImage');

    if (profileImg) originProfileSrc = profileImg.src;
    if (bgImageDisplay) originBgSrc = bgImageDisplay.src;

    function handleMobileTabs() {
        if (window.innerWidth <= 512) {
            // [수정] 저장된 탭 정보 가져오기 (없으면 'item-list'를 기본값으로 사용)
            const savedTab = localStorage.getItem('activeTab') || 'item-list';

            // 1. 초기화: 모든 섹션 및 버튼 active 제거
            Object.values(sections).forEach(sec => { if(sec) sec.classList.remove('active'); });
            tabButtons.forEach(btn => btn.classList.remove('active'));

            // 2. 저장된 탭 활성화
            const targetBtn = document.querySelector(`.tab-nav-button[data-tab="${savedTab}"]`);
            if (targetBtn) targetBtn.classList.add('active');
            if (sections[savedTab]) sections[savedTab].classList.add('active');

            // 3. 버튼 클릭 이벤트
            tabButtons.forEach(button => {
                button.onclick = function() {
                    const targetTab = this.getAttribute('data-tab');

                    // [추가] 클릭한 탭 정보를 localStorage에 저장
                    localStorage.setItem('activeTab', targetTab);

                    // 클래스 교체 로직
                    tabButtons.forEach(btn => btn.classList.remove('active'));
                    this.classList.add('active');

                    Object.values(sections).forEach(sec => { if(sec) sec.classList.remove('active'); });
                    if (sections[targetTab]) sections[targetTab].classList.add('active');

                    window.scrollTo({ top: 0, behavior: 'smooth' });
                };
            });
        } else {
            Object.values(sections).forEach(sec => { if(sec) sec.classList.remove('active'); });
        }
    }

    handleMobileTabs();
    window.addEventListener('resize', handleMobileTabs);


    checkEmptyState();

    const inputs = document.querySelectorAll('.item-list .product input');
    inputs.forEach(input => {
        resizeInput(input);
        input.addEventListener('input', () => resizeInput(input));
    });

    loadList();

    // ============================================================
    // [수정] URL 파라미터 체크 로직 (if문 분리 및 안전장치 추가)
    // ============================================================
    const urlParams = new URLSearchParams(window.location.search);
    const alertType = urlParams.get('alert');

    if (alertType) { // alertType이 존재할 때만 실행

        // 1. noshop 일 때
        if (alertType === 'noshop') {
            openModal("WARN",
                `<p style="text-align:center; font-weight:bold;">매장 정보가 등록되지 않았습니다.</p>
                 <p style="text-align:center;"><span style="color: red">상품등록</span> 을 하려면 먼저<br>[매장정보수정]을 완료해주세요.</p>`,
                {
                    confirmText: '확인',
                    onConfirm: () => {
                        const infoBtn = document.getElementById('infoUpdateBtn');
                        const shopInfoSection = document.getElementById('shop-info');

                        // 버튼 클릭 트리거
                        if(infoBtn) infoBtn.click();

                        // 스크롤 이동
                        if(shopInfoSection) {
                            shopInfoSection.scrollIntoView({ behavior: 'smooth' });
                        }
                    }
                }
            );
        }
        if (alertType === 'nograph') {
            openModal("WARN",
                `<p style="text-align:center; font-weight:bold;">매장 정보가 등록되지 않았습니다.</p>
                 <p style="text-align:center;"><span style="color: blue">매출흐름</span>을 확인하려면 <br>[매장정보수정]을 완료해주세요.</p>`,
                {
                    confirmText: '확인',
                    onConfirm: () => {
                        const infoBtn = document.getElementById('infoUpdateBtn');
                        const shopInfoSection = document.getElementById('shop-info');

                        // 버튼 클릭 트리거
                        if(infoBtn) infoBtn.click();

                        // 스크롤 이동
                        if(shopInfoSection) {
                            shopInfoSection.scrollIntoView({ behavior: 'smooth' });
                        }
                    }
                }
            );
        }
        // 2. noauth 일 때 (else if로 분리해야 함!)
        else if (alertType === 'noauth') {
            openModal("WARN",
                `<p style="text-align:center; font-weight:bold;">권한이 없습니다.</p>
                 <p style="text-align:center;">최고 관리자에게 문의하여 주세요.</p>`,
                {
                    confirmText: '확인',
                    onConfirm: () => {
                    }
                }
            );
        }

        // 3. 처리가 끝난 후 URL에서 파라미터 제거 (깔끔하게)
        const newUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
        window.history.replaceState({path: newUrl}, '', newUrl);
    }
});

// ==========================================
// 2. 이미지 업로드 미리보기 기능
// ==========================================
const profileImg = document.getElementById('profileImg');
const profileInput = document.getElementById('profileImageInput');
const bgUpdateBtn = document.getElementById('bgUpdateBtn');
const bgInput = document.getElementById('backgroundImageInput');
const bgImageDisplay = document.getElementById('backgroundImage');
    // (1) 프로필 이미지 처리 부분
if (profileImg && profileInput) {
        profileImg.addEventListener('click', () => profileInput.click());
        profileInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = (e) => profileImg.src = e.target.result;
                reader.readAsDataURL(file);

                // [★ 추가된 코드] 사진을 바꿨다면, 강제로 수정 모드로 진입시킴
                // 텍스트 인풋 중 하나를 골라 readonly인지 확인
                const textInput = document.querySelector('#form input:not([type="file"])');
                if (textInput && textInput.hasAttribute('readonly')) {
                    infoUpdateBtn.click(); // "매장정보수정" 버튼을 프로그램이 대신 클릭해줌 -> 수정 모드 전환
                }
            }
        });
    }

    // (2) 배경 이미지 처리 부분도 똑같이
if (bgUpdateBtn && bgInput && bgImageDisplay) {
        bgUpdateBtn.addEventListener('click', () => bgInput.click());
        bgInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = (e) => bgImageDisplay.src = e.target.result;
                reader.readAsDataURL(file);

                // [★ 추가된 코드] 여기도 똑같이 추가
                const textInput = document.querySelector('#form input:not([type="file"])');
                if (textInput && textInput.hasAttribute('readonly')) {
                    infoUpdateBtn.click();
                }
            }
        });
    }


// ==========================================
    // 3. 매장 정보 수정 및 전송 기능 (통합됨)
    // ==========================================
const infoUpdateBtn = document.getElementById('infoUpdateBtn');
const shopDeleteBtn = document.getElementById('shopDeleteBtn');
const form = document.forms['form']; // HTML에 <form name="form"> 확인 필수
const cancelBtn = document.getElementById('cancelBtn');
    // 파일 인풋은 제외하고 텍스트 인풋만 선택 (스타일/readonly 제어용)
const formInputs = document.querySelectorAll('#form input:not([type="file"])');

if (infoUpdateBtn && form ) {
        infoUpdateBtn.addEventListener('click', () => {
            // 현재 상태가 '읽기 전용'인지 확인
            const isReadonly = formInputs[0].hasAttribute('readonly');

            if (isReadonly) {
                // ------------------------------------
                // [STEP 1] 수정 모드 진입 (잠금 해제)
                // ------------------------------------
                formInputs.forEach(input => {
                    input.removeAttribute('readonly');
                });

                infoUpdateBtn.textContent = '수정완료'; // 버튼 글자 변경
                infoUpdateBtn.style.backgroundColor = '#2196f3'; // 버튼 색 강조
                infoUpdateBtn.classList.add('edit-mode-btn');
                formInputs[0].focus(); // 첫 번째 칸에 포커스
                shopDeleteBtn.style.display = 'none';
                cancelBtn.style.display = 'block';
            } else {
                // ------------------------------------
                // [STEP 2] 수정 완료 -> 서버 전송
                // ------------------------------------
                const xhr = new XMLHttpRequest();
                const formData = new FormData();

                // 2. 텍스트 데이터 수동 추가 (append '키', '값')
                // HTML의 id를 기준으로 값을 가져옵니다.
                formData.append('shopName', document.getElementById('shopName_Input').value);
                formData.append('shopTime', document.getElementById('time').value);
                formData.append('shopCategory', document.getElementById('category').value);
                formData.append('shopAddress', document.getElementById('address').value);
                formData.append('shopTel', document.getElementById('tel').value);

                // 3. 파일 데이터 수동 추가
                // 파일 인풋 요소 가져오기
                const profileInput = document.getElementById('profileImageInput');
                const bgInput = document.getElementById('backgroundImageInput');

                if (profileInput && profileInput.files[0]) {
                    formData.append('profileImageFile', profileInput.files[0]); // 이름 변경
                }
                if (bgInput && bgInput.files[0]) {
                    formData.append('backgroundImageFile', bgInput.files[0]); // 이름 변경
                }

                // (확인용) 실제로 잘 담겼는지 콘솔에 출력해보기
                for (let pair of formData.entries()) {
                    console.log(pair[0] + ', ' + pair[1]);
                }

                xhr.onreadystatechange = () => {
                    if (xhr.readyState !== XMLHttpRequest.DONE) return;

                    if (xhr.status >= 200 && xhr.status < 400) {
                        const response = JSON.parse(xhr.responseText);
                        console.log(xhr.responseText);
                        switch (response.result) {
                            case 'FAILURE':
                                openModal("FAILURE", `<p>매장 정보 수정에 실패하였습니다. 입력 정보를 다시 확인해주세요.</p>`, {confirmText: '확인'});
                                break;
                            case 'NO_AUTH':
                                openModal("FAILURE", `<p>권한이 없습니다.</p>`, {confirmText: '확인'});
                                break;
                            case 'SUCCESS':
                                openModal("SUCCESS", `<p>매장 정보가 성공적으로 수정되었습니다.</p>`, {
                                    confirmText: '확인',
                                    onConfirm: () => { window.location.href = '/owner';
                                        // 다시 읽기 전용으로 변경 (잠금)
                                        formInputs.forEach(input => {
                                            input.setAttribute('readonly', true);
                                            input.style.borderBottom = 'none';
                                        });

                                        infoUpdateBtn.textContent = '매장정보수정';
                                        infoUpdateBtn.style.backgroundColor = '';
                                        infoUpdateBtn.classList.remove('edit-mode-btn');}
                                });
                                break;
                            default:
                                openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});
                        }
                    } else {
                        openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, {
                            confirmText: '확인',
                            onConfirm: () => { window.location.href = '/owner'; }
                        });
                    }
                };
                xhr.open('POST', '/owner/post-info');
                xhr.send(formData); // 수동으로 꽉 채운 데이터 전송!
            }
        });
}


if (cancelBtn) {
    cancelBtn.addEventListener('click', () => {
        // 1. 폼에 입력된 변경사항을 초기값(HTML에 렌더링된 값)으로 리셋
        form.reset();

        // 2. 모든 텍스트 인풋을 다시 '읽기 전용'으로 변경
        formInputs.forEach(input => {
            input.setAttribute('readonly', true);
        });
        if (profileImg) profileImg.src = originProfileSrc;
        if (bgImageDisplay) bgImageDisplay.src = originBgSrc;
        // 3. 버튼 가시성 원상복구
        cancelBtn.style.display = 'none';        // 취소 버튼 숨김
        shopDeleteBtn.style.display = 'block';   // 삭제 버튼 다시 보임

        // 4. 메인 수정 버튼 텍스트 및 스타일 초기화
        infoUpdateBtn.textContent = '매장정보 수정';
        infoUpdateBtn.style.backgroundColor = '';
        infoUpdateBtn.classList.remove('edit-mode-btn');
    });
}
/* ==========================================
   Input 너비 자동 조절 스크립트
========================================== */
function resizeInput(input) {
    // 임시 span을 만들어 글자 너비를 측정합니다.
    const span = document.createElement('span');
    span.style.font = getComputedStyle(input).font;
    span.style.visibility = 'hidden';
    span.style.position = 'absolute';
    span.style.whiteSpace = 'pre';
    span.textContent = input.value || input.placeholder;

    document.body.appendChild(span);
    // 글자 너비 + 약간의 여유공간(2px)
    input.style.width = (span.offsetWidth + 2) + 'px';
    document.body.removeChild(span);
}


// item리스트에 상품이 있는지 확인해서 없으면 상품을 등록해달라는 문구를 띄우는함수
function checkEmptyState() {
    // =========================================
    // 1. 등록상품 관리 (.item-list) 처리
    // =========================================
    const itemList = document.querySelector('.item-list');
    if (itemList) {
        const products = itemList.querySelectorAll('.product');
        const emptyMsg = itemList.querySelector('.empty');
        const emptySpring = emptyMsg ? emptyMsg.previousElementSibling : null;

        if (products.length > 0) {
            // 상품 있음 -> 숨김
            if (emptyMsg) emptyMsg.classList.add('hidden');
            if (emptySpring && emptySpring.classList.contains('spring')) {
                emptySpring.classList.add('hidden');
            }
        } else {
            // 상품 없음 -> 보임
            if (emptyMsg) emptyMsg.classList.remove('hidden');
            if (emptySpring && emptySpring.classList.contains('spring')) {
                emptySpring.classList.remove('hidden');
            }
        }
    }

    // =========================================
    // 2. 예약정보 확인 (.res-list) 처리
    // =========================================
    const resList = document.querySelector('.res-list');
    if (resList) {
        const resProducts = resList.querySelectorAll('.product');
        const resEmptyMsg = resList.querySelector('.empty');

        // res-list도 empty 바로 위의 요소를 가져옴
        const resSpring = resEmptyMsg ? resEmptyMsg.previousElementSibling : null;

        if (resProducts.length > 0) {
            // 예약 상품이 있음 -> 메세지와 빈 줄 숨기기
            if (resEmptyMsg) resEmptyMsg.classList.add('hidden');

            // 바로 위의 요소가 존재하고, 클래스가 확실히 'spring'일 때만 숨김 (제목 등 오작동 방지)
            if (resSpring && resSpring.classList.contains('spring')) {
                resSpring.classList.add('hidden');
            }
        } else {
            // 예약 상품이 없음 -> 메세지와 빈 줄 보이기
            if (resEmptyMsg) resEmptyMsg.classList.remove('hidden');

            // 바로 위의 요소가 존재하고, 클래스가 확실히 'spring'일 때만 보임
            if (resSpring && resSpring.classList.contains('spring')) {
                resSpring.classList.remove('hidden');
            }
        }
    }
}

/**@type{HTMLElement} */
const $item = document.getElementById('items');
/**@type{HTMLDivElement} */
const $itemList = $item.querySelector(':scope > .item-list')
/**@type{HTMLDivElement} */
const $resList = $item.querySelector(':scope > .res-list');

const loadList = () => {
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, {
                confirmText: '확인'
            });
        }
        const response = JSON.parse(xhr.responseText);
        switch (response.result) {
            case 'FAILURE':
                openModal("FAILURE", `<p>등록 상품정보를 불러오는데 실패하였습니다.</p>`, {confirmText: '확인'});
                break;
            case 'SUCCESS':
                const oldProducts = $itemList.querySelectorAll('.product');
                oldProducts.forEach(el => el.remove());
                const referenceNode = $itemList.querySelector('.empty');
                const items = response.items;
                items.reverse().forEach(item => {
                    // HTML 템플릿 생성 (백틱 사용)
                    // ★ 중요: item.itemName, item.price 등은 Entity 필드명과 같아야 함
                    const html = `
                    <form class="product" name="productForm" method="post">
                        <input type="hidden" name="id" value="${item.id}">
                        <input type="file" name="productImage" class="file-input" style="display: none;" accept="image/!*">
                        
                    <div class="image-wrapper">
                <img src="${item.imagePath ? '/images/' + item.imagePath : '/assets/images/ownermain/item-list/default.png'}" class="image" 
                     alt="상품이미지"></div>

                        <span class="itemTitle">
                             <input type="text" class="title" name="itemName" value="${item.itemName}" readonly required>
                             <input type="text" class="description" name="size" value="${item.size}" readonly>
                             <span style="font-size: 0.9rem"><input type="text" class="price" name="price" value="${Number(item.price).toLocaleString()}" readonly>원</span>
                        </span>

                        <span class="itemStatus">
                          <span class="type">
                            <input type="text" class="productType" name="style" value="${item.style}" readonly style="text-align: right; width: 40px;">
                              <span>/</span>
                              <input type="text" class="productType" name="mainCategory" value="${item.mainCategory}" readonly style="text-align: right; width: 40px;">
                              <span>/</span>
                              <input type="text" class="productCategory" name="subCategory" value="${item.subCategory}" readonly style="text-align: right; width: 40px;">
                          </span>
                         <span class="list-column">
                              <div class="color" style="display:flex; justify-content:flex-end;">
                                  <span style="margin-right:4px;">색상:</span>
                                  <input type="text" name="color" value="${item.color}" readonly style="text-align: right; width: 60px;">
                             </div>
                             <span class="button-container">
                                <button type="button" class="res-btn" >수정하기</button>
                                <button type="button" class="del-btn">삭제하기</button>
                                <button type="submit" class="save-btn" style="display:none;">저장하기</button>
                            </span>
                        </span>
                       </span>
                    </form>
                    `;

                    // 3. HTML을 DOM 요소로 변환하여 추가
                    referenceNode.insertAdjacentHTML('afterend', html);
                });
                checkEmptyState();

                // 동적으로 추가된 input들에게도 resize 이벤트 걸어주기
                const newInputs = $itemList.querySelectorAll('.product input');
                newInputs.forEach(input => {
                    resizeInput(input);
                    input.addEventListener('input', () => resizeInput(input));
                });
                break;
            default:
                openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});
        }

    }

    xhr.open('GET', '/owner/all-items')
    xhr.send();

}

// 부모이벤트로 위임받아서 수정하기/ 삭제하기 버튼 누르는조건문
if ($itemList) {
    $itemList.addEventListener('click', (e) => {
        // 클릭된 요소가 '수정하기' 버튼인지 확인
        if (e.target.classList.contains('res-btn')) {
            e.preventDefault(); // 버튼의 기본 동작 방지
            toggleEditMode(e.target);
        }

        // (참고) '삭제하기' 버튼 로직도 여기에 추가하면 좋습니다.
        if (e.target.classList.contains('del-btn')) {
            e.preventDefault();
            deleteProduct(e.target);
        }
        if (e.target.classList.contains('save-btn')) {
            e.preventDefault(); // submit 방지
            saveProduct(e.target); // 이름도 edit보다는 save가 더 직관적이죠?
        }
    });
}
// 모든 상태변경 대응  수락, 취소 , 확정
const changeStatus = ( a, b ) => {
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('reservationId', a);
    formData.append('status', b);
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
            case 'FAILURE':
                openModal("FAILURE", `<p>예약 확정에 실패하였습니다.</p>`, {confirmText: '확인'});
                break;
            case 'NO_AUTH':
                openModal("FAILURE", `<p>권한이 없습니다.</p>`, {confirmText: '확인'});
                break;
            case 'SUCCESS':
                openModal("SUCCESS", `<p>예약 정보가 성공적으로 변경되었습니다.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => {
                        location.reload();
                    }
                });
                break;
            default:
                openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});
        }
     };
     xhr.open('PATCH', '/owner/patch-reservation')
     xhr.send(formData);
            
}

// 취소하기/저장하기로 바뀌는버튼
function toggleEditMode(btn) {
    const form = btn.closest('form');
    const inputs = form.querySelectorAll('input');
    const saveBtn = form.querySelector('.save-btn');
    const delBtn = form.querySelector('.del-btn');

    // [수정된 부분] inputs[1]은 파일 입력창이라 readonly가 없을 수 있습니다.
    // 확실한 텍스트 입력창(.title)을 잡아서 확인해야 합니다.
    const titleInput = form.querySelector('.title');
    const isReadonly = titleInput.hasAttribute('readonly');

    if (isReadonly) {
        // [수정 모드로 전환]
        inputs.forEach(input => {
            // 파일 인풋은 제외하고 readonly 해제
            if (input.type !== 'file' && input.type !== 'hidden') {
                input.removeAttribute('readonly');
                // (선택사항) 수정 모드임을 티내기 위해 테두리 스타일 추가
                input.style.borderBottom = '1px solid #2196f3';
            }
        });
        btn.textContent = '취소하기';
        btn.style.backgroundColor = '#999';
        saveBtn.style.display = 'inline-block';
        delBtn.style.display = 'none';

        titleInput.focus();

    } else {
        // [다시 읽기 모드로 복구]
        inputs.forEach(input => {
            if (input.type !== 'file' && input.type !== 'hidden') {
                input.setAttribute('readonly', true);
                // 스타일 복구
                input.style.borderBottom = 'none';
            }
        });
        btn.style.backgroundColor = '';
        btn.textContent = '수정하기';
        saveBtn.style.display = 'none';
        delBtn.style.display = 'inline-block';

        form.reset(); // 취소했으니 값 원상복구

        // reset을 하면 사이즈가 안 맞을 수 있으니 리사이즈 다시 실행
        inputs.forEach(input => {
            if(typeof resizeInput === 'function') resizeInput(input);
        });
    }
}

// 삭제기능 구현함수
const deleteProduct = (btn) => {
    /**@type {HTMLFormElement}*/
    const form = btn.closest('form');
    const formData = new FormData();
    formData.append('id', form['id'].value);
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState === XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, {
                confirmText: '확인'
            });
        }
        const response = JSON.parse(xhr.responseText);
        switch (response.result) {
            case 'FAILURE':
                openModal("FAILURE", `<p>상품정보를 삭제하는데 실패하였습니다.</p>`, {confirmText: '확인'});
                break;
            case 'NO_AUTH':
                openModal("FAILURE", `<p>권한이 없습니다.</p>`, {confirmText: '확인'});
                break;
            case 'SUCCESS':
                openModal("SUCCESS", `<p>상품이 성공적으로 삭제되었습니다.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => {
                        loadList();
                    }
                });
                break;
            default:
                openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});
        }
    }
    xhr.open('DELETE', '/owner/delete-item');
    xhr.send(formData);
}
// 수정내용(저장) 함수
const saveProduct = (btn) => {
    /**@type {HTMLFormElement}*/
    const form = btn.closest('form');
    const formData = new FormData();
    formData.append('id', form['id'].value);
    formData.append('itemName', form['itemName'].value);
    formData.append('size', form['size'].value);
    formData.append('price', form['price'].value.replace(/,/g, ''));
    formData.append('price', form['price'].value);
    formData.append('style', form['style'].value);
    formData.append('mainCategory', form['mainCategory'].value);
    formData.append('subCategory', form['subCategory'].value);
    formData.append('color', form['color'].value);
    console.log("저장할 상품 ID:", formData.get("id"));
    console.log("변경된 이름:", formData.get("itemName"));

    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, {
                confirmText: '확인'
            });
        }
       const response = JSON.parse(xhr.responseText);
        switch (response.result) {
          case 'FAILURE':
              openModal("FAILURE", `<p>상품정보를 수정하는데 실패하였습니다.</p>`, {confirmText: '확인'});
              break;
            case 'NO_AUTH':
                openModal("FAILURE", `<p>권한이 없습니다.</p>`, {confirmText: '확인'});
                break;
          case 'SUCCESS':
              openModal("SUCCESS", `<p>상품이 성공적으로 수정되었습니다.</p>`, {
                  confirmText: '확인',
                  onConfirm: () => {
                      loadList();
                  }
              });
              break;
          default:
              openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});
        }
    };

    xhr.open('PATCH', '/owner/patch-item');
    xhr.send(formData);
}

// [기존 deleteShop 함수 수정 및 이벤트 리스너 연결]

// 1. 버튼 요소를 가져옵니다.


// 2. 이벤트 리스너를 연결합니다.
if (shopDeleteBtn) {
    shopDeleteBtn.addEventListener('click', () => {
        // 기존 deleteShop 함수 로직을 여기에 넣거나, 함수를 호출합니다.

        openModal("answer", "<p>정말 삭제하시겠습니까?<br>삭제된 매장은 복구할 수 없습니다.</p>", {
            confirmText: '탈퇴',
            cancelText: '취소',
            onConfirm: () => {
                const xhr = new XMLHttpRequest();
                xhr.onreadystatechange = () => {
                    if (xhr.readyState !== XMLHttpRequest.DONE) return;

                    if (xhr.status >= 200 && xhr.status < 300) {
                        const response = JSON.parse(xhr.responseText);
                        if (response.result === 'SUCCESS') {
                            openModal("SUCCESS", "<p>회원 탈퇴가 완료되었습니다.</p>", {
                                confirmText: '확인',
                                onConfirm: () => {
                                    location.href = '/logout'; // 로그아웃 처리
                                }
                            });
                        }
                        else if (response.result === 'NO_AUTH') {
                            openModal("WARN", // WARN이나 FAILURE 모달 사용 권장
                                `<p style="text-align:center; font-weight:bold;">권한이 없습니다.</p>
                                 <p style="text-align:center;">최고 관리자에게 문의하여 주세요.</p>`,
                                {
                                    confirmText: '확인',
                                    onConfirm: () => {
                                        // 권한이 없다고 해서 로그아웃 시킬 필요는 없으므로
                                        // location.href = '/logout'; 은 제거하거나 주석 처리하는 것이 자연스럽습니다.
                                    }
                                });
                        }
                        else {
                            openModal("WARN", "<p>탈퇴 처리에 실패하였습니다.</p>");
                        }
                    } else {
                        openModal("ERROR", `<p>서버 통신 오류 (${xhr.status})</p>`);
                    }
                };

                xhr.open('DELETE', '/owner/delete-shop');
                xhr.send();
            }
        });
    });
}

const updateStatus = (id, status) => {
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('id', id);
    formData.append('status', status);
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
            case 'FAILURE':
                openModal("FAILURE", `<p>성공적으로 완료되었습니다.</p>`, {confirmText: '확인'});
                break;
            case 'NO_AUTH':
                openModal("FAILURE", `<p>권한이 없습니다.</p>`, {confirmText: '확인'});
                break;
            case 'SUCCESS':
                openModal("SUCCESS", `<p>완료되었습니다.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => {
                        location.href = '/owner'
                    }
                });
                break;
            default:
                openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});
        }
    };
     xhr.open('PATCH', '/owner/patch-order-status')
     xhr.send(formData);

}