/**@type {HTMLDivElement}*/
const $shop = document.getElementById("shop");
const $storeList = $shop.querySelector(':scope > .store-list');

const renderShopList = () => {
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, {confirmText: '확인'});
            return;
        }
        const shops = JSON.parse(xhr.responseText);
        if (shops.length > 0) {
            $storeList.innerHTML = '';
            shops.forEach(shop => {
                const imgSrc = shop.profileImage
                    ? `${shop.profileImage}`
                    : 'https://static.upbit.com/logos/USDT.png'; // 기본 이미지
            const html = `
            <li class="store" >
            <a href="/shop/?shopId=${shop.shopId}">
            <img class="image" src="${imgSrc}" alt="매장이미지">
            <div class="store-info">
                <div class="firstLine">
                    <span class="name">${shop.shopName}</span>
                    <span class="km">0.67km</span>
                </div>
                <div class="address">${shop.shopAddress}</div>
                <div class="time">영업중: ${shop.shopTime}</div>
                <div class="tag-area">
                    <span class="tag">예약 가능</span>
                    <span class="tag">픽업 가능</span>
                </div>
            </div>
            </a>
        </li>`;
        $storeList.insertAdjacentHTML('beforeend',html)
         });
        }
     };
     xhr.open('GET', '/all-shops');
     xhr.send();
}

renderShopList();

