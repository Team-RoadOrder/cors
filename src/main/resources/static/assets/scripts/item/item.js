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
    //#region : 기존 handelReviewResponse
    // const handleReviewResponse = (data) => {
    //     if (data.result === 'SUCCESS') {
    //         openModal("처리 성공", `<p>후기가 성공적으로 반영되었습니다!</p>`, {
    //             confirmText: '확인', onConfirm: () => { location.reload(); }
    //         });
    //     } else if (data.result === 'FAILURE_SESSION') {
    //         openModal("로그인 필요", `<p>로그인이 필요한 서비스입니다.</p>`, {
    //             confirmText: '로그인 이동', onConfirm: () => { location.href = '/login'; }
    //         });
    //     } else {
    //         openModal("처리 실패", `<p>권한이 없거나 오류가 발생했습니다.</p>`, { confirmText: '확인' });
    //     }
    // };
    //#endregion : 기존 handelReviewResponse
    const handleReviewResponse = (data) => {
        if (data.result === 'SUCCESS') {
            openModal("처리 성공", `<p>후기가 성공적으로 반영되었습니다!</p>`, {
                confirmText: '확인', onConfirm: () => { location.reload(); }
            });
        } else if (data.result === 'FAILURE_SESSION') {
            openModal("로그인 필요", `<p>로그인이 필요한 서비스입니다.</p>`, {
                confirmText: '로그인 이동', onConfirm: () => { location.href = '/login'; }
            });
        } else if (data.result === 'FAILURE') {
            // [FBI 정규화] 1구매-1리뷰 원칙 및 60일 기한 만료 통합 안내
            openModal("작성 불가",
                `<p>리뷰 작성 권한이 없거나 구매 후 60일이 경과했습니다.</p>
                 <p style="font-size: 0.85rem; color: #888; margin-top: 0.5rem;">
                    * 삭제 후 재작성도 구매일로부터 60일 이내에만 가능합니다.
                 </p>`,
                { confirmText: '확인' }
            );
        } else {
            openModal("처리 실패", `<p>알 수 없는 오류가 발생했습니다.</p>`, { confirmText: '확인' });
        }
    };
    const handleReviewError = () => { openModal("ERROR", `<p>서버 통신 중 에러가 발생했습니다.</p>`, { confirmText: '확인' }); };
});

// 사이즈선택, 장바구니
// [수정] 모바일용 데이터(serverSizeData-M)도 함께 찾도록 수정
const sizeDataInput = document.getElementById('serverSizeData') || document.getElementById('serverSizeData-M');
let sizes = [];
if (sizeDataInput && sizeDataInput.value) sizes = sizeDataInput.value.split(',').map(s => s.trim());

// [수정] PC와 모바일 요소를 모두 선택할 수 있도록 querySelectorAll과 closest 로직을 최소한으로 적용
const modal = document.getElementById('optionModal');
const modalM = document.getElementById('optionModal-M');
const openBtns = document.querySelectorAll('.optionLink');
const closeBtns = document.querySelectorAll('.close');
const sizeGrid = document.getElementById('sizeGrid');
const sizeGridM = document.getElementById('sizeGrid-M');
const confirmButtons = document.querySelectorAll('.confirm-size-button');
const buyButtons = document.querySelectorAll('.buying');
const cartButtons = document.querySelectorAll('.cart_Add .cart');
const selectOptionTexts = document.querySelectorAll('.selectOption b');
let currentSelectedSize = null;

function renderSizeButtons(targetGrid, wrapper) {
    if (!targetGrid) return;
    targetGrid.innerHTML = '';
    sizes.forEach(size => {
        const button = document.createElement('button');
        button.classList.add('size');
        button.textContent = size;
        button.dataset.size = size;
        if (size === currentSelectedSize) button.classList.add('selected');
        button.addEventListener('click', (e) => {
            // [수정] 클릭한 구역 내의 버튼들만 selected 클래스 제어
            wrapper.querySelectorAll('.size').forEach(btn => btn.classList.remove('selected'));
            e.target.classList.add('selected');
            currentSelectedSize = e.target.dataset.size;
        });
        targetGrid.appendChild(button);
    });
}

// [수정] if (openBtn) 대신 모든 버튼에 대해 처리 (모바일 포함)
openBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
        e.preventDefault();
        const wrapper = btn.closest('#buying-wrapper, #buying-wrapper-M');
        const targetGrid = wrapper.querySelector('.size-options');
        const targetModal = wrapper.querySelector('.modal');

        renderSizeButtons(targetGrid, wrapper);
        if (targetModal) targetModal.style.display = 'flex';
    });
});

// [수정] 모든 닫기 버튼 대응
closeBtns.forEach(btn => {
    btn.addEventListener('click', () => {
        const targetModal = btn.closest('.modal');
        if (targetModal) targetModal.style.display = 'none';
    });
});

window.addEventListener('click', (e) => {
    if (e.target === modal || e.target === modalM) {
        e.target.style.display = 'none';
    }
});

// [수정] 확인 버튼 클릭 시 모든 텍스트 동기화
confirmButtons.forEach(btn => {
    btn.addEventListener('click', () => {
        if (currentSelectedSize) {
            selectOptionTexts.forEach(txt => txt.textContent = currentSelectedSize);
            btn.closest('.modal').style.display = 'none';
        } else {
            openModal("ERROR", `<p>사이즈를 먼저 선택해주세요</p>`, { confirmText: '확인' });
        }
    });
});

// [수정] 구매/장바구니 버튼들 (querySelectorAll로 변경하여 모든 버튼 대응)
buyButtons.forEach(btn => {
    btn.addEventListener('click', () => {
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
                            location.href = '/cart';
                        } else {
                            openModal("ERROR", `<p>${response.message || '구매 처리에 실패했습니다.'}</p>`, { confirmText: '확인' });
                        }
                    }
                }
            };
            xhr.send(formData);
        } else openModal("ERROR", `<p>사이즈를 선택해주세요</p>`, { confirmText: '확인' });
    });
});

cartButtons.forEach(btn => {
    btn.addEventListener('click', (e) => {
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
                            if (typeof updateCartCount === 'function') updateCartCount();
                            openModal("장바구니 담기 성공", `<p>장바구니에 상품이 담겼습니다.</p>`, {
                                confirmText: '장바구니로 이동',
                                cancelText: '계속 쇼핑하기', // <-- 이 줄이 빠졌었습니다!
                                onConfirm: () => {
                                    location.href = '/cart';
                                },
                                onCancel: () => {
                                    // 계속 쇼핑하기 클릭 시 모달만 닫힘 (로직 추가 필요 없음)
                                }
                            });
                        }
                    }
                }
            };
            xhr.send(formData);
        } else openModal("ERROR", `<p>사이즈를 선택해주세요</p>`, { confirmText: '확인' });
    });
});

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
            openModal("로그인 필요", `<p>로그인이 필요한 서비스입니다.</p>`, {
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
//#region: 기존 deleteReview
// function deleteReview(reviewId) {
//     openModal("리뷰 삭제", `<p>정말 이 후기를 삭제하시겠습니까?</p>`, {
//         confirmText: '삭제',
//         cancelText: '취소',
//         onConfirm: () => {
//             const xhr = new XMLHttpRequest();
//             xhr.onreadystatechange = () => {
//                 if (xhr.readyState !== XMLHttpRequest.DONE) {
//                     return;
//                 }
//                 if (xhr.status < 200 || xhr.status >= 400) {
//                     return;
//                 }
//                 const response = JSON.parse(xhr.responseText);
//                 if (response.result === 'SUCCESS') {
//                     location.reload();
//                 }
//             };
//             xhr.open('DELETE', `/item/review/${reviewId}`);
//             xhr.send();
//         }
//     });
// }
//#endregion
// [정규화] 리뷰 삭제 함수
function deleteReview(reviewId) {
    const finalContent = `
        <div style="text-align: center;">
            <p style="font-size: 1.05rem; margin-bottom: 1rem;">정말 이 후기를 삭제하시겠습니까?</p>
            <hr style="border: none; border-top: 1px solid #eee; margin: 1rem 0;">
            <div style="background-color: #f9f9f9; padding: 0.8rem; border-radius: 0.4rem; text-align: left;">
                <p style="font-size: 0.85rem; color: #555; font-weight: bold; margin-bottom: 0.3rem;">리뷰 정책 안내</p>
                <p style="font-size: 0.8rem; color: #888; line-height: 1.5; margin: 0;">
                    * 후기 삭제 시,<br> <b>구매일로부터 60일이 경과한 상품</b>은 시스템 정책상 재작성이 불가능합니다.<br>
                    * 60일 이내인 경우에만 삭제 후 다시 작성하실 수 있습니다.
                </p>
            </div>
        </div>
    `;

    openModal("리뷰 삭제", finalContent, {
        confirmText: '확인',
        cancelText: '취소',
        onConfirm: () => {
            const xhr = new XMLHttpRequest();
            xhr.onreadystatechange = () => {
                if (xhr.readyState === XMLHttpRequest.DONE && xhr.status === 200) {
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
    if (!item) return;

    const isReply = item.classList.contains('reply');
    const nextItem = item.nextElementSibling;
    const hasChild = nextItem && nextItem.classList.contains('reply');

    // [보안 정책] 원본 댓글인데 답글이 달려있는 경우만 삭제 차단
    if (!isReply && hasChild) {
        openModal("삭제 불가", "<p>답글이 있는 댓글은 서비스 정책상 삭제할 수 없습니다.</p>", { confirmText: '확인' });
        return;
    }

    openModal("댓글 삭제", "<p>댓글을 정말 삭제하시겠습니까?</p>", {
        confirmText: '삭제', cancelText: '취소',
        onConfirm: () => {
            const xhr = new XMLHttpRequest();
            xhr.onreadystatechange = () => {
                if (xhr.readyState !== XMLHttpRequest.DONE) return;
                const data = JSON.parse(xhr.responseText);
                if (data.result === 'SUCCESS') {
                    item.remove(); // 화면에서 즉시 제거
                } else {
                    openModal("오류", "<p>삭제 권한이 없거나 실패했습니다.</p>", { confirmText: '확인' });
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
        if (xhr.readyState !== XMLHttpRequest.DONE) return;
        if (xhr.status < 200 || xhr.status >= 400) return;

        const comments = JSON.parse(xhr.responseText);
        const listBody = container.querySelector('.comment-list');
        if (!listBody) return;

        listBody.innerHTML = comments.length === 0
            ? '<p class="empty-msg" style="text-align:center; padding:1rem; color:#ccc;">첫 댓글을 남겨보세요.</p>'
            : '';

        comments.forEach(comment => {
            const commentItem = document.createElement('div');
            // parentId가 있으면 대댓글(reply) 클래스 부여
            commentItem.className = `comment-item ${comment.parentId ? 'reply' : ''}`;
            commentItem.dataset.commentId = comment.id;

            // [보안 포인트] 세션 이메일과 댓글 이메일의 엄격한 비교
            const isMyComment = (sessionUserEmail && sessionUserEmail === comment.userEmail);

            commentItem.innerHTML = `
                <div class="comment-header">
                    <strong class="author">${comment.userName || '알 수 없는 사용자'}</strong>
                    <span class="date">${comment.createdAt.replace('T', ' ').substring(0, 16)}</span>
                </div>
                <p class="content">${comment.content}</p>
                
                <div class="comment-actions">
                    ${(!comment.parentId) ? `<button class="btn-reply" onclick="showReplyForm(${reviewId}, ${comment.id}, this)">답글 달기</button>` : ''}
                </div>

                ${isMyComment ? `
                    <div class="owner-comment-btns">
                        <button onclick="showEditCommentForm(${comment.id}, this)">수정</button>
                        <button onclick="deleteComment(${comment.id})">삭제</button>
                    </div>` : ''}
            `;
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


//#region : 기존 submitComment
// function submitComment(reviewId, parentId, btnElement) {
//     const container = btnElement.closest('.comment-form') || btnElement.closest('.reply-form-wrapper');
//     const input = container?.querySelector('textarea');
//     if (!input) return;
//     // 댓글 공백 제거 및 연속 공백 정리 로직 보존
//     const content = input.value.trim().replace(/\s{2,}/g, ' ');
//     // 글자 수 유효성 검사 보존
//     if (content.length < 1 || content.length > 100) {
//         openModal("입력 오류", `<p>댓글 내용은 1자 이상 100자 이하로 작성해주세요.</p>`, { confirmText: '확인' });
//         return;
//     }
//     const xhr = new XMLHttpRequest();
//     const formData = new FormData();
//     formData.append('reviewId', reviewId);
//     formData.append('content', content);
//     if (parentId) formData.append('parentId', parentId);
//     xhr.onreadystatechange = () => {
//         if (xhr.readyState !== XMLHttpRequest.DONE) {
//             return;
//         }
//         if (xhr.status < 200 || xhr.status >= 400) {
//             return;
//         }
//         const data = JSON.parse(xhr.responseText);
//         if (data.result === 'SUCCESS') {
//             input.value = '';
//             // 답글 폼인 경우 폼 제거
//             if (parentId && container.classList.contains('reply-form-wrapper')) {
//                 container.remove();
//             }
//             const reviewCard = document.querySelector(`.review-item-card[data-review-id="${reviewId}"]`);
//             const commentBox = reviewCard?.querySelector('.comment-section');
//             if (commentBox) {
//                 loadComments(reviewId, commentBox);
//             }
//         }
//     };
//     xhr.open('POST', '/item/review/comment');
//     xhr.send(formData);
// }
//endregion
// 댓글/답글 등록 로직]
function submitComment(reviewId, parentId, btnElement) {
    const container = btnElement.closest('.comment-form') || btnElement.closest('.reply-form-wrapper');
    const input = container?.querySelector('textarea');
    if (!input) return;

    const content = input.value.trim().replace(/\s{2,}/g, ' ');
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
        if (xhr.readyState !== XMLHttpRequest.DONE) return;
        if (xhr.status >= 200 && xhr.status < 400) {
            const data = JSON.parse(xhr.responseText);
            if (data.result === 'SUCCESS') {
                input.value = '';
                if (parentId) container.remove(); // 답글 폼 제거
                const reviewCard = document.querySelector(`.review-item-card[data-review-id="${reviewId}"]`);
                loadComments(reviewId, reviewCard.querySelector('.comment-section'));
            }
        }
    };
    xhr.open('POST', '/item/review/comment');
    xhr.send(formData);
}

//#region: 기존 renderReviewList
// function renderReviewList(reviews) {
//     const container = document.querySelector('.review-list-container');
//     if (!container) return;
//     const sessionEmail = document.getElementById('sessionUserEmail')?.value;
//     container.innerHTML = reviews.length === 0 ? '<p class="empty-msg" style="text-align:center; padding:3rem; color:#999;">검색 결과가 없습니다.</p>' : '';
//     reviews.forEach(review => {
//         const stars = '★'.repeat(review.rating) + '☆'.repeat(5 - review.rating);
//
//
//         let imagesHtml = (review.images || []).map((img, idx) => {
//             const imgSrc = img.startsWith('data:image') ? img : `/review-images/${img}`;
//             return `
//                 <div class="review-img-box" onclick="viewReviewImages(${review.id}, ${idx})" style="cursor:pointer; width: 6.25rem; height: 6.25rem; overflow: hidden; border-radius: 0.25rem;">
//                     <img src="${imgSrc}" alt="리뷰이미지" style="width: 100%; height: 100%; object-fit: cover;" onerror="this.parentElement.style.display='none'">
//                 </div>`;
//         }).join('');
//
//         if (imagesHtml) {
//             imagesHtml = `<div class="review-contents-images" style="display:flex; gap:0.5rem; flex-wrap:wrap; margin-bottom: 1rem;">${imagesHtml}</div>`;
//         }
//
//         const isAuthor = (sessionEmail === review.userEmail);
//
//         const itemHtml = `
//         <div class="review-item-card" data-review-id="${review.id}">
//             <div class="review-card-header">
//                 <div class="user-meta">
//                     <img src="/assets/images/my/default_profile.png" class="user-avatar">
//                     <div class="user-text">
//                         <strong class="user-name">${review.userName}</strong>
//                         <span class="review-date">${review.createdAt.substring(0, 10)}</span>
//                     </div>
//                 </div>
//                 </div>
//         </div>`;
//         container.insertAdjacentHTML('beforeend', itemHtml);
//     });
// }
//#endregion
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
        <div class="review-item-card" data-review-id="${review.id}">
            <div class="review-card-header">
                <div class="user-meta">
                    <img src="/assets/images/my/default_profile.png" class="user-avatar">
                    <div class="user-text">
                        <strong class="user-name">${review.userName}</strong>
                        <span class="review-date">${review.createdAt.substring(0, 10)}</span>
                    </div>
                </div>
                ${isAuthor ? `
                    <div class="owner-btns" style="display: flex; gap: 0.5rem;">
                        <button onclick="openEditReview(${review.id})" style="cursor:pointer; background:none; border:none; color:#666; font-size:0.8rem;">수정</button>
                        <button onclick="deleteReview(${review.id}, '${review.orderDate}')" style="cursor:pointer; background:none; border:none; color:#666; font-size:0.8rem;">삭제</button>
                    </div>` : ''}
            </div>
            <div class="review-contents" style="margin-top: 1rem;">
                <div class="stars" style="color: #ffcc00; margin-bottom: 0.5rem;">${stars}</div>
                <p class="text" style="line-height: 1.5; color: #333; margin-bottom: 1rem;">${review.content}</p>
                ${imagesHtml}
            </div>
            </div>`;
        container.insertAdjacentHTML('beforeend', itemHtml);
    });
}

const reserveItem = () => {
    if (!currentSelectedSize) {
        openModal("알림", "<p>사이즈를 먼저 선택해주세요.</p>", { confirmText: '확인' });
        return;
    }

    const productName = document.querySelector('.productName')?.innerText || '상품명 없음';

    const itemId = new URLSearchParams(window.location.search).get('id');

    const shopIdInput = document.querySelector('input[name="shopId"]');
    const shopId = shopIdInput ? shopIdInput.value : null;

    if (!shopId || !itemId) {
        openModal("ERROR", "<p>상품 정보를 불러올 수 없습니다.</p>", { confirmText: '확인' });
        return;
    }

    const modalContent = `
        <style>
            .reserve-modal-wrap { text-align: left; padding: 0.5rem; }
            .reserve-info-row { display: flex; justify-content: space-between; margin-bottom: 0.8rem; border-bottom: 1px solid #eee; padding-bottom: 0.5rem; }
            .reserve-info-row .label { font-weight: bold; color: #555; }
            .reserve-info-row .value { font-weight: bold; color: #000; }
            .calendar-container { display: flex; justify-content: center; margin-top: 1rem; }
            .flatpickr-calendar.inline { box-shadow: none; border: 1px solid #eee; }
        </style>
        
        <div class="reserve-modal-wrap">
            <div class="reserve-info-row">
                <span class="label">상품명</span>
                <span class="value">${productName}</span>
            </div>
            <div class="reserve-info-row">
                <span class="label">선택 사이즈</span>
                <span class="value" style="color: #333; font-size: 1.1em;">${currentSelectedSize}</span>
            </div>
            
            <div style="text-align: center; margin-top: 1rem; font-weight:bold; color:#333;">방문 예정 시간 선택</div>
            
            <input type="hidden" id="modalDateInput">
            
            <div class="calendar-container">
                <div id="modal-inline-calendar"></div>
            </div>
        </div>
      
    `;
    openModal("방문 예약", modalContent, {
        confirmText: '예약 확정',
        cancelText: '취소',
        onConfirm: () => {

            const dateValue = document.getElementById('modalDateInput').value;

            if (!dateValue) {
                setTimeout(() => {
                    openModal("알림", "<p>방문 날짜와 시간을 선택해야 합니다.</p>", {
                        confirmText: '확인',
                        onConfirm: () => reserveItem() // 다시 예약창 띄우기
                    });
                }, 200);
                return;
            }

            sendReservationRequest(shopId, itemId, currentSelectedSize, dateValue);
        },
        onCancel: () => {
        }
    });

    setTimeout(() => {
        flatpickr("#modal-inline-calendar", {
            inline: true,             // 인라인 모드 (달력이 바로 보임)
            enableTime: true,         // 시간 선택 활성화
            dateFormat: "Y-m-d H:i",  // 저장될 포맷
            minDate: "today",         // 오늘 이전 선택 불가
            locale: "ko",             // 한국어
            minuteIncrement: 30,      // 30분 단위
            onChange: function(selectedDates, dateStr, instance) {
                // 달력 선택 시 hidden input에 값 매핑
                const input = document.getElementById('modalDateInput');
                if(input) input.value = dateStr;
            }
        });
    }, 0);
};

// [추가] 예약 데이터 전송 함수 (AJAX)
const sendReservationRequest = (shopId, itemId, size, dateStr) => {
    const dataObj = {
        shopId: Number(shopId),
        visitDate: dateStr.replace(' ', 'T'),
        items: [
            {
                itemId: Number(itemId),
                size: size
            }
        ]
    };

    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/reservation/post-items');
    xhr.setRequestHeader('Content-Type', 'application/json');

    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) return;

        if (xhr.status >= 200 && xhr.status < 400) {
            const response = JSON.parse(xhr.responseText);
            if (response.result === 'SUCCESS') {
                openModal("예약 성공", `<p>예약이 확정되었습니다.<br>마이페이지에서 내역을 확인하세요.</p>`, {
                    confirmText: '확인',
                    onConfirm: () => { location.href = '/my?open=reservation'; }
                });
            } else if (response.result === 'FAILURE_SESSION') {
                openModal("로그인 필요", `<p>로그인이 필요한 서비스입니다.</p>`, {
                    confirmText: '로그인',
                    onConfirm: () => { location.href = '/login'; }
                });
            } else {
                openModal("오류", `<p>예약 실패: ${response.message || '다시 시도해주세요.'}</p>`, { confirmText: '확인' });
            }
        } else {
            openModal("통신 오류", `<p>서버와 통신 중 오류가 발생했습니다.</p>`, { confirmText: '확인' });
        }
    };
    xhr.send(JSON.stringify(dataObj));
};