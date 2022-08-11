/*!
 * jQuery Message Plugin (with Transition Definitions)
 * Examples and documentation at: http://json.cn/
 * Copyright (c) 2012-2017  Paul.Ren.
 * Version: 1.0.2 (19-OCT-2017)
 * Dual licensed under the MIT and GPL licenses.
 * http://jquery.malsup.com/license.html
 * Requires: jQuery v1.3.1 or later
 */
 var container=$('#jquery-beauty-msg');
    if(container.length<=0){
        $("body").append('<div style="clear:both;"></div><div id="jquery-beauty-msg"></div>');
        container=$('#jquery-beauty-msg');
    }
    var containerStyle='color:#e1282b;font-family:"微软雅黑";font-weight:bold;font-size:20px;text-shadow:5px 5px 10px #eee;'
            +'text-align:center;margin:0;padding-top:15%;width:100%;word-break:break-all;z-index:100000;';
var closeFlag=false;
var timer=0;
var msgContent='';
$.msg=function(txt,style,obj,delay){
    msgContent=txt;

    if(obj!="undefined"&&obj!=null){
        containerStyle+='position:relative;top:'+$(obj).attr('top')+';left:'+$(obj).attr('left')+';';
    }
    else{
        containerStyle+='position:fixed;top:0;left:0;';
        $(container).attr('style',containerStyle+style);
        $(container).html(msgContent);
        $(container).fadeIn(300,function(){
            $(container).animate({fontSize:'40px'},'300');
            $(container).delay(1000).fadeOut();
        });
    }
}
function addDot(){
    msgContent=msgContent+".";
    $(container).html(msgContent);
    timer=timer+1;
    if(!closeFlag&&timer>=5){
        $(container).html("操作超时！");
        window.clearInterval();
    }
}
$.loading=function(txt,action){
        msgContent=txt;
        containerStyle+='position:fixed;top:0;left:0;';
        $(container).attr('style',containerStyle+"color:blue;");
        $(container).html(msgContent);
        if(action!="close"){
            $(container).fadeIn(300,function(){
                $(container).animate({fontSize:'40px'},'300');
            });
            window.setInterval("addDot",1000);

        }else{
            window.clearInterval();
            closeFlag=true;
            $(container).fadeOut();
        }
}
