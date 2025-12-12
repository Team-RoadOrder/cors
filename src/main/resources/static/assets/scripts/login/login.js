/**@type{HTMLFormElement}*/
$loginForm = document.getElementById('loginForm');

$loginForm.addEventListener('submit', (e)=> {
    e.preventDefault();

    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('email', $loginForm['email'].value);
    formData.append('password', $loginForm['password'].value);
    xhr.onreadystatechange = () => {
        // 여기에 이메일이나 비밀번호 필터링 로직작성

        if (xhr.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (xhr.status < 200 || xhr.status >= 400) {
            alert('요청을 전송하는 도중 오류가 발생하였습니다. 잠시 후 다시 시도해 주세요.');

            return;
        }
        const response = JSON.parse(xhr.responseText);
        console.log(response);
        if( response.status === 'SUCCESS' && response.usertype === 'customer' ) {
            alert('로그인성공')

            window.location.href = '/';
        } else if(response.status === 'SUCCESS' && response.usertype === 'owner'){
            alert('로그인성공')
            window.location.href = '/owner';
        } else {
            const title ="Error";
            const content = `<p>로그인에 실패하였습니다. 잠시 후 다시 시도해 주세요.</p>`;
            const options = {
                confirmText: '확인',
                onConfirm: () => {

                }
            }
            openModal(title,content,options);
        }

     };
     xhr.open('POST', '/login')
     xhr.send(formData);

})
// 모달창 수정