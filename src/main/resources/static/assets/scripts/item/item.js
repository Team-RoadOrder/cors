document.addEventListener("DOMContentLoaded", () => {



    // --- [1. 이미지 슬라이더 및 광고 로직] ---
    const dots = document.querySelectorAll('.dot');
    const imageWrapper = document.querySelector('.image-wrapper');
    const images = document.querySelectorAll('.image-wrapper .image');
    let currentIndex = 0;
    let slideInterval;

    const scrollToImage = (index) => {
        if (!images[index] || !imageWrapper) return;
        imageWrapper.scrollTo({ left: images[index].offsetLeft, behavior: 'smooth' });
        dots.forEach((dot, i) => { dot.classList.toggle('active', i === index); });
        currentIndex = index;
    };

    dots.forEach((dot, index) => {
        dot.addEventListener('click', (e) => {
            e.preventDefault();
            stopAutoSlide();
            scrollToImage(index);
            startAutoSlide();
        });
    });

    const startAutoSlide = () => {
        slideInterval = setInterval(() => {
            let nextIndex = (currentIndex + 1) % images.length;
            scrollToImage(nextIndex);
        }, 2000);
    };

    const stopAutoSlide = () => { clearInterval(slideInterval); };
    if (imageWrapper) {
        imageWrapper.addEventListener('mouseenter', stopAutoSlide);
        imageWrapper.addEventListener('mouseleave', startAutoSlide);
    }
    startAutoSlide();

    // 탭 메뉴 전환 로직
    document.querySelectorAll('.productHead .item').forEach(tab => {
        tab.addEventListener('click', function (e) {
            e.preventDefault();
            document.querySelectorAll('.productHead .item').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            const targetId = this.getAttribute('data-tab');
            document.querySelectorAll('.content-pane').forEach(pane => { pane.classList.remove('active'); });
            const targetPane = document.getElementById(targetId);
            if (targetPane) targetPane.classList.add('active');
        });
    });

    //스타일리뷰:상단 스타일 그리드 클릭 이벤트
    const gridItems = document.querySelectorAll('.style-grid .grid-item');
    gridItems.forEach((item) => {
        item.style.cursor = 'pointer';
        item.addEventListener('click', () => {
            const img = item.querySelector('img');
            if (img) window.viewReviewImages(null, img);
        });
    });

    //리뷰 작성 및 미리보기 로직
    const writeToggle = document.getElementById('review-write-toggle');
    const writeForm = document.getElementById('style-write-form');
    const cancelBtn = document.getElementById('btn-review-cancel');
    const submitBtn = document.getElementById('btn-review-submit');
    const imageInput = document.getElementById('image-upload');
    const previewContainer = document.getElementById('image-preview-container');

    writeToggle?.addEventListener('change', function() {
        if (writeForm) {
            writeForm.style.display = this.checked ? 'block' : 'none';
            if (!this.checked) {
                writeForm.reset();
                delete writeForm.dataset.mode;
                delete writeForm.dataset.editId;
                if (previewContainer) previewContainer.innerHTML = '';
            }
        }
    });

    cancelBtn?.addEventListener('click', () => {
        openModal("작성 취소", `<p>작성 중인 내용이 사라집니다. 취소하시겠습니까?</p>`, {
            confirmText: '확인', cancelText: '계속 작성',
            onConfirm: () => {
                if (writeToggle) writeToggle.checked = false;
                if (writeForm) {
                    writeForm.style.display = 'none';
                    writeForm.reset();
                }
                if (previewContainer) previewContainer.innerHTML = '';
            }
        });
    });

    imageInput?.addEventListener('change', (e) => {
        const files = e.target.files;
        if (previewContainer) previewContainer.innerHTML = '';
        if (files.length > 3) {
            openModal("업로드 제한", `<p>사진은 최대 3장까지만 선택할 수 있습니다.</p>`, { confirmText: '확인' });
            e.target.value = "";
            return;
        }
        if (files.length > 0) {
            Array.from(files).forEach(file => {
                if (!file.type.startsWith('image/')) return;
                const reader = new FileReader();
                reader.onload = (event) => {
                    const img = document.createElement('img');
                    img.src = event.target.result;
                    img.style.width = "5rem"; img.style.height = "5rem"; img.style.objectFit = "cover";
                    img.style.borderRadius = "0.25rem"; img.style.marginRight = "0.5rem";
                    previewContainer.appendChild(img);
                };
                reader.readAsDataURL(file);
            });
        }
    });

    if (submitBtn) {
        submitBtn.addEventListener('click', () => {
            if (submitBtn.disabled) return;

            const contentRaw = document.getElementById('review-content')?.value || "";
            const content = contentRaw.trim().replace(/\s{2,}/g, ' ');
            const rating = document.getElementById('review-rating')?.value;

            if (content.length < 1 || content.length > 100) {
                openModal("입력 오류", `<p>리뷰 내용은 1자 이상 100자 이하로 작성해주세요.</p>`, { confirmText: '확인' });
                return;
            }
            submitBtn.disabled = true;
            const mode = writeForm.dataset.mode;
            const editId = writeForm.dataset.editId;

            const xhr = new XMLHttpRequest();

            xhr.onreadystatechange = () => {
                if (xhr.readyState !== XMLHttpRequest.DONE) {
                    return;
                }
                submitBtn.disabled = false;
                if (xhr.status < 200 || xhr.status >= 400) {
                    handleReviewError();
                    return;
                }
                handleReviewResponse(JSON.parse(xhr.responseText));
            };
            if (mode === 'edit') {
                xhr.open('PATCH', `/item/review/${editId}`);
                xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
                xhr.send(`content=${encodeURIComponent(content)}&rating=${encodeURIComponent(rating)}`);
            } else {
                xhr.open('POST', '/item/review');
                const formData = new FormData(writeForm);
                formData.set('content', content);
                xhr.send(formData);
            }
        });
    }

    const usefulToggle = document.getElementById('useful-toggle');
    usefulToggle?.addEventListener('change', function() {
        const sortType = this.checked ? 'useful' : 'latest';
        const urlParams = new URLSearchParams(window.location.search);
        const itemId = urlParams.get('id');

        const xhr = new XMLHttpRequest();
        xhr.onreadystatechange = () => {
            if (xhr.readyState !== XMLHttpRequest.DONE) {
                return;
            }

            if (xhr.status < 200 || xhr.status >= 400) {
                return;
            }
            renderReviewList(JSON.parse(xhr.responseText));
        };
        xhr.open('GET', `/item/reviews?itemId=${itemId}&sort=${sortType}`);
        xhr.send();
    });

    const handleReviewResponse = (data) => {
        if (data.result === 'SUCCESS') {
            openModal("처리 성공", `<p>후기가 성공적으로 반영되었습니다!</p>`, {
                confirmText: '확인', onConfirm: () => { location.reload(); }
            });
        } else if (data.result === 'FAILURE_SESSION') {
            openModal("로그인 필요", `<p>로그인이 필요한 서비스입니다.</p>`, {
                confirmText: '로그인 이동', onConfirm: () => { location.href = '/login'; }
            });
        } else {
            openModal("처리 실패", `<p>권한이 없거나 오류가 발생했습니다.</p>`, { confirmText: '확인' });
        }
    };

    const handleReviewError = () => { openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, { confirmText: '확인' }); };
});

// 사이즈선택, 장바구니
const sizeDataInput = document.getElementById('serverSizeData');
let sizes = [];
if (sizeDataInput && sizeDataInput.value) sizes = sizeDataInput.value.split(',').map(s => s.trim());

const modal = document.getElementById('optionModal');
const openBtn = document.querySelector('.optionLink');
const closeBtn = document.querySelector('.close');
const sizeGrid = document.getElementById('sizeGrid');
const confirmButton = document.getElementById('confirmSizeButton');
const buyButton = document.querySelector('.buying');
const cartButton = document.querySelector('.cart_Add .cart');
const selectOptionText = document.querySelector('.selectOption b');
let currentSelectedSize = null;

function renderSizeButtons() {
    sizeGrid.innerHTML = '';
    sizes.forEach(size => {
        const button = document.createElement('button');
        button.classList.add('size');
        button.textContent = size;
        button.dataset.size = size;
        if (size === currentSelectedSize) button.classList.add('selected');
        button.addEventListener('click', (e) => {
            document.querySelectorAll('.size').forEach(btn => btn.classList.remove('selected'));
            e.target.classList.add('selected');
            currentSelectedSize = e.target.dataset.size;
        });
        sizeGrid.appendChild(button);
    });
}

if (openBtn) { openBtn.addEventListener('click', (e) => { e.preventDefault(); renderSizeButtons(); modal.style.display = 'flex'; }); }
if (closeBtn) closeBtn.addEventListener('click', () => modal.style.display = 'none');
window.addEventListener('click', (e) => { if (e.target === modal) modal.style.display = 'none'; });

if (confirmButton) {
    confirmButton.addEventListener('click', () => {
        if (currentSelectedSize) { selectOptionText.textContent = currentSelectedSize; modal.style.display = 'none'; }
        else openModal("ERROR", `<p>사이즈를 선택해주세요</p>`, { confirmText: '확인' });
    });
}

if (buyButton) {
    buyButton.addEventListener('click', () => {
        if (currentSelectedSize) {
            const itemId = new URLSearchParams(window.location.search).get('id');
            location.href = `/pay?itemId=${itemId}&size=${currentSelectedSize}`;
        } else openModal("ERROR", `<p>사이즈를 선택해주세요</p>`, { confirmText: '확인' });
    });
}

if (cartButton) {
    cartButton.addEventListener('click', (e) => {
        e.preventDefault();
        if (currentSelectedSize) {
            const itemId = new URLSearchParams(window.location.search).get('id');
            const formData = new FormData();
            formData.append('itemId', itemId);
            formData.append('size', currentSelectedSize);
            formData.append('quantity', 1);
            const xhr = new XMLHttpRequest();
            xhr.open('POST', '/cart');
            xhr.onreadystatechange = () => {
                if (xhr.readyState === XMLHttpRequest.DONE) {
                    if (xhr.status >= 200 && xhr.status < 300) {
                        const response = JSON.parse(xhr.responseText);
                        if (response.result === 'SUCCESS') {
                            openModal("장바구니 담기 성공", `<p>장바구니에 상품이 담겼습니다.</p>`, {
                                confirmText: '장바구니로 이동',
                                cancelText: '계속 쇼핑하기',
                                onConfirm: () => {
                                    location.href = '/cart';
                                },
                                onCancel: () => {
                                    // 계속 쇼핑하기: 아무 동작 안 함 (모달 닫힘)
                                }
                            });
                        } else {
                            openModal("ERROR", `<p>${response.message || '장바구니 담기에 실패했습니다.'}</p>`, {
                                confirmText: '확인'
                            });
                        }
                    } else {
                        openModal("ERROR", `<p>오류가 발생했습니다.</p>`, {
                            confirmText: '확인'
                        });
                    }
                }
            };
            xhr.send(formData);
        } else openModal("ERROR", `<p>사이즈를 선택해주세요</p>`, { confirmText: '확인' });
    });
}

// --- 관심상품,관심매장
const toggleLikeItem = (shopId, itemId) => {
    const formData = new FormData();
    formData.append('shopId', shopId);
    formData.append('itemId', itemId);

    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }

        if (xhr.status < 200 || xhr.status >= 400) {
            return;
        }
        const data = JSON.parse(xhr.responseText);
        if (data.result === "FAILURE_SESSION") {
            openModal("로그인 필요", `<p>로그인이 필요합니다.</p>`, {
                confirmText: '확인',
                onConfirm: () => location.href = '/login'
            });
        } else if (data.result === 'SUCCESS') {
            openModal("SUCCESS", `<p>관심상품으로 저장했습니다.</p>`, {
                confirmText: '확인',
                onConfirm: () => location.href = "/my?open=likes-item"
            });
        } else {
            openModal("FAILURE", `<p>관심상품 등록을 취소하였습니다.</p>`, {
                confirmText: '확인'
            });
        }
    };

    xhr.open('POST', '/item/like');
    xhr.send(formData);
}


// --- 사이즈테이블 탭전환 ---
window.changeSizeTab = function (type) {
    // 모든 사이즈 테이블 숨기기
    document.querySelectorAll('.size-table').forEach(table => {
        table.style.display = 'none';
    });

    // 선택한 타입의 테이블만 보여주기
    const targetTable = document.getElementById('size-' + type);
    if (targetTable) {
        targetTable.style.display = 'table';
    }

    // 탭 버튼 활성화 스타일 처리
    document.querySelectorAll('.s-tab').forEach(btn => {
        btn.classList.remove('active');
    });

    // 클릭된 버튼에 active 클래스 추가
    if (event && event.currentTarget) {
        event.currentTarget.classList.add('active');
    }
};

// 사이즈테이블 : 가로휠
document.addEventListener("wheel", (e) => {
    const tableWrapper = e.target.closest('.size-table-wrapper');
    if (tableWrapper && tableWrapper.scrollWidth > tableWrapper.clientWidth && e.deltaY !== 0) {
        e.preventDefault();
        tableWrapper.scrollLeft += e.deltaY;
    }
}, { passive: false });

const toggleLikeShop = (shopId) => {
    const formData = new FormData();
    formData.append('shopId', shopId);
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }

        if (xhr.status < 200 || xhr.status >= 400) {
            return;
        }
        const data = JSON.parse(xhr.responseText);
        if (data.result === "FAILURE_SESSION") {
            openModal("로그인 필요", `<p>로그인이 필요합니다.</p>`, {
                confirmText: '확인',
                onConfirm: () => location.href = '/login'
            });
        } else if (data.result === 'SUCCESS') {
            openModal("SUCCESS", `<p>관심매장에 등록되었습니다.</p>`, {
                confirmText: '확인',
                onConfirm: () => location.href = "/my?open=likes-shop"
            });
        } else {
            openModal("FAILURE", `<p>관심매장 등록을 취소하였습니다.</p>`, {
                confirmText: '확인'
            });
        }
    };

    xhr.open('POST', '/shop/like');
    xhr.send(formData);
}
//리뷰삭제
function deleteReview(reviewId) {
    openModal("리뷰 삭제", `<p>정말 이 후기를 삭제하시겠습니까?</p>`, {
        confirmText: '삭제',
        cancelText: '취소',
        onConfirm: () => {
            const xhr = new XMLHttpRequest();
            xhr.onreadystatechange = () => {
                if (xhr.readyState !== XMLHttpRequest.DONE) {
                    return;
                }
                if (xhr.status < 200 || xhr.status >= 400) {
                    return;
                }
                const response = JSON.parse(xhr.responseText);
                if (response.result === 'SUCCESS') {
                    location.reload();
                }
            };
            xhr.open('DELETE', `/item/review/${reviewId}`);
            xhr.send();
        }
    });
}
//수정
function openEditReview(reviewId) {
    const xhr = new XMLHttpRequest();

    xhr.onreadystatechange = () => {
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (xhr.status >= 200 && xhr.status < 400) {

                const review = JSON.parse(xhr.responseText);
                const writeForm = document.getElementById('style-write-form');
                const previewContainer = document.getElementById('image-preview-container');

                // 후기 작성 토글 활성화
                if (document.getElementById('review-write-toggle')) {
                    document.getElementById('review-write-toggle').checked = true;
                }

                if (writeForm) {
                    writeForm.style.display = 'block';
                    // 수정 모드임을 표시 (MODE: edit)
                    writeForm.dataset.mode = 'edit';
                    writeForm.dataset.editId = reviewId;

                    // 기존 내용 및 별점 복구
                    const contentInput = document.getElementById('review-content');
                    const ratingInput = document.getElementById('review-rating');
                    if (contentInput) contentInput.value = review.content;
                    if (ratingInput) ratingInput.value = review.rating;

                    //업로드된 이미지 미리보기 생성---
                    if (previewContainer) {
                        previewContainer.innerHTML = ''; // 초기화
                        if (review.images && review.images.length > 0) {
                            review.images.forEach(imgName => {
                                const img = document.createElement('img');
                                img.src = `/review-images/${imgName}`;
                                img.style.width = "5rem";
                                img.style.height = "5rem";
                                img.style.objectFit = "cover";
                                img.style.borderRadius = "0.25rem";
                                img.style.marginRight = "0.5rem";
                                previewContainer.appendChild(img);
                            });
                        }
                    }

                    writeForm.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            } else {
                if (typeof openModal === 'function') {
                    openModal("오류", "<p>리뷰 정보를 불러오지 못했습니다.</p>", { confirmText: '확인' });
                }
            }
        }
    };
    xhr.open('GET', `/item/review/${reviewId}`);
    xhr.send();
}
//도움돼요
function toggleReviewLike(reviewId, btnElement) {
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('reviewId', reviewId);

    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            return;
        }
        const data = JSON.parse(xhr.responseText);
        // SUCCESS(도움돼요) 또는 FAILURE(좋아요 취소) 결과에 따라 UI 업데이트
        if (data.result === 'SUCCESS' || data.result === 'FAILURE') {
            const countElement = btnElement.querySelector('.like-count');
            if (countElement) {
                countElement.innerText = data.count;
            }
            btnElement.classList.toggle('active', data.result === 'SUCCESS');
        } else if (data.result === 'FAILURE_SESSION') {
            openModal("로그인 필요", `<p>로그인이 필요합니다.</p>`, {
                confirmText: '확인',
                onConfirm: () => location.href = '/login'
            });
        }
    };

    xhr.open('POST', '/item/review/like');
    xhr.send(formData);
}


// 댓글 및 대댓글
window.deleteComment = function(commentId) {
    const item = document.querySelector(`.comment-item[data-comment-id="${commentId}"]`);
    const card = item ? item.closest('.review-item-card') : null;

    // 대댓글 존재 여부 체크
    // 현재 댓글 바로 다음 요소가 'reply' 클래스를 가지고 있는지 확인
    const nextItem = item.nextElementSibling;
    const hasReply = nextItem && nextItem.classList.contains('reply');

    if (hasReply) {
        // 대댓글이 있으면 삭제를 중단하고 안내 모달을 띄웁니다.
        if (typeof openModal === 'function') {
            openModal("삭제 불가", "<p>답글이 있는 댓글은 서비스 정책상 삭제할 수 없습니다.</p>", { confirmText: '확인' });
        }
        return; // 함수 실행 종료
    }
    // ------------------------------------------

    openModal("댓글 삭제", "<p>댓글을 정말 삭제하시겠습니까?</p>", {
        confirmText: '삭제',
        cancelText: '취소',
        onConfirm: () => {
            const xhr = new XMLHttpRequest();

            xhr.onreadystatechange = () => {
                if (xhr.readyState !== XMLHttpRequest.DONE) return;
                if (xhr.status < 200 || xhr.status >= 400) {
                    openModal("오류", "<p>삭제 실패했습니다.</p>", { confirmText: '확인' });
                    return;
                }

                const data = JSON.parse(xhr.responseText);
                if (data.result === 'SUCCESS' && item) {
                    item.style.transition = "opacity 0.3s ease";
                    item.style.opacity = "0";
                    setTimeout(() => {
                        item.remove();
                        // 모든 댓글이 사라졌을 때 '첫 댓글' 문구 표시 유지
                        if (card && card.querySelectorAll('.comment-item').length === 0) {
                            const listBody = card.querySelector('.comment-list');
                            if (listBody) {
                                listBody.innerHTML = '<p class="empty-msg" style="text-align:center; padding:1rem; color:#ccc;">첫 댓글을 남겨보세요.</p>';
                            }
                        }
                    }, 300);
                } else {
                    openModal("오류", "<p>삭제 실패했습니다.</p>", { confirmText: '확인' });
                }
            };
            xhr.open('DELETE', `/item/review/comment/${commentId}`);
            xhr.send();
        }
    });
};

window.showEditCommentForm = function(commentId, btnElement) {
    const item = btnElement.closest('.comment-item');
    const contentP = item.querySelector('.content');
    const originalText = contentP.innerText;
    if (item.querySelector('.edit-comment-wrapper')) return;

    const editHtml = `
        <div class="edit-comment-wrapper" style="margin-top:0.5rem;">
            <textarea class="edit-input" style="width:100%; height:2.5rem; padding:0.3125rem; resize:none; border:0.0625rem solid #ddd; border-radius:0.25rem;">${originalText}</textarea>
            <div class="form-action-btns" style="display:flex; gap:0.3125rem; margin-top:0.3125rem; justify-content:flex-end;">
                <button type="button" onclick="loadCommentsAfterCancel(${commentId}, this)" style="padding:0.2rem 0.6rem; border:0.0625rem solid #ddd; background:#fff; cursor:pointer; border-radius:0.25rem; font-size:0.8rem;">취소</button>
                <button type="button" onclick="submitEditComment(${commentId}, this)" style="padding:0.2rem 0.6rem; background:#333; color:#fff; border:none; cursor:pointer; border-radius:0.25rem; font-size:0.8rem;">수정 완료</button>
            </div>
        </div>`;

    contentP.style.display = 'none';
    const actions = item.querySelector('.comment-actions');
    if (actions) actions.style.display = 'none';
    const ownerBtns = item.querySelector('.owner-comment-btns');
    if (ownerBtns) ownerBtns.style.display = 'none';
    item.insertAdjacentHTML('beforeend', editHtml);
};

window.loadCommentsAfterCancel = function(commentId, btnElement) {
    const reviewCard = btnElement.closest('.review-item-card');
    if (reviewCard) loadComments(reviewCard.dataset.reviewId, reviewCard.querySelector('.comment-section'));
};

window.submitEditComment = function(commentId, btnElement) {
    const input = btnElement.closest('.edit-comment-wrapper').querySelector('.edit-input');
    // 댓글 공백 제거 및 연속 공백 정리 로직 보존
    const content = input.value.trim().replace(/\s{2,}/g, ' ');

    // 글자 수 유효성 검사 보존
    if (content.length < 1 || content.length > 100) {
        openModal("입력 오류", `<p>댓글 내용은 1자 이상 100자 이하로 작성해주세요.</p>`, { confirmText: '확인' });
        return;
    }

    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            return;
        }
        const data = JSON.parse(xhr.responseText);
        if (data.result === 'SUCCESS') {
            const card = btnElement.closest('.review-item-card');
            if (card) {
                loadComments(card.dataset.reviewId, card.querySelector('.comment-section'));
            }
        }
    };
    xhr.open('PATCH', `/item/review/comment/${commentId}`);
    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    xhr.send(`content=${encodeURIComponent(content)}`);
};



//갤러리 상태 전역 관리
window.galleryImages = [];
window.galleryCurrentIndex = 0;

window.viewReviewImages = function(reviewId, clickedImgElement) {
    // 화면에 보이는 4개와 display:none된 모든 리뷰 이미지를 한꺼번에 수집
    const allImages = document.querySelectorAll('.style-review-full-img');
    const imagesUrl = Array.from(allImages).map(img => img.src);


    const clickedSrc = clickedImgElement.src;
    const startIndex = imagesUrl.indexOf(clickedSrc);


    // console.log("갤러리 수집 총 개수:", imagesUrl.length);

    if (imagesUrl.length > 0) {
        window.openImageGallery(imagesUrl, startIndex === -1 ? 0 : startIndex);
    }
};

//갤러리 열기 및 이동 로직
window.openImageGallery = function(imagesUrl, startIndex) {
    window.galleryImages = imagesUrl;
    window.galleryCurrentIndex = startIndex;

    const modalContent = `
        <div class="gallery-wrapper" style="position: relative; width: 100%; display: flex; justify-content: center; align-items: center; padding: 1rem 0;">
            <div class="gallery-container" style="width: 100%; max-width: 31.25rem; aspect-ratio: 1 / 1; 
            background: #fff; display: flex; justify-content: center; align-items: center; overflow: hidden; border-radius: 0.5rem;">
                <img id="gallery-current-img" src="${window.galleryImages[window.galleryCurrentIndex]}" style="width: 100%; height: 100%; object-fit: contain;">
            </div>
            ${window.galleryImages.length > 1 ? `
             <button onclick="window.moveGallery(-1, event)" class="gallery-nav prev">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
    </button>
    <button onclick="window.moveGallery(1, event)" class="gallery-nav next">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
    </button>
            ` : ''}
        </div>
    `;
    openModal("STYLE REVIEW", modalContent, { confirmText: '닫기' });
};


window.moveGallery = function(step, event) {
    if (event) event.stopPropagation();
    window.galleryCurrentIndex = (window.galleryCurrentIndex + step + window.galleryImages.length) % window.galleryImages.length;
    const imgElement = document.getElementById('gallery-current-img');
    if (imgElement) {
        imgElement.src = window.galleryImages[window.galleryCurrentIndex];
    }
};

//리뷰리스트,댓글로드
function toggleCommentSection(reviewId) {
    const reviewCard = document.querySelector(`.review-item-card[data-review-id="${reviewId}"]`);
    const commentBox = reviewCard?.querySelector('.comment-section');
    if (!commentBox) return;
    if (commentBox.style.display === 'none' || !commentBox.style.display) {
        commentBox.style.display = 'block';
        loadComments(reviewId, commentBox);
    } else {
        commentBox.querySelector('.comment-input').value = '';
        commentBox.style.display = 'none';
    }
}

function loadComments(reviewId, container) {
    const sessionUserEmail = document.getElementById('sessionUserEmail')?.value;
    const xhr = new XMLHttpRequest();

    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }

        if (xhr.status < 200 || xhr.status >= 400) {
            return;
        }
        const comments = JSON.parse(xhr.responseText);
        const listBody = container.querySelector('.comment-list');
        if (!listBody) return;

        // 목록 초기화 및 데이터 유무에 따른 메시지 처리 보존
        listBody.innerHTML = comments.length === 0 ? '<p class="empty-msg" style="text-align:center; padding:1rem; color:#ccc;">첫 댓글을 남겨보세요.</p>' : '';

        comments.forEach(comment => {
            const commentItem = document.createElement('div');
            // 대댓글 여부에 따른 클래스 부여 로직 보존
            commentItem.className = `comment-item ${comment.parentId ? 'reply' : ''}`;
            commentItem.dataset.commentId = comment.id;

            // 작성자 본인 확인 로직 보존
            const isMyComment = (sessionUserEmail === comment.userEmail);

            commentItem.innerHTML = `
                <div class="comment-header">
                    <strong class="author">${comment.userName || '익명'}</strong>
                    <span class="date">${comment.createdAt.replace('T', ' ').substring(0, 16)}</span>
                </div>
                <p class="content">${comment.content}</p>
                <div class="comment-actions">
                    ${!comment.parentId ? `<button class="btn-reply" onclick="showReplyForm(${reviewId}, ${comment.id}, this)">답글 달기</button>` : ''}
                </div>
                ${isMyComment ? `
                    <div class="owner-comment-btns">
                        <button onclick="showEditCommentForm(${comment.id}, this)">수정</button>
                        <button onclick="deleteComment(${comment.id})">삭제</button>
                    </div>` : ''}`;
            listBody.appendChild(commentItem);
        });
    };
    xhr.open('GET', `/item/review/comments/${reviewId}`);
    xhr.send();
}

function showReplyForm(reviewId, parentId, btnElement) {
    document.querySelector('.reply-form-wrapper')?.remove();
    const formHtml = `
        <div class="reply-form-wrapper" style="margin: 0.625rem 0;">
            <textarea class="reply-input" placeholder="댓글을 1자 이상 100자 이하로 작성해주세요." style="width:100%; height:2.5rem; padding:0.3125rem; resize:none;"></textarea>
            <div class="form-action-btns" style="display:flex; gap:0.3125rem; margin-top:0.3125rem; justify-content:flex-end;">
                <button type="button" class="btn-form-cancel" onclick="this.closest('.reply-form-wrapper').remove()" style="padding:0.125rem 0.625rem; border:0.0625rem solid #ddd; background:#fff; cursor:pointer; border-radius:0.25rem;">취소</button>
                <button type="button" class="btn-form-submit" onclick="submitComment(${reviewId}, ${parentId}, this)" style="padding:0.125rem 0.625rem; background:#333; color:#fff; border:none; cursor:pointer; border-radius:0.25rem;">등록</button>
            </div>
        </div>`;
    btnElement.parentElement.insertAdjacentHTML('afterend', formHtml);
}

function submitComment(reviewId, parentId, btnElement) {
    const container = btnElement.closest('.comment-form') || btnElement.closest('.reply-form-wrapper');
    const input = container?.querySelector('textarea');
    if (!input) return;
    // 댓글 공백 제거 및 연속 공백 정리 로직 보존
    const content = input.value.trim().replace(/\s{2,}/g, ' ');
    // 글자 수 유효성 검사 보존
    if (content.length < 1 || content.length > 100) {
        openModal("입력 오류", `<p>댓글 내용은 1자 이상 100자 이하로 작성해주세요.</p>`, { confirmText: '확인' });
        return;
    }
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('reviewId', reviewId);
    formData.append('content', content);
    if (parentId) formData.append('parentId', parentId);
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            return;
        }
        const data = JSON.parse(xhr.responseText);
        if (data.result === 'SUCCESS') {
            input.value = '';
            // 답글 폼인 경우 폼 제거
            if (parentId && container.classList.contains('reply-form-wrapper')) {
                container.remove();
            }
            const reviewCard = document.querySelector(`.review-item-card[data-review-id="${reviewId}"]`);
            const commentBox = reviewCard?.querySelector('.comment-section');
            if (commentBox) {
                loadComments(reviewId, commentBox);
            }
        }
    };
    xhr.open('POST', '/item/review/comment');
    xhr.send(formData);
}

function renderReviewList(reviews) {
    const container = document.querySelector('.review-list-container');
    if (!container) return;
    const sessionEmail = document.getElementById('sessionUserEmail')?.value;
    container.innerHTML = reviews.length === 0 ? '<p class="empty-msg" style="text-align:center; padding:3rem; color:#999;">검색 결과가 없습니다.</p>' : '';
    reviews.forEach(review => {
        const stars = '★'.repeat(review.rating) + '☆'.repeat(5 - review.rating);


        let imagesHtml = (review.images || []).map((img, idx) => {
            const imgSrc = img.startsWith('data:image') ? img : `/review-images/${img}`;
            return `
                <div class="review-img-box" onclick="viewReviewImages(${review.id}, ${idx})" style="cursor:pointer; width: 6.25rem; height: 6.25rem; overflow: hidden; border-radius: 0.25rem;">
                    <img src="${imgSrc}" alt="리뷰이미지" style="width: 100%; height: 100%; object-fit: cover;" onerror="this.parentElement.style.display='none'">
                </div>`;
        }).join('');

        if (imagesHtml) {
            imagesHtml = `<div class="review-contents-images" style="display:flex; gap:0.5rem; flex-wrap:wrap; margin-bottom: 1rem;">${imagesHtml}</div>`;
        }

        const isAuthor = (sessionEmail === review.userEmail);

        const itemHtml = `
            <div class="review-item-card" data-review-id="${review.id}" style="margin-bottom:2rem; border-bottom:0.0625rem solid #eee; padding-bottom:1.5rem;">
                <div class="review-card-header" style="display:flex; justify-content:space-between; align-items:center;">
                    <div class="user-meta" style="display:flex; align-items:center; gap:0.75rem;">
                        <img src="/assets/images/my/default_profile.png" class="user-avatar" alt="profile" style="width:2.5rem; height:2.5rem; border-radius:50%;">
                        <div class="user-text">
                            <strong class="user-name">${review.userName || '익명'}</strong>
                            <span class="review-date" style="font-size:0.8rem; color:#999; margin-left:0.5rem;">
                                ${review.createdAt ? review.createdAt.substring(0, 10).replace(/-/g, '.') : ''}
                            </span>
                            <div class="star-rating" style="color: #f1c40f;">${stars}</div>
                        </div>
                    </div>
                    ${isAuthor ? `
                        <div class="owner-btns">
                            <button type="button" class="btn-action-text" onclick="openEditReview(${review.id})" style="background:none; 
                            border:none; color:#999; cursor:pointer; font-size:0.8rem;">수정</button>
                            <button type="button" class="btn-action-text" onclick="deleteReview(${review.id})" style="background:none; 
                            border:none; color:#999; cursor:pointer; font-size:0.8rem;">삭제</button>
                        </div>` : ''}
                </div>
                
                <div class="review-card-content" style="margin-top:1rem;">
                    ${imagesHtml}
                    <p class="content-text" style="margin: 1rem 0; line-height: 1.6;">${review.content}</p>
                </div>
                
                <div class="review-card-footer" style="display:flex; gap:0.5rem;">
                    <button type="button" class="btn-utility ${review.isLiked ? 'active' : ''}" onclick="toggleReviewLike(${review.id}, this)" style="padding: 0.3rem 0.6rem; border:0.0625rem solid #ddd; border-radius: 0.25rem; background: #fff; cursor: pointer;">
                        도움 돼요 <span class="like-count">${review.usefulCount}</span>
                    </button>
                    <button type="button" class="btn-utility" onclick="toggleCommentSection(${review.id})" style="padding: 0.3rem 0.6rem; border: 0.0625rem solid #ddd; border-radius: 0.25rem; background: #fff; cursor: pointer;">댓글</button>
                </div>
                
                <div class="comment-section" style="display: none; margin-top: 0.9375rem; padding: 0.9375rem; background: #f9f9f9; border-radius: 0.5rem;">
                    <div class="comment-form" style="margin-bottom: 0.625rem; display: flex; gap: 0.3125rem;">
                        <textarea class="comment-input" placeholder="댓글을 1자 이상 100자 이하로 작성해주세요." style="flex: 1; height: 2.5rem; 
                        padding: 0.3125rem; resize: none; border:0.0625rem solid #ddd;"></textarea>
                        <button onclick="submitComment(${review.id}, null, this)" style="padding: 0 0.9375rem; background: #333;
                         color: #fff; border: none; border-radius: 0.25rem; cursor: pointer;">등록</button>
                    </div>
                    <div class="comment-list"></div>
                </div>
            </div>`;

        container.insertAdjacentHTML('beforeend', itemHtml);
    });
}