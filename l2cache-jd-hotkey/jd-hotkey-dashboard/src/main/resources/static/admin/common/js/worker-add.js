$("#form-add").validate({
	rules:{
		ip:{
			required:true,
		},
		port:{
			required:true,
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
		url : "/worker/add",
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

