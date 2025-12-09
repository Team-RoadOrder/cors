document.addEventListener('DOMContentLoaded', () => {

    // 각 영역의 박스와 메시지를 미리 찾아둡니다.
    const typeBox = document.getElementById('type');
    const detailBox = document.getElementById('detail');
    const subDetailBox = document.getElementById('sub-detail');

    const detailMsg = detailBox.querySelector('.message');
    const subDetailMsg = subDetailBox.querySelector('.message');

    // 헬퍼 함수: 리스트의 모든 'active' 클래스 제거
    const removeActiveFromList = (list) => {
        list.querySelectorAll('.item').forEach(item => {
            item.classList.remove('active');
        });
    };

    // 헬퍼 함수: 특정 박스 안의 모든 리스트 숨기기
    const hideAllListsInBox = (box) => {
        box.querySelectorAll('.list').forEach(list => {
            list.classList.remove('show');
        });
    };

    // --- 1. 대분류 (Type) 아이템 클릭 이벤트 ---
    typeBox.querySelectorAll('.item').forEach(item => {
        item.addEventListener('click', (e) => {
            const clickedItem = e.currentTarget;
            const targetName = clickedItem.dataset.target; // 'man', 'woman', 'shoe'

            // 1-1. 'active' 클래스 처리
            removeActiveFromList(typeBox);
            clickedItem.classList.add('active');

            // 1-2. 하위 목록(중분류, 소분류) 초기화
            hideAllListsInBox(detailBox);
            hideAllListsInBox(subDetailBox);

            // 1-3. 메시지 초기화
            // [수정 1] "대분류" 메시지는 *숨기고*
            detailMsg.classList.add('hidden');
            // "중분류" 메시지는 *보이게*
            subDetailMsg.classList.remove('hidden');

            // 1-4. 타겟 중분류 목록 보여주기
            // [수정 2] querySelector에 따옴표(백틱) 추가
            const targetDetailList = detailBox.querySelector(`.${targetName}-detail`);
            if (targetDetailList) {
                targetDetailList.classList.add('show');
            }

            // 1-5. ★★★ '신발(shoe)' 예외 처리 ★★★
            if (targetName === 'shoe') {
                // [수정 3] querySelector에 따옴표(백틱) 추가
                const shoeSubList = subDetailBox.querySelector(`.sub-detail-shoe`);
                if (shoeSubList) {
                    shoeSubList.classList.add('show');
                    // 신발은 소분류도 바로 보이므로 "중분류 선택" 메시지 숨김
                    subDetailMsg.classList.add('hidden');
                }
            }
        });
    });

    // --- 2. 중분류 (Detail) 아이템 클릭 이벤트 ---
    detailBox.querySelectorAll('.item').forEach(item => {
        item.addEventListener('click', (e) => {
            const clickedItem = e.currentTarget;
            const parentList = clickedItem.closest('.list');

            // 'active' 클래스 처리
            removeActiveFromList(parentList);
            clickedItem.classList.add('active');

            // 2-1. '신발' 중분류를 클릭한 경우 (스니커즈, 구두류)
            if (parentList.classList.contains('shoe-detail')) {
                return;
            }

            // 2-2. '남성' 또는 '여성' 의류를 클릭한 경우
            const targetName = clickedItem.dataset.target; // 'outer', 'top', 'pants'
            const type = parentList.classList.contains('man-detail') ? 'man' : 'woman'; // 'man' or 'woman'

            // 2-3. 소분류 목록 초기화
            hideAllListsInBox(subDetailBox);
            subDetailMsg.classList.add('hidden'); // "중분류 선택" 메시지 숨기기

            // 2-4. 타겟 소분류 목록 보여주기
            // [수정 4] querySelector에 따옴표(백틱) 추가
            const targetSubList = subDetailBox.querySelector(`.${type}-sub-detail-${targetName}`);
            if (targetSubList) {
                targetSubList.classList.add('show');
            }
        });
    });

    // --- 3. 소분류 (Sub-Detail) 아이템 클릭 이벤트 ---
    subDetailBox.querySelectorAll('.item').forEach(item => {
        item.addEventListener('click', (e) => {
            const clickedItem = e.currentTarget;
            const parentList = clickedItem.closest('.list');

            // 'active' 클래스 처리 (마지막 단계)
            removeActiveFromList(parentList);
            clickedItem.classList.add('active');
        });
    });

});