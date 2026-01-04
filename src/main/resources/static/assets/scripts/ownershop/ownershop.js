
/**@type {HTMLInputElement} */
const imageInput = document.getElementById('imageInput');
/**@type {HTMLSpanElement} */
const imageCount = document.getElementById('imageCount');
const imageSection = document.getElementById('item-images');
const mainImageIndexInput = document.getElementById('mainImageIndex');

// 현재 선택된 파일들을 담아둘 배열 (이게 있어야 삭제가 제대로 됨)
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
                <span class="close" data-index="${index}">x</span>
            `;

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
    const itemName = $form['itemName'].value.trim();
    const color = $form['color'].value.trim();
    const size = $form['size'].value.trim();
    const price = $form['price'].value.trim();
    const style = $form['style'].value.trim();
    const mainCategory = document.getElementById('inputMainCategory').value;
    const subCategory = document.getElementById('inputSubCategory').value;
    const detailCategory = document.getElementById('inputDetailCategory').value;
    const mainImageIndex = document.getElementById('mainImageIndex').value;
    const imageFiles = imageInput.files;

    if (!itemName) {
        alert("상품 이름을 입력해주세요.");
        $form['itemName'].focus();
        return;
    }
    if (!color) {
        alert("색상을 입력해주세요.");
        $form['color'].focus();
        return;
    }
   /* if (!size) {
        alert("사이즈를 입력해주세요.");
        $form['size'].focus();
        return;
    }*/
    if (!style ){
        alert('스타일을 입력해주세요.');
        $form['style'].focus();
        return;
    }
    if (!price || isNaN(price) || parseInt(price) <= 0) {
        alert("가격을 올바르게 입력해주세요.");
        $form['price'].focus();
        return;
    }
    if (!mainCategory || !subCategory) {
        alert("카테고리를 최소 중분류까지 선택해주세요.");
        return;
    }
    if (imageFiles.length === 0) {
        alert("상품 이미지를 최소 1장 등록해주세요.");
        return;
    }

    // --- 3. FormData 생성 및 하나씩 append ---
    const formData = new FormData();

    // 텍스트 데이터 추가
    formData.append('itemName', itemName);
    formData.append('color', color);
    formData.append('size', size);
    formData.append('price', price);
    formData.append('style', style);
    // 카테고리 데이터 추가
    formData.append('mainCategory', mainCategory);
    formData.append('subCategory', subCategory);
    formData.append('detailCategory', detailCategory);

    // 메인 이미지 설정 정보 추가
    formData.append('mainImageIndex', mainImageIndex);

    // 이미지 파일들 추가 (동일한 key 이름인 'images'로 여러 개 추가)
    for (let i = 0; i < imageFiles.length; i++) {
        formData.append('images', imageFiles[i]);
    }

    // --- 4. XHR 전송 ---
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) return;

        if (xhr.status >= 200 && xhr.status < 400) {
            const response = JSON.parse(xhr.responseText);
            switch (response.result) {
                case 'FAILURE':
                    openModal("FAILURE", `<p>상품 등록에 실패하였습니다. 입력 정보를 다시 확인해주세요.</p>`, {confirmText: '확인'});
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

