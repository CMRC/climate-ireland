function googleEvent(radioObj, category, action) {
    var radioLength = radioObj.length;
    for(var i = 0; i < radioLength; i++) {
	if(radioObj[i].checked) {
	    _gaq.push(['_trackEvent',category,action,(radioObj[i].value),i+1]);
	}
    }
}