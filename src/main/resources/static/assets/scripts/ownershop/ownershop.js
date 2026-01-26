/* --- 추가: [A. 모바일 가이드 오버레이 로직] --- (이 위치로 이동) */
const overlay = document.getElementById('mobileGuideOverlay');
const btnTodayClose = document.getElementById('btnTodayClose');
const btnClose = document.getElementById('btnCloseOverlay');

if (overlay && btnTodayClose && btnClose) {
    const checkOverlay = () => {
        const hideExpiry = localStorage.getItem('mobileGuideHideExpiry');
        const now = new Date().getTime();

        // 32rem(512px) 이하일 때만 작동
        if (window.innerWidth <= 512) {
            if (!hideExpiry || now > parseInt(hideExpiry)) {
                overlay.style.display = 'flex';
                document.body.style.overflow = 'hidden';
            }
        } else {
            overlay.style.display = 'none';
            document.body.style.overflow = '';
        }
    };

    // 초기 실행
    checkOverlay();

    // 닫기 버튼
    btnClose.addEventListener('click', () => {
        overlay.style.display = 'none';
        document.body.style.overflow = '';
    });

    // 오늘 하루 그만보기 (24시간)
    btnTodayClose.addEventListener('click', () => {
        const expiryDate = new Date().getTime() + (24 * 60 * 60 * 1000);
        localStorage.setItem('mobileGuideHideExpiry', expiryDate.toString());
        overlay.style.display = 'none';
        document.body.style.overflow = '';
    });

    // 리사이즈 대응
    window.addEventListener('resize', checkOverlay);
}


/**@type {HTMLInputElement} */
const imageInput = document.getElementById('imageInput');
/**@type {HTMLSpanElement} */
const imageCount = document.getElementById('imageCount');
const imageSection = document.getElementById('item-images');
const mainImageIndexInput = document.getElementById('mainImageIndex');


let currentFiles = [];

imageInput.addEventListener('change', (e) => {
    // 새로 들어온 파일들
    const newFiles = Array.from(e.target.files);

    // 1. 개수 제한 체크 (기존 파일 + 새 파일)
    if (currentFiles.length + newFiles.length > 5) {
        alert("최대 5장까지 가능합니다.");
        // 파일을 선택했다가 취소한 효과를 주기 위해 input 초기화 필요시 로직 추가 가능
        // 여기서는 기존 파일 유지를 위해 별도 초기화는 안 함 (DataTransfer가 덮어씌움)
        return;
    }

    // 2. 파일 배열 합치기 (기존 파일 뒤에 이어붙이기)
    currentFiles = currentFiles.concat(newFiles);

    // 3. 화면 다시 그리기 & input 태그 동기화
    renderImages();
    updateInputFiles();
});

// 화면에 이미지를 그려주는 함수 (수정버전)
function renderImages() {
    // 1. [수정] 통째로 지우지 않고, '미리보기 상자(.sub-image-wrapper)'만 골라서 지웁니다.
    // 이렇게 하면 업로드 버튼이나 다른 디자인 요소는 그대로 유지됩니다.
    const existingWrappers = imageSection.querySelectorAll('.sub-image-wrapper');
    existingWrappers.forEach(el => el.remove());

    currentFiles.forEach((file, index) => {
        const reader = new FileReader();
        reader.onload = (event) => {
            const wrapper = document.createElement('div');
            wrapper.className = 'sub-image-wrapper';

            // 현재 메인 인덱스와 같으면 하이라이트
            const currentMainIndex = parseInt(mainImageIndexInput.value) || 0;
            if (index === currentMainIndex) wrapper.classList.add('is-main');

            wrapper.innerHTML = `
                <img class="img" src="${event.target.result}" alt="미리보기">
                <span class="close" data-index="${index}">
                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
                </span>`;

            // [클릭 이벤트] 메인 이미지 변경
            wrapper.addEventListener('click', (e) => {
                if (e.target.classList.contains('close')) return;

                document.querySelectorAll('.sub-image-wrapper').forEach(w => w.classList.remove('is-main'));
                wrapper.classList.add('is-main');
                mainImageIndexInput.value = index;
            });

            // [삭제 이벤트] X 버튼 클릭
            wrapper.querySelector('.close').addEventListener('click', (e) => {
                e.stopPropagation();
                const removeIndex = parseInt(e.target.dataset.index);

                // 배열에서 해당 파일 삭제
                currentFiles.splice(removeIndex, 1);

                // 메인 이미지 인덱스 재조정 로직
                let newMainIndex = parseInt(mainImageIndexInput.value);
                if (removeIndex === newMainIndex) {
                    newMainIndex = 0;
                } else if (removeIndex < newMainIndex) {
                    newMainIndex--;
                }
                mainImageIndexInput.value = newMainIndex;

                renderImages(); // 다시 그리기
                updateInputFiles(); // input 동기화
            });

            // 2. [수정] appendChild 위치 주의
            // 만약 '+ 버튼'이 이미지 뒤에 있어야 한다면,
            // appendChild 대신 insertBefore 등을 써야 할 수도 있습니다.
            // 일단은 기존처럼 appendChild로 붙입니다.
            imageSection.appendChild(wrapper);
        };
        reader.readAsDataURL(file);
    });

    updateImageCount();
}
// [핵심 기능] 자바스크립트 배열(currentFiles)과 input 태그를 일치시키는 함수
function updateInputFiles() {
    const dataTransfer = new DataTransfer();
    currentFiles.forEach(file => dataTransfer.items.add(file));
    imageInput.files = dataTransfer.files; // 여기서 input 내용이 실제로 바뀝니다!
}

function updateImageCount() {
    imageCount.innerText = `(${currentFiles.length}/5)`;
}

// xhr폼데이터 보내기
/**@type {HTMLFormElement} */
const $form = document.forms['form'];

$form.addEventListener('submit', (e) => {
    e.preventDefault();

    // 변수 선언
    const itemName = $form['itemName'].value.trim();
    const color = $form['color'].value.trim();
    const size = $form['size'].value.trim();
    const price = $form['price'].value.trim();
    const style = $form['style'].value.trim();

    // 카테고리 및 이미지 관련 변수
    const mainCategory = document.getElementById('inputMainCategory').value;
    const subCategory = document.getElementById('inputSubCategory').value;
    const detailCategory = document.getElementById('inputDetailCategory').value;
    const mainImageIndex = document.getElementById('mainImageIndex').value;
    const imageInput = document.getElementById('imageInput');
    const imageFiles = imageInput.files;

    // 상품 이름
    if (!itemName) {
        openModal("FAILURE", `<p>상품 등록에 실패하였습니다.<br>상품 이름을 확인해주세요.</p>`, {
            confirmText: '확인',
            onConfirm: () => { $form['itemName'].focus(); }
        });
        return;
    }
    if (itemName.length < 2 || itemName.length > 50) {
        openModal("FAILURE", `<p>상품 이름은 2자 이상,<br>50자 이하로 입력해주세요.</p>`, {
            confirmText: '확인',
            onConfirm: () => { $form['itemName'].focus(); }
        });
        return;
    }
    const namePattern = /^[가-힣a-zA-Z0-9\s()[\]_&\/-]+$/;
    if (!namePattern.test(itemName)) {
        openModal("FAILURE", `<p>상품 이름에 사용 불가능한<br>특수문자가 포함되어 있습니다.<br>(허용 기호: [ ] ( ) - _ &)</p>`, {
            confirmText: '확인',
            onConfirm: () => { $form['itemName'].focus(); }
        });
        return;
    }
    // 색상
    if (!color) {
        openModal("FAILURE", `<p>색상을 입력해주세요</p>`, {
            confirmText: '확인',
            onConfirm: () => { $form['color'].focus(); }
        });
        return;
    }
    const colorPattern = /^[가-힣a-zA-Z\s]+$/;
    if (!colorPattern.test(color)) {
        openModal("FAILURE", `<p>색상에 특수문자나 숫자는<br>포함될 수 없습니다.<br>(예: 블랙, Navy)</p>`, {
            confirmText: '확인',
            onConfirm: () => { $form['color'].focus(); }
        });
        return;
    }


    if (size) {
        const sizePattern = /^[^,\s]+(\s*,\s*[^,\s]+)*$/;
        if (!sizePattern.test(size)) {
            openModal("FAILURE", `<p>사이즈 형식이 올바르지 않습니다.<br>쉼표(,)로 구분하여 입력해주세요.<br>(예: S,M,L)</p>`, {
                confirmText: '확인',
                onConfirm: () => { $form['size'].focus(); }
            });
            return;
        }
    }
    const allowedStyles = ['스트릿', '미니멀', '댄디', '캐주얼', '빈티지', '모던', '스포티', '페미닌'];

    if (!style) {
        openModal("FAILURE", `<p>스타일을 선택해주세요.</p>`, {
            confirmText: '확인',
            onConfirm: () => { $form['style'].focus(); }
        });
        return;
    }
    if (!allowedStyles.includes(style)) {
        openModal("FAILURE", `<p>유효하지 않은 스타일입니다.<br>목록에서 선택해주세요.</p>`, {
            confirmText: '확인',
            onConfirm: () => { $form['style'].focus(); }
        });
        return;
    }

    // 가격
    if (!price || isNaN(price) || parseInt(price) <= 0) {
        openModal("FAILURE", `<p>가격을 올바르게 입력해주세요.</p>`, {
            confirmText: '확인',
            onConfirm: () => { $form['price'].focus(); }
        });
        return;
    }

    // 카테고리
    if (!mainCategory || !subCategory) {
        openModal("FAILURE", `<p>카테고리를 최소 중분류까지<br>선택해주세요.</p>`, {
            confirmText: '확인'
        });
        return;
    }

    // 이미지
    if (imageFiles.length === 0) {
        openModal("FAILURE", `<p>상품 이미지를<br>최소 1장 등록해주세요.</p>`, {
            confirmText: '확인'
        });
        return;
    }

    // --- 2. FormData 생성 및 데이터 추가 ---
    const formData = new FormData();

    formData.append('itemName', itemName);
    formData.append('color', color);
    formData.append('size', size);
    formData.append('price', price);
    formData.append('style', style);
    formData.append('mainCategory', mainCategory);
    formData.append('subCategory', subCategory);
    formData.append('detailCategory', detailCategory);
    let fileList = Array.from(imageFiles);

    // 2. 사용자가 선택한 메인 인덱스 번호 (예: 2)
    const targetIndex = parseInt(mainImageIndex);

    // 3. 해당 인덱스를 배열의 맨 앞(0번)으로 이동
    if (targetIndex >= 0 && targetIndex < fileList.length) {
        const mainFile = fileList[targetIndex]; // 선택된 파일 저장
        fileList.splice(targetIndex, 1);      // 원래 위치에서 제거
        fileList.unshift(mainFile);           // 맨 앞으로 이동
    }
    fileList.forEach((file) => {
        formData.append('images', file);
    });

    // --- 3. 서버 전송 ---
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) return;

        if (xhr.status >= 200 && xhr.status < 400) {
            const response = JSON.parse(xhr.responseText);
            switch (response.result) {
                case 'FAILURE':
                    openModal("FAILURE", `<p>상품 등록에 실패하였습니다.<br>입력 정보를 다시 확인해주세요.</p>`, {confirmText: '확인'});
                    break;
                case 'SUCCESS':
                    openModal("SUCCESS", `<p>상품이 성공적으로 등록되었습니다.</p>`, {
                        confirmText: '확인',
                        onConfirm: () => { window.location.href = '/owner'; }
                    });
                    break;
                default:
                    openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});
            }
        } else {
            openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, {confirmText: '확인'});
        }
    };

    xhr.open('POST', '/owner/shop');
    xhr.send(formData);
});

