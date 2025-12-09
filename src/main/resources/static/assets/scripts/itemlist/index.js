// 1. "node:url" import 제거

// 4. cachedQuotes 선언 추가
let cachedQuotes = [];
// 3. 변수명 오타 수정 (cashed -> cached)
let cachedPairs = [];
let cachedPairMapByCurrency = {}; // API 데이터를 가공하여 '통화(currency)'를 기준으로 정리한 객체
let cachedMapByCode = {};

// HTML에서 <div id="item">을 찾음
// (주의: 이 스크립트가 <head>에 defer로 로드되므로 $item은 나중에 DOM이 로드된 후 접근해야 합니다.)
// -> 하지만 $item은 drawPairs에서만 사용되므로, loadPairs가 끝난 시점에는 DOM이 로드되어 문제가 없습니다.
const $item = document.getElementById("item");

// 데이터 로드 함수
const loadPairs = (args) => { // args 객체를 받도록 수정 (원래 코드엔 없었음)
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

        // 3. 변수명 오타 수정 (cashedPairs -> cachedPairs)
        cachedPairs = response;

        for (const product of response) {
            const code = product['market']
            const currency = code.split('-')[0];
            cachedPairMapByCurrency[currency] ??= [];
            cachedPairMapByCurrency[currency].push(product);
            cachedMapByCode[code] = product;
        }

        // 2. 콜백 로직 오류 수정 (onerror -> onsuccess)
        if (typeof args?.onsuccess === 'function') {
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
        // readystate 가 4일경우에만 함수종료
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

//요소의 가격을 적용하는함수
const applyQuote = (quote) => {
    const $tradePrices = document.body.querySelectorAll(`[data-code="${quote['market']}"][data-quote="trade_price"]`);
    for (const $tradePrice of $tradePrices) {
        $tradePrice.innerText = quote['trade_price'].toLocaleString();
    }
    const $changePrices = document.body.querySelectorAll(`[data-code="${quote['market']}"][data-quote="change_price"]`);
    for (const $changePrice of $changePrices) {
        $changePrice.innerText = quote['change_price'].toLocaleString();
    }
    const $changeRates = document.body.querySelectorAll(`[data-code="${quote['market']}"][data-quote="change_rate"]`);
    for (const $changeRate of $changeRates) {
        $changeRate.innerText = (quote['change_rate'] * 100).toFixed(2);
    }
}


const drawPairs = (args) => {
    // args가 null이나 undefined일 경우를 대비해 빈 객체 {}를 할당합니다.
    args ??= {};
    // args 객체에 'currency' 속성이 없으면 기본값으로 'KRW'를 할당합니다.
    args['currency'] ??= 'KRW';
    // <nav> 요소 안에서 '.list' 클래스를 가진 자식 요소를 찾습니다.
    const $itemContainer = $item.querySelector(':scope > .item-container');

    // 5. (선택 사항) 정적 HTML 아이템을 지우고 API 데이터로 덮어쓰기
    $itemContainer.innerHTML = '';
    // $itemContainer.querySelectorAll(':scope > .list').forEach(($item) => $item.remove()); // <- 기존 코드

    // 미리 그룹화해 둔 `cachedPairMapByCurrency` 객체에서
    // 원하는 기준 통화(기본값 'KRW')에 해당하는 코인 목록 배열을 가져와 반복문을 실행합니다.
    // (데이터 로드가 실패했다면 cachedPairMapByCurrency[args['currency']]이 undefined일 수 있으므로 옵셔널 체이닝 '?' 추가)
    cachedPairMapByCurrency[args['currency']]?.slice(0,6).forEach((pair) => {
        //  'BTC-BERA' 같은 마켓 코드에서 거래 대상이 되는 코인 심볼(BERA)만 추출합니다.
        const symbol = pair['market'].split('-')[1];
        // DOMParser를 사용하여 HTML 문자열로부터 <li> 요소를 생성합니다.
        const $li = new DOMParser().parseFromString(`
             <li class="list" data-code="${pair['market']}" data-quote-loader >
            <a href="../ITEM/item.html">
                <img class="image" src="https://static.upbit.com/logos/${symbol}.png" alt="상품이미지">
                <div class="item-info">
                    <div class="firstLine">
                        <span class="name">${pair['korean_name']}</span>
                        <span class="km">${pair['english_name']}</span>
                    </div>
                    <div class="address">${pair['market']}</div>
                    <div class="time" data-code="${pair['market']}" data-quote="trade_price" ></div>
                    <div class="tag-area">
                        <span class="tag" >예약가능</span>
                        <span class="tag" data-code="${pair['market']}" data-quote="change_price"></span>
                    </div>
                </div>
            </a>
        </li>`, 'text/html').querySelector('li');

        $itemContainer.append($li);
    });
}

// 가장 먼저 loadPairs 함수를 호출해서 업비트의 모든 마켓(코인) 정보를 가져옵니다.
loadPairs({
    // onsuccess는 loadPairs 함수가 성공적으로 모든 마켓 정보를 가져왔을 때 실행될 작업(콜백 함수)을 의미합니다.
    onsuccess: () => {
        // 1. 마켓 정보를 성공적으로 가져왔으니, 그 정보를 바탕으로 화면에 코인 목록을 그립니다.
        drawPairs();
        // 2. 화면에 목록을 그린 후, 이제 각 코인의 실시간 시세(가격, 변동률 등) 정보를 가져오기 위해 loadQuotes 함수를 호출합니다.
        loadQuotes({
            onsuccess: () => {
                // 3. 가져온 시세 정보를 '24시간 누적 거래대금'이 높은 순서대로 정렬(내림차순)합니다.
                cachedQuotes = cachedQuotes.sort((a, b) => b['acc_trade_price_24h'] - a['acc_trade_price_24h']);
                // 4. 정렬된 순서대로 각 시세 정보에 순서 번호('_order')를 부여하고, 화면에 최종 적용합니다.
                for (let i = 0; i < cachedQuotes.length; i++) {
                    // 5. '_order' 속성을 추가하여 CSS의 order 속성으로 목록 순서를 제어할 수 있게 합니다. (0, 1, 2, ...)
                    cachedQuotes[i]['_order'] = i;
                    // 6.  applyQuote 함수를 호출하여 현재 시세 정보를 화면의 해당 요소에 업데이트합니다.
                    applyQuote(cachedQuotes[i]);
                }
            },
        });
    },
});