$("#form-edit").validate({

	submitHandler : function(form) {
		edit();
	}
});

function edit() {
	var dataFormJson = $("#form-edit").serialize();
	$.ajax({
		cache : true,
		type : "POST",
		url : "/rule/edit",
		data : dataFormJson,
		headers: {
			"Authorization":getCookie("token")
		},
		async : false,
		error : function(XMLHttpRequest) {
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
