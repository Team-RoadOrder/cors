
/** @type {HTMLFormElement}*/
const loginForm = document.getElementById('loginForm');
/** @type {HTMLDivElement}*/
const caption = document.querySelector('.caption');
/** @type {HTMLDivElement}*/
const passwordResult = document.getElementById('passwordResult');
loginForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('email', loginForm['email'].value)
    xhr.onreadystatechange = () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            const title = "경고";
            const content = `
            <p>요청을 전송하는 도중 오류가 발생하였습니다. 잠시 후 다시 시도해 주세요.</p>
        `;

            const options = {
                confirmText: '확인',
                onConfirm: () => {
                }
            };

            openModal(title, content, options);
            return;
        }
        const response = JSON.parse(xhr.responseText);
        console.log(response);
        if( response.status === 'SUCCESS' ) {
            const title = "비밀번호 찾기 성공";
            const content = `
            <p>비밀번호 찾기에 성공하였습니다.</p>
        `;
            const options = {
                confirmText: '확인',
                onConfirm: () => {
                }
            };
            openModal(title, content, options);
            caption.classList.add('show');
            passwordResult.innerHTML = `비밀번호는 ${response.password}입니다.`;
        } else {
            const title = "비밀번호 찾기 실패";
            const content = `
            <p>비밀번호 찾기에 실패하였습니다.</p>
        `;
            const options = {
                confirmText: '확인',
                onConfirm: () => {
                }
            };
            openModal(title, content, options);
            caption.classList.add('show');
            passwordResult.innerHTML = '';
        }

    };
    xhr.open('POST', '/findpw')
    xhr.send(formData);

})

