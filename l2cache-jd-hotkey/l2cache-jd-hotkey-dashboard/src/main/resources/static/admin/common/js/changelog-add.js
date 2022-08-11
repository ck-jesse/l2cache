$("#form-add").validate({

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
		url : "/changeLog/add",
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

