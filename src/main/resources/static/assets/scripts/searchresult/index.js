document.addEventListener("DOMContentLoaded", function() {
    const searchForm = document.getElementById("searchForm");
    const keywordInput = document.getElementById("keywordInput");

    if (searchForm) {
        searchForm.addEventListener("submit", function(e) {
            const keyword = keywordInput.value.trim();
            
            if (keyword.length < 1) {
                e.preventDefault();
                // alert("검색어를 입력해주세요."); // 필요 시 알림
                keywordInput.focus();
                return;
            }
            
            // 추가적인 유효성 검사가 필요하다면 여기에 작성
        });
    }
});
