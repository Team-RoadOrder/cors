document.addEventListener('DOMContentLoaded', () => {
    const items = document.querySelectorAll('#inner > .item');

    items.forEach(item => {
        const grid = item.querySelector('.grid');
        const prevBtn = item.querySelector('.slider-nav.prev');
        const nextBtn = item.querySelector('.slider-nav.next');
        const boxes = grid.querySelectorAll('.box');

        // 요소가 없으면 실행하지 않음
        if (!grid || !prevBtn || !nextBtn || boxes.length === 0) return;

        const firstBox = boxes[0];
        const lastBox = boxes[boxes.length - 1];

        // 1. 관찰자 설정 (첫 번째와 마지막 요소를 감시)
        const observerOptions = {
            root: grid,       // 슬라이더 영역을 기준으로 감시
            threshold: 0.5    // 대상이 50% 이상 보이면 감지 (중앙 정렬 고려)
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


        // 2. 버튼 클릭 기능 (기존과 동일)
        const getScrollAmount = () => {
            const style = window.getComputedStyle(grid);
            const gap = parseFloat(style.gap) || 0;
            return boxes[0].offsetWidth + gap;
        };

        prevBtn.addEventListener('click', () => {
            grid.scrollBy({ left: -getScrollAmount(), behavior: 'smooth' });
        });

        nextBtn.addEventListener('click', () => {
            grid.scrollBy({ left: getScrollAmount(), behavior: 'smooth' });
        });
    });
});