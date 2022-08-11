$("#form-add").validate({
	rules:{
		pwd:{
			required:true,
			minlength: 5,
			maxlength: 20
		},
	},
	messages: {
        "userName": {
            remote: "用户已经存在"
        }
    },
	submitHandler:function(form){
		add();
	}
});

/**
 *
 */
function add() {
	var dataFormJson=$("#form-add").serialize();
	$.ajax({
		cache : true,
		type : "POST",
		url : "/user/add",
		data : dataFormJson,
		headers: {
			"Authorization":getCookie("token")
		},
		async : false,
		error : function(XMLHttpRequest){
			$.modal.alertError(XMLHttpRequest.responseJSON.msg);
			var token = getCookie("token");
			if(XMLHttpRequest.status == 1000 && ( token == "undefined" || token =="")){
				top.location.href = '/user/login';
			}
		},
		success : function(data) {
			$.operate.saveSuccess(data);
		}
	});
}

