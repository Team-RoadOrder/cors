

// 2. 초기화 (페이지 로드 시 '전체(1)' 카테고리 보여주기)
document.addEventListener('DOMContentLoaded', () => {
    changeCategory('1');
});

function changeCategory(categoryValue) {
    const shopId = new URL(location.href).searchParams.get('shopId') ?? '0';

    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }

        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, {confirmText: '확인'});
            return;
        }

        try {
            const items = JSON.parse(xhr.responseText);
            renderItems(items);
        } catch (e) {
            console.error("데이터 파싱 실패:", e);
        }
    };
    xhr.open('GET', `/shop/items?shopId=${shopId}&category=${categoryValue}`);
    xhr.send();
}

// 4. 화면 렌더링 함수
function renderItems(items) {
    const gridContainer = document.getElementById('productGrid');
    gridContainer.innerHTML = ''; // 기존 목록 비우기

    if (items.length === 0) {
        openModal("Error", "<p>등록된 상품이없습니다.</p>", { confirmText: '확인' })
    }

    items.forEach(item => {
        // 가격에 콤마 찍기 (412000 -> 412,000)
        const formattedPrice = item.price.toLocaleString() + '원';
        const imgSrc = item.imagePath ? `/images/${item.imagePath}` : '/assets/images/default.png';
        // HTML 문자열 생성
        const html = `
            <a class="box" href="/item/?shopId=${item.shopId}&id=${item.id}">
                <span class="image-wrapper">
                    <div class="image" style="background-image: url('${imgSrc}');"></div>
                </span>
                <span class="text-container">
                    <span class="title">${item.itemName}</span>
                    <span class="price">${formattedPrice}</span>
                </span>
            </a>
        `;

        // 그리드에 추가
        gridContainer.insertAdjacentHTML('beforeend', html);
    });
}

const toggleLikeShop = (shopId) => {
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
         const response = JSON.parse(xhr.responseText);
         switch (response.result) {
             case "FAILURE_SESSION":
                 openModal("FAILURE_SESSION", `<p>세션이 만료되었습니다. 다시 로그인해주세요.</p>`, {
                     confirmText: '확인',
                     onConfirm: () => { location.href = '/login'; }
                 });
                 break;
             case 'FAILURE':
                 openModal("FAILURE", `<p>관심매장 등록을 취소하였습니다.</p>`, {confirmText: '확인'});
                 break;
             case 'SUCCESS':
                 openModal("SUCCESS", `<p>관심매장에 등록되었습니다..</p>`, {
                     confirmText: '확인',
                     onConfirm: () => {
                         location.href="/my?open=likes-shop"
                     }
                 });
                 break;
             default:
                 openModal("WARN", `<p>서버 응답 오류가 발생했습니다.</p>`, {confirmText: '확인'});

         }
     }
        xhr.open('POST', '/shop/like')
        xhr.send(formData);
}