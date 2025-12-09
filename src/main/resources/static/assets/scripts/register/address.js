$dialog = document.getElementById("dialog");
$modal = $dialog.querySelector(":scope >.modal");
$close = $dialog.querySelector(":scope >.close");
$buttonCustomer = document.getElementById("customer-button");
$buttonOwner = document.getElementById("owner-button");
function loadAddress() {
    new daum.Postcode({
        oncomplete: function(data) {
            let addr = '';
            if (data.userSelectedType === 'R') {
                addr = data.roadAddress;
            } else {
                addr = data.jibunAddress;
            }
            document.getElementById("address_customer").value = addr;
            document.getElementById("address_customer_detail").focus();
            $dialog.style.display='none';
        },
        width : '100%',
        height : '100%'
    }).embed($modal);
    $dialog.style.display='block';
}
function loadAddress2() {
    new daum.Postcode({
        oncomplete: function(data) {
            let addr = '';
            if (data.userSelectedType === 'R') {
                addr = data.roadAddress;
            } else {
                addr = data.jibunAddress;
            }
            document.getElementById("address_owner").value = addr;
            document.getElementById("address_owner_detail").focus();
            $dialog.style.display='none';
        },
        width : '100%',
        height : '100%'
    }).embed($modal);
    $dialog.style.display='block';
}

$buttonCustomer.addEventListener("click", ()=> {
    loadAddress()
})
$buttonOwner.addEventListener("click", ()=>{
    loadAddress2()
})
$close.addEventListener('click', ()=> {
    $dialog.style.display='none';
})
