
document.addEventListener("DOMContentLoaded", () => {
    changeCategory('1');
    renderShops();

});

const changeCategory = (categoryValue) => {
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }

        // 1. HTTP 상태 코드가 에러인 경우
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, {confirmText: '확인'});
            return;
        }

        // 2. JSON 변환 시도 (여기서 에러가 날 수 있으므로 안전장치 추가)
        try {
            const items = JSON.parse(xhr.responseText);
            renderItems(items);
        } catch (e) {
            console.error("데이터 파싱 실패:", e);
            openModal("ERROR", `<p>서버에서 올바르지 않은 데이터가 도착했습니다.</p>`, {confirmText: '확인'});
        }
    };

    xhr.open('GET', `/items?style=${categoryValue}`);
    xhr.send();
}

// 화면에 HTML을 그려주는 함수 (코드 분리를 위해 따로 뺌)
const  renderItems = (items) => {
    const gridContainer = document.getElementById('productGrid');
    gridContainer.innerHTML = ''; // 기존 목록 비우기

    if (items.length === 0) {
        gridContainer.innerHTML = '<p>해당 스타일의 상품이 없습니다.</p>';
        return;
    }

    items.forEach(item => {
        // 이미지 경로 처리 (컨트롤러에서 imagePath를 잘 넘겨줬다고 가정)
        // 만약 imagePath가 null이면 기본 이미지 사용
        const imgSrc =item.imagePath ? '/images/' + item.imagePath : '/assets/images/ownermain/item-list/default.png';

        // 가격 포맷팅 (1000 -> 1,000)
        const formattedPrice = Number(item.price).toLocaleString() + '원';

        const html = `
            <div class="box">
                <a href="/item/?shopId=${item.shopId}&id=${item.id}"><div class="image" style="background-image: url('${imgSrc}'); background-size: cover; background-position: center; cursor: pointer"></div></a>
                <div class="text-container">
                    <span class="title">${item.itemName}</span>
                    <span class="price">${formattedPrice}</span>
                </div>
            </div>
        `;
        gridContainer.insertAdjacentHTML('beforeend', html);
    });
}

/** @type{HTMLDivElement} */
const $shop = document.getElementById('popularShop');
const renderShops = () => {
    // HTML에 정의된 인기 매장 섹션의 ul 태그 선택
    const $shopList = document.querySelector('#popularShop .list');

    const xhr = new XMLHttpRequest();
    // MainController에 정의된 /all-shops 엔드포인트 호출
    xhr.open('GET', '/all-shops');

    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }

        if (xhr.status < 200 || xhr.status >= 400) {
            console.error("매장 목록을 불러오는데 실패했습니다.");
            return;
        }

        try {
            // 서버에서 받은 JSON 데이터를 자바스크립트 객체 배열로 변환
            const shops = JSON.parse(xhr.responseText);

            // 기존 목록 비우기
            $shopList.innerHTML = '';

            // 데이터가 없을 경우 처리
            if (shops.length === 0) {
                $shopList.innerHTML = '<li class="item">등록된 매장이 없습니다.</li>';
                return;
            }

            // 배열을 순회하며 HTML 생성
            shops.forEach(shop => {
                // 이미지 경로 처리 (DB에는 파일명만 저장되어 있다고 가정)
                // ShopInfoEntity의 필드명은 profileImage 입니다.
                const imgSrc = shop.profileImage
                    ? `${shop.profileImage}`
                    : 'https://static.upbit.com/logos/USDT.png'; // 기본 이미지

                const html = `
                    <a class="item" href="/shop/?shopId=${shop.shopId}">
                        <img alt="${shop.shopName}" class="logo" draggable="false" src="${imgSrc}">
                        <div class="text-container">
                            <div class="name-container">
                                <span class="shopName">
                                    ${shop.shopName}
                                </span>
                            </div>
                            <span class="time">${shop.shopTime}</span>
                        </div>
                        <div class="address-container">
                            <span class="address">${shop.shopAddress}</span>
                        </div>
                    </a>`;

                // 생성한 HTML을 ul 태그 안에 추가
                $shopList.insertAdjacentHTML('beforeend', html);
            });

        } catch (e) {
            console.error("데이터 파싱 실패:", e);
        }
    };

    xhr.send();
}