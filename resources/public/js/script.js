function googleEvent(radioObj, category, action) {
    var radioLength = radioObj.length;
    for(var i = 0; i < radioLength; i++) {
	if(radioObj[i].checked) {
	    _gaq.push(['_trackEvent',category,action,(radioObj[i].value),i+1]);
	}
    }
}

function qinit () {
    for (var a=document.querySelectorAll('form'),i=0,len=a.length;i<len;++i){
	(function(form) {
	    var radioLength = form.length;
	    for(var i = 0; i < radioLength; i++) {
		if(form[i].checked) {
		    form.style.display = 'none';
		}
	    }
	})(a[i]);
    }
}