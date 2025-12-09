const $shop = document.getElementById('popularShop');
let cachedPairs = [];
let cachedPairMapByCurrency = {}; //API 데이터를 가공하여 '통화(currency)'를 기준으로 정리한 객체
let cachedPairMapByCode = {}; // API 데이터를 가공하여 '마켓 코드(code)'를 기준으로 정리한 객체
let cachedQuotes = [];

//데이터 로딩함수

const loadPairs = (args) => {
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }

    if (xhr.status < 200 || xhr.status >= 300) {
        if (typeof args?.onerror === 'function') {
            args.onerror();
        }
        return;
    }
    const response = JSON.parse(xhr.responseText);
    cachedPairs = response;
    for (const product of response) {
        const code = product['market'];
        const currency = code.split('-')[0];
        cachedPairMapByCurrency[currency] ??= [];
        cachedPairMapByCurrency[currency].push(product);
        cachedPairMapByCode[code] = product;
    }
    if(typeof args?.onsuccess === 'function') {
        args.onsuccess();
    }
   };
    xhr.open('GET', 'https://api.upbit.com/v1/market/all?is_details=true');
    xhr.send();
}
const loadQuotes = (args) => {
    // 서버와 통신하기 위한 XMLHttpRequest 객체를 새로 생성
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        //readystate 가 4일경우에만 함수종료
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        // status 가 200~299 가 아니면 에러메세지 띄우기
        if (xhr.status < 200 || xhr.status >= 300) {
            if (typeof args?.onerror === 'function') {
                args.onerror();
            }
            return;
        }
        // 서버로부터 받은 응답 텍스트(JSON 형태의 문자열)를 자바스크립트 배열객체로 변환(파싱)
        const response = JSON.parse(xhr.responseText);
        // 파싱된 시세 데이터를 `cachedQuotes` 전역 변수에 저장하여 나중에 재사용할 수 있게 합니다.
        cachedQuotes = response;
        // args?.onsuccess 는 'args가 존재하고, 그 안에 onsuccess 속성이 있다면' 이라는 의미의 옵셔널체이닝코드
        if (typeof args?.onsuccess === 'function') {
            args?.onsuccess();
        }
    };

    //서버에 get 방식으로 quote의 통화를 krw,usdt btc 가져옴
    xhr.open('GET', 'https://api.upbit.com/v1/ticker/all?quote_currencies=KRW,BTC,USDT');
    xhr.send();
}
const applyQuote = (quote) => {
    const $tradePrices = document.body.querySelectorAll(`[data-code="${quote['market']}"][data-quote="trade_price"]`);
    for (const $tradePrice of $tradePrices) {
        $tradePrice.innerText = quote['trade_price'].toLocaleString();
    }
}
const drawPairs = (args) => {
    args ??= {};
    args['currency'] ??= 'KRW';
    const $list = $shop.querySelector(':scope > .list');
    $list.querySelectorAll(':scope > .item').forEach($item => $item.remove());

    cachedPairMapByCurrency[args['currency']].slice(0,6).forEach((pair) => {
        const symbol = pair['market'].split('-')[1];

        // 템플릿 리터럴의 HTML 구조를 수정합니다.
        const liTemplate = `
            <li class="item" data-code="${pair['market']}" >
                <img alt="" class="logo" draggable="false" src="https://static.upbit.com/logos/${symbol}.png">
                <div class="text-container">
                    <div class="name-container">
                        <span class="ko">${pair['korean_name']}</span>
                        <span class="en">${pair['english_name']}</span>
                    </div>
                    <span class="code">${pair['market']}</span>  </div>
                <div class="number-container">
                    <span class="price" data-code="${pair['market']}" data-quote="trade_price"></span>
                </div> </li>
        `;

        const $li = new DOMParser().parseFromString(liTemplate, 'text/html').querySelector('li');
        $list.append($li);
    });
};
loadPairs({
    onsuccess: () => {
        drawPairs();
        loadQuotes({
            onsuccess: () => {
                for (const quote of cachedQuotes) {
                    applyQuote(quote);
                }
            },

        });
    },

});