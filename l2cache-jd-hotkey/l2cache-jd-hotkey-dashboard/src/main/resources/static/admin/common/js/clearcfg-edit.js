$("#form-edit").validate({
    submitHandler:function(form){
        edit();
    }
});

function edit() {
    var dataFormJson=$("#form-edit").serialize();
    $.ajax({
        cache : true,
        type : "POST",
        url : "/clear/save",
        data : dataFormJson,
        headers: {
            "Authorization":getCookie("token")
        },
        async : false,
        error : function(XMLHttpRequest){
            $.modal.alertError(XMLHttpRequest.responseJSON.msg);
        },
        success : function(data) {
            $.operate.saveSuccess(data);
        }
    });
}
