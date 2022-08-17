$.ajaxSetup({
    contentType:"application/x-www-form-urlencoded;charset=utf-8",
    complete:function(XMLHttpRequest,textStatus){
        var token = getCookie("token");
        if(XMLHttpRequest.status == 1000 && ( token == "undefined" || token =="")){
            top.location.href = '/user/login';
        }
    }
});