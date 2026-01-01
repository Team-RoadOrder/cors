document.addEventListener('DOMContentLoaded', () => {
    // 요소 참조
    const mainInput = document.getElementById('inputMainCategory');
    const subInput = document.getElementById('inputSubCategory');
    const detailInput = document.getElementById('inputDetailCategory');

    const typeBox = document.getElementById('type');
    const detailBox = document.getElementById('detail');
    const subDetailBox = document.getElementById('sub-detail');

    const detailMsg = detailBox.querySelector('.message');
    const subDetailMsg = subDetailBox.querySelector('.message');

    const removeActiveFromList = (list) => {
        list.querySelectorAll('.item').forEach(item => item.classList.remove('active'));
    };

    const hideAllListsInBox = (box) => {
        box.querySelectorAll('.list').forEach(list => list.classList.remove('show'));
    };

    // --- 1. 대분류 (Type) 클릭 ---
    typeBox.querySelectorAll('.item').forEach(item => {
        item.addEventListener('click', (e) => {
            const clickedItem = e.currentTarget;
            const targetName = clickedItem.dataset.target;

            // 밸류 반영
            mainInput.value = clickedItem.innerText.trim();
            subInput.value = "";    // 상위 클릭 시 하위 초기화
            detailInput.value = "";

            // UI 처리
            removeActiveFromList(typeBox);
            clickedItem.classList.add('active');
            hideAllListsInBox(detailBox);
            hideAllListsInBox(subDetailBox);
            detailMsg.classList.add('hidden');
            subDetailMsg.classList.remove('hidden');

            const targetDetailList = detailBox.querySelector(`.${targetName}-detail`);
            if (targetDetailList) targetDetailList.classList.add('show');

            if (targetName === 'shoe') {
                const shoeSubList = subDetailBox.querySelector(`.sub-detail-shoe`);
                if (shoeSubList) {
                    shoeSubList.classList.add('show');
                    subDetailMsg.classList.add('hidden');
                }
            }
        });
    });

    // --- 2. 중분류 (Detail) 클릭 ---
    detailBox.querySelectorAll('.item').forEach(item => {
        item.addEventListener('click', (e) => {
            const clickedItem = e.currentTarget;
            const parentList = clickedItem.closest('.list');

            // 밸류 반영
            subInput.value = clickedItem.innerText.trim();
            detailInput.value = ""; // 중분류 클릭 시 소분류 초기화

            // UI 처리
            removeActiveFromList(parentList);
            clickedItem.classList.add('active');

            if (parentList.classList.contains('shoe-detail')) return;

            const targetName = clickedItem.dataset.target;
            const type = parentList.classList.contains('man-detail') ? 'man' : 'woman';

            hideAllListsInBox(subDetailBox);
            subDetailMsg.classList.add('hidden');

            const targetSubList = subDetailBox.querySelector(`.${type}-sub-detail-${targetName}`);
            if (targetSubList) targetSubList.classList.add('show');
        });
    });

    // --- 3. 소분류 (Sub-Detail) 클릭 ---
    subDetailBox.querySelectorAll('.item').forEach(item => {
        item.addEventListener('click', (e) => {
            const clickedItem = e.currentTarget;
            const parentList = clickedItem.closest('.list');

            // 밸류 반영
            detailInput.value = clickedItem.innerText.trim();

            // UI 처리
            removeActiveFromList(parentList);
            clickedItem.classList.add('active');
        });
    });
});