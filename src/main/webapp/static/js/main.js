
jQuery(function($){
	
	$('input.date-time-picker').AnyTime_picker({
		format: "%e-%b-%Y %H:%i:%s",
		firstDOW: 1
	});
	
	$('a.long-running').click(function(event){
		
	});
	
});

var Forms = {};
Forms.DeletePermission = function(moduleCode, userId) {
	if (confirm('Are you sure that you want to remove permission from ' + userId + "?")) {
		
	} else {
		
	}
}
