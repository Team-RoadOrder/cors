document.addEventListener('DOMContentLoaded', function() {
        Loading.show('지도로딩중');

    // 1. 필수 요소 확인 (에러 방지)
    let mapContainer = document.getElementById('map-background');
    if (!mapContainer || typeof kakao === 'undefined') {
        Loading.hide();
        return;
    }

    let mapOption = {
        center: new kakao.maps.LatLng(37.541814, 127.004605),
        level: 3
    };

    // 2. 지도 생성
    let map = new kakao.maps.Map(mapContainer, mapOption);
    let geocoder = new kakao.maps.services.Geocoder();
    const userAddressInput = document.getElementById('user-address');

    // 3. 비동기 작업을 모을 배열 생성
    const promises = [];

    // [작업 1] 내 위치 찾기 (입력값이 있을 때만)
    if (userAddressInput && userAddressInput.value) {
        const userPromise = new Promise((resolve, reject) => {
            geocoder.addressSearch(userAddressInput.value, function(result, status) {
                if (status === kakao.maps.services.Status.OK) {
                    const userCoords = new kakao.maps.LatLng(result[0].y, result[0].x);
                    map.setCenter(userCoords);

                    let content = `
                     <div class="user-marker-wrap">
                        <div class="user-marker-dot"></div>
                        <div class="user-marker-pulse"></div>
                    </div>`;

                    new kakao.maps.CustomOverlay({
                        map: map,
                        position: userCoords,
                        content: content,
                        yAnchor: 1.5,
                        zIndex: 100
                    });
                }
                resolve(); // 성공하든 실패하든 작업 완료 처리
            });
        });
        promises.push(userPromise);
    }

    // 4. 서버 데이터 가져오기
    const shopsData = (typeof serverShopList !== 'undefined') ? serverShopList : [];

    // [작업 2] 매장 리스트 마커 찍기
    shopsData.forEach(function(shop) {
        if (shop.shopAddress) {
            const shopPromise = new Promise((resolve, reject) => {
                geocoder.addressSearch(shop.shopAddress, function(result, status) {
                    if (status === kakao.maps.services.Status.OK) {
                        let coords = new kakao.maps.LatLng(result[0].y, result[0].x);
                        let content = `<div class="custom-marker-style">
                                <span class="shop-name">${shop.shopName}</span>
                                <span class="shop-address">${shop.shopAddress}</span> 
                                  </div>`;

                        new kakao.maps.CustomOverlay({
                            map: map,
                            position: coords,
                            content: content,
                            yAnchor: 1.5
                        });
                    }
                    resolve(); // 성공하든 실패하든 다음으로 진행
                });
            });
            promises.push(shopPromise);
        }
    });

    // 5. [핵심] 모든 작업(Promise)이 끝난 후 로딩 숨기기
    Promise.all(promises).then(() => {
        Loading.hide();
    }).catch(err => {
        Loading.hide(); // 에러가 나도 로딩은 꺼야 함
    });
});

// 리스트 클릭 이벤트 (기존 유지)
document.addEventListener('DOMContentLoaded', function() {
    const shopList = document.querySelector('.shop-list');
    if (shopList) {
        shopList.addEventListener('click', function(e) {
            const clickedItem = e.target.closest('.shop-detail');
            if (clickedItem) {
                clickedItem.classList.toggle('active');
            }
        });
    }
});