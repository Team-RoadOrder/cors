/**
 * 매출 현황 차트 관리 모듈 (SalesChartModule)
 * 역할: 데이터 Fetch 즉시 로딩 제거, 비동기 렌더링을 통한 시각적 잔상 방지, UI 최적화
 */
const SalesChartModule = (function () {
    let salesChart = null;

    // 데이터를 미리 담아둘 캐시 (탭 전환 시 즉시 사용)
    const dataCache = {
        daily: null,
        monthly: null
    };

    /**
     * 주간 날짜 배열 생성
     */
    const getWeekDates = () => {
        const now = new Date();
        const day = now.getDay();
        const diff = now.getDate() - day + (day === 0 ? -6 : 1);
        const monday = new Date(now.setDate(diff));
        const week = [];
        for (let i = 0; i < 7; i++) {
            const tempDate = new Date(monday);
            tempDate.setDate(monday.getDate() + i);
            week.push(tempDate.toISOString().split('T')[0]);
        }
        return week;
    };

    /**
     * 월간 레이블 생성
     */
    const getMonthLabels = () => {
        const now = new Date();
        const months = [];
        for (let i = 2; i >= 0; i--) {
            const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
            const year = d.getFullYear();
            const month = d.getMonth() + 1;
            months.push({
                full: `${year}-${month < 10 ? '0' + month : month}`,
                display: `${month}월`
            });
        }
        return months;
    };

    /**
     * 차트 렌더링 함수
     * 수정사항: y축 매출(원) 레이블 display를 false로 변경
     */
    const renderChart = (data, type) => {
        const canvas = document.getElementById('salesChart');
        if (!canvas || !data) return;

        const ctx = canvas.getContext('2d');
        let chartLabels = [];
        let salesData = [];
        let orderCountData = [];

        if (type === 'daily') {
            const weekDates = getWeekDates();
            chartLabels = weekDates.map(date => date.substring(5).replace('-', '/'));
            salesData = weekDates.map(date => {
                return data.filter(item => item.date === date)
                    .reduce((sum, item) => sum + (item.sales || 0), 0);
            });
            orderCountData = weekDates.map(date => {
                return data.filter(item => item.date === date)
                    .reduce((sum, item) => sum + (item.orderCount || 0), 0);
            });
        } else {
            const monthInfo = getMonthLabels();
            chartLabels = monthInfo.map(m => m.display);
            salesData = monthInfo.map(m => {
                return data.filter(item => item.date.startsWith(m.full))
                    .reduce((sum, item) => sum + (item.sales || 0), 0);
            });
            orderCountData = monthInfo.map(m => {
                return data.filter(item => item.date.startsWith(m.full))
                    .reduce((sum, item) => sum + (item.orderCount || 0), 0);
            });
        }

        if (salesChart) {
            salesChart.destroy();
        }

        salesChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: chartLabels,
                datasets: [
                    {
                        label: '매출 (원)',
                        data: salesData,
                        borderColor: '#3b82f6',
                        backgroundColor: 'rgba(59, 130, 246, 0.12)',
                        borderWidth: 2.5,
                        tension: 0.45,
                        pointRadius: 5,
                        pointHoverRadius: 7,
                        pointBackgroundColor: '#fff',
                        pointBorderColor: '#3b82f6',
                        pointBorderWidth: 2,
                        fill: true,
                        yAxisID: 'y'
                    },
                    {
                        label: '주문수 (건)',
                        data: orderCountData,
                        borderColor: '#ef4444',
                        borderWidth: 2.5,
                        tension: 0.45,
                        pointRadius: 5,
                        pointHoverRadius: 7,
                        pointBackgroundColor: '#fff',
                        pointBorderColor: '#ef4444',
                        pointBorderWidth: 2,
                        fill: false,
                        yAxisID: 'y1'
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                scales: {
                    x: { grid: { display: false }, ticks: { font: { size: 12 } } },
                    y: {
                        position: 'left',
                        beginAtZero: true,
                        grid: { color: '#e5e7eb' },
                        ticks: { callback: value => value.toLocaleString() },
                        // [핵심 수정] 좌측 레이블 '매출 (원)'을 보이지 않게 처리합니다.
                        title: { display: false, text: '매출 (원)' }
                    },
                    y1: { position: 'right', beginAtZero: true, grid: { drawOnChartArea: false }, ticks: { display: false } }
                },
                plugins: {
                    legend: { position: 'bottom', labels: { usePointStyle: true, pointStyle: 'line', padding: 20 } },
                    tooltip: {
                        backgroundColor: '#111827',
                        padding: 12,
                        callbacks: {
                            label: ctx => {
                                const value = ctx.raw;
                                return ctx.dataset.label.includes('매출')
                                    ? ` ${ctx.dataset.label}: ${value.toLocaleString()}원`
                                    : ` ${ctx.dataset.label}: ${value}건`;
                            }
                        }
                    }
                }
            }
        });
    };

    /**
     * 데이터 로드 및 UI 제어
     */
    const loadGraph = async (type) => {
        const weekBtn = document.querySelector('.weekButton');
        const monthBtn = document.querySelector('.monthButton');

        if (type === 'daily') {
            weekBtn?.classList.add('-selected');
            monthBtn?.classList.remove('-selected');
        } else {
            weekBtn?.classList.remove('-selected');
            monthBtn?.classList.add('-selected');
        }

        if (dataCache[type]) {
            renderChart(dataCache[type], type);
            return;
        }

        if (typeof Loading !== 'undefined') {
            Loading.show("매출 데이터를 분석 중입니다...");
        }

        try {
            const response = await fetch(`/owner/graph/data/${type}`);
            if (!response.ok) throw new Error('서버 통신 실패');
            const data = await response.json();

            dataCache[type] = data;

            if (typeof Loading !== 'undefined') {
                Loading.hide();
            }

            requestAnimationFrame(() => {
                renderChart(data, type);
            });

        } catch (error) {
            console.error('오류:', error);
            if (typeof Loading !== 'undefined') Loading.hide();
            setTimeout(() => {
                if (typeof openModal === 'function') {
                    openModal("오류", "<p>데이터를 불러오지 못했습니다.</p>", { confirmText: '확인' });
                }
            }, 100);
        }
    };

    /**
     * 초기화 (Pre-fetch 적용)
     */
    const init = async () => {
        const canvas = document.getElementById('salesChart');
        if (!canvas) return;
        //
        // if (typeof Loading !== 'undefined') {
        //     Loading.show("데이터 초기화 중...");
        // }

        try {
            const [dailyRes, monthlyRes] = await Promise.all([
                fetch('/owner/graph/data/daily'),
                fetch('/owner/graph/data/monthly')
            ]);

            dataCache.daily = await dailyRes.json();
            dataCache.monthly = await monthlyRes.json();

            if (typeof Loading !== 'undefined') {
                Loading.hide();
            }

            requestAnimationFrame(() => {
                renderChart(dataCache.daily, 'daily');
            });

        } catch (error) {
            console.error('초기 로드 실패:', error);
            if (typeof Loading !== 'undefined') Loading.hide();
        }
    };

    return { init, loadGraph };
})();

document.addEventListener('DOMContentLoaded', SalesChartModule.init);