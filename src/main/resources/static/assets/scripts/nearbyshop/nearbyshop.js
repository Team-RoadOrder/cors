document.addEventListener('DOMContentLoaded', function() {

    // 1. 지도 컨테이너 및 옵션 설정
    let mapContainer = document.getElementById('map-background');

    let mapOption = {
        center: new kakao.maps.LatLng(37.541814, 127.004605), // 기본 위치 (용산/서울 근처)
        level: 3
    };

    // 2. 지도 생성
    let map = new kakao.maps.Map(mapContainer, mapOption);

    // 3. 주소-좌표 변환 객체 생성
    let geocoder = new kakao.maps.services.Geocoder();
    const userAddressInput = document.getElementById('user-address');

    // ... (이전 코드)
    if (userAddressInput && userAddressInput.value) {
        geocoder.addressSearch(userAddressInput.value, function(result, status) {
            if (status === kakao.maps.services.Status.OK) {
                const userCoords = new kakao.maps.LatLng(result[0].y, result[0].x);

                // 1) 지도 중심 이동
                map.setCenter(userCoords);

                // 2) 내 위치 마커 표시
                // 눈에 잘 띄도록 스타일(배경색, 테두리 등)을 인라인으로 확실하게 지정합니다.
                let content = `
                 <div class="user-marker-wrap">
                <div class="user-marker-dot"></div>
                <div class="user-marker-pulse"></div>
            </div>
                `;

                new kakao.maps.CustomOverlay({
                    map: map,
                    position: userCoords,
                    content: content,
                    yAnchor: 1.5,
                    zIndex: 100  // [핵심] API 옵션으로 zIndex를 높게 설정해야 합니다.
                });
            } else {
                console.log('사용자 주소를 찾을 수 없습니다.'); // 디버깅용 로그
            }
        });
    }

    // 4. 서버 데이터 가져오기
    const shopsData = (typeof serverShopList !== 'undefined') ? serverShopList : [];

    // [핵심] 주소 리스트를 돌면서 좌표로 변환 후 마커(커스텀 오버레이) 찍기
    shopsData.forEach(function(shop) {
        if (shop.shopAddress) {
            geocoder.addressSearch(shop.shopAddress, function(result, status) {
                if (status === kakao.maps.services.Status.OK) {
                    let coords = new kakao.maps.LatLng(result[0].y, result[0].x);

                    // 매장용 커스텀 오버레이 내용
                    let content = `<div class="custom-marker-style">
                            <span class="shop-name">${shop.shopName}</span>
                            <span class="shop-address">${shop.shopAddress}</span> 
                              </div>`;

                    let customOverlay = new kakao.maps.CustomOverlay({
                        map: map,
                        position: coords,
                        content: content,
                        yAnchor: 1.5
                    });
                }
            });
        }
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