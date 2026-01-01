// 1. 더미 데이터 생성 (나중에 API에서 받아올 데이터 형태와 유사하게 구성)
// categoryId: 1=전체(보통 전체는 필터링 안함), 2=아우터, 3=상의, 4=하의 ...
const productData = [
    { id: 1, categoryId: '2', name: 'Waviness Tide Layer Wool Crop Jacket Black', price: 412000, image: '../../assets/images/shop/category/one-1.webp' },
    { id: 2, categoryId: '2', name: 'Classic Wool Coat Camel', price: 320000, image: '/assets/images/shop/category/one-2.webp' },
    { id: 3, categoryId: '3', name: 'Signature Logo Hoodie Grey', price: 89000, image: '../assets/images/shop/category/one-3.webp' },
    { id: 4, categoryId: '4', name: 'Wide Fit Denim Pants Blue', price: 105000, image: '../assets/images/shop/category/one-4.webp' },
    { id: 5, categoryId: '5', name: 'Leather Cross Bag Black', price: 150000, image: '../assets/images/shop/category/one-5.webp' },
    { id: 6, categoryId: '2', name: 'Check Pattern Blazer', price: 210000, image: '../assets/images/shop/category/one-1.webp' }, // 테스트용 추가 데이터
    { id: 7, categoryId: '6', name: 'Silver Ring Set', price: 45000, image: '../assets/images/shop/category/one-3.webp' },
];

// 2. 초기화 (페이지 로드 시 '전체(1)' 카테고리 보여주기)
document.addEventListener('DOMContentLoaded', () => {
    changeCategory('1');
});

// 3. 카테고리 변경 함수
function changeCategory(categoryValue) {
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

        // HTML 문자열 생성
        const html = `
            <a class="box" href="#">
                <span class="image-wrapper">
                    <div class="image" style="background-image: url('${item.image}');"></div>
                </span>
                <span class="text-container">
                    <span class="title">${item.name}</span>
                    <span class="price">${formattedPrice}</span>
                </span>
            </a>
        `;

        // 그리드에 추가
        gridContainer.insertAdjacentHTML('beforeend', html);
    });
}