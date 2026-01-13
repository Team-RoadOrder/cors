document.addEventListener('DOMContentLoaded', function() {

    // 1. 지도 컨테이너 및 옵션 설정
    let mapContainer = document.getElementById('map-background');


    let mapOption = {
        center: new kakao.maps.LatLng(37.541814, 127.004605), // 기본 위치 (용산/서울 근처)
        level: 4
    };

    // 2. 지도 생성
    let map = new kakao.maps.Map(mapContainer, mapOption);

    // 3. 주소-좌표 변환 객체 생성
    let geocoder = new kakao.maps.services.Geocoder();

    // 4. 서버 데이터 가져오기
    const shopsData = (typeof serverShopList !== 'undefined') ? serverShopList : [];

    // [핵심] 주소 리스트를 돌면서 좌표로 변환 후 마커 찍기
    shopsData.forEach(function(shop) {

        // 주소값이 있는 경우에만 실행
        if (shop.shopAddress) {

            // 주소로 좌표를 검색합니다
            geocoder.addressSearch(shop.shopAddress, function(result, status) {

                // 정상적으로 검색이 완료됐으면
                if (status === kakao.maps.services.Status.OK) {

                    // result[0].y 가 위도(latitude), result[0].x 가 경도(longitude)
                    let coords = new kakao.maps.LatLng(result[0].y, result[0].x);

                    // 커스텀 오버레이 내용
                    let content = '<div class="custom-marker-style" style="padding:5px; background:white; border:1px solid #ccc; border-radius:5px;">' + shop.shopName + '</div>';

                    // 커스텀 오버레이 생성
                    let customOverlay = new kakao.maps.CustomOverlay({
                        map: map,
                        position: coords,
                        content: content,
                        yAnchor: 1.5 // 마커 위치 조정
                    });

                    // (선택사항) 첫 번째 가게가 검색되면 거기로 중심 이동
                    // shopsData[0]과 현재 shop이 같은지 비교하거나 flag 사용 가능
                    if (shop === shopsData[0]) {
                        map.setCenter(coords);
                    }
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