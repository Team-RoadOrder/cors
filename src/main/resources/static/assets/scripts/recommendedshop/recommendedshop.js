document.addEventListener('DOMContentLoaded', () => {
    const items = document.querySelectorAll('#inner > .item');

    if (!items || items.length === 0) {
        // console.warn('추천 아이템 영역을 찾을 수 없습니다.');
        return;
    }

    items.forEach(item => {
        const grid = item.querySelector('.grid');
        const prevBtn = item.querySelector('.slider-nav.prev');
        const nextBtn = item.querySelector('.slider-nav.next');
        const boxes = grid ? grid.querySelectorAll('.box') : [];

        // 필수 요소가 없으면 실행하지 않음 (유효성 검사)
        if (!grid || !prevBtn || !nextBtn) {
            // console.warn('슬라이더 구성 요소가 누락되었습니다.', item);
            return;
        }

        if (boxes.length === 0) {
            // 아이템이 없을 경우 버튼 숨김 처리
            prevBtn.style.display = 'none';
            nextBtn.style.display = 'none';
            
            // 안내 메시지 표시 (선택 사항)
            // const emptyMsg = document.createElement('div');
            // emptyMsg.className = 'empty-message';
            // emptyMsg.textContent = '추천 항목이 없습니다.';
            // grid.appendChild(emptyMsg);
            return;
        }

        const firstBox = boxes[0];
        const lastBox = boxes[boxes.length - 1];

        // 1. 관찰자 설정 (첫 번째와 마지막 요소를 감시)
        const observerOptions = {
            root: grid,       // 슬라이더 영역을 기준으로 감시
            threshold: 0.1    // 대상이 10% 이상 보이면 감지 (민감도 조정)
        };

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                // 첫 번째 박스가 화면에 들어왔는지 확인
                if (entry.target === firstBox) {
                    // 보이면(isIntersecting) 버튼 숨김, 안 보이면 버튼 표시(flex)
                    prevBtn.style.display = entry.isIntersecting ? 'none' : 'flex';
                }

                // 마지막 박스가 화면에 들어왔는지 확인
                if (entry.target === lastBox) {
                    nextBtn.style.display = entry.isIntersecting ? 'none' : 'flex';
                }
            });
        }, observerOptions);

        // 첫 번째 요소와 마지막 요소를 관찰 시작
        observer.observe(firstBox);
        observer.observe(lastBox);


        // 2. 버튼 클릭 기능
        const getScrollAmount = () => {
            const style = window.getComputedStyle(grid);
            const gap = parseFloat(style.gap) || 0;
            // 박스가 하나라도 있을 때만 너비 계산
            return boxes.length > 0 ? boxes[0].offsetWidth + gap : 0;
        };

        prevBtn.addEventListener('click', () => {
            grid.scrollBy({ left: -getScrollAmount(), behavior: 'smooth' });
        });

        nextBtn.addEventListener('click', () => {
            grid.scrollBy({ left: getScrollAmount(), behavior: 'smooth' });
        });
    });
});
