document.addEventListener('scroll', function() {
    const header = document.getElementById('header');
    if (window.scrollY > 0) {
        header.classList.add('scroll-active');
    } else {
        header.classList.remove('scroll-active');
    }
});

// 검색창에 있는 헤더를 위한
const searchIcon = document.querySelector('.glass');
const searchInput = document.getElementById('searchInput');

// 돋보기 아이콘에 'click' 이벤트 리스너를 추가합니다.
searchIcon.addEventListener('click', (event) => {


    // searchInput 요소에 'show' 클래스를 추가하거나 제거합니다 (toggle).
    searchInput.classList.toggle('show');

    // 만약 검색창이 보이는 상태가 되었다면, 바로 입력할 수 있도록 focus를 줍니다.
    if (searchInput.classList.contains('show')) {
        searchInput.focus();
    }
});

const dialogHandler = {
    $dialog: document.getElementById('dialog'),
    $modals: [],

    hide ($modal) {
        let index = dialogHandler.$modals.indexOf($modal);
        if (index > -1) {
            dialogHandler.$modals.splice(index, 1);
        }
        $modal.classList.remove('visible');
        if (dialogHandler.$modals.length === 0) {
            dialogHandler.$dialog.classList.remove('visible');
        }
        $modal.classList.remove('visible');
        setTimeout(()=> $modal.classList.remove('visible'), 1000);
    },
    show (args) {
        const $modal = document.createElement('div');
        const $title = document.createElement('div');
        const $content = document.createElement('div');
        $title.classList.add('title')
        $title.innerText = args['title'];
        $content.classList.add('content');
        if (args['isContentHtml']=== true) {
            $content.innerHTML = args['content'];
        } else {
            $content.innerText = args['content'];
        }
       $modal.append($title,$content);
        $modal.classList.add('modal');
        if (args['buttons'] !=null && args['buttons'].length > 0) {
            // 버튼을 감쌀 컨테이너 생성
            const $buttonContainer = document.createElement('div');
            $buttonContainer.classList.add('button-container');
            // 배열의 모든 버튼에 대해 반복
            for ( const button of args['buttons'] ) {
                const $button = document.createElement('button');
                $button.classList.add('button');
                $button.setAttribute('type', 'button');// 기본 submit 동작 방지
                $button.innerText = button['caption'];// 버튼 텍스트 설정
                // 버튼 객체에 'onclick' 콜백 함수가 정의되어 있으면
                if (typeof button['onclick'] === 'function') {
                    // 해당 버튼에 클릭 이벤트 리스너 추가
                    $button.addEventListener('click', ()=>{
                        // 클릭 시, 'onclick' 함수를 실행하며
                        // 인자로 현재 생성된 '$modal' 요소를 넘겨줌
                        // (콜백 함수 내에서 hide($modal) 등을 호출할 수 있도록)
                        button['onclick']($modal);
                    });
                }
                // 버튼 컨테이너에 버튼 추가
                $buttonContainer.append($button);
            }
            // 모달에 버튼 컨테이너 추가
            $modal.append($buttonContainer);
        }
        dialogHandler.$dialog.append($modal);
        dialogHandler.$dialog.classList.add('visible');
        dialogHandler.$modals.push($modal);
        setTimeout(() => $modal.classList.add('visible'), 50);
        return $modal;
    },
    ShowSimpleOk: ( title, content, args = {} )=>{
        // 기본 'show' 함수를 재사용
        return dialogHandler.show({
            title: title,
            content: content,
            isContentHtml: args?.isContentHtml,
            buttons: [
                {
                    // 'okCaption'이 있으면 그 값을 쓰고, 없으면(nullish) '확인'을 씀
                    caption: args['okCaption'] ?? '확인',
                    onclick: ($modal) => {
                        // 클릭 시 무조건 모달을 닫음
                        dialogHandler.hide($modal);
                        if(typeof args['onclickok'] === 'function') {
                            // 모달이 닫힌 후 해당 콜백 함수를 실행
                            args['onclickok']($modal);
                        }
                    }
                }
            ]
        });
    }

}