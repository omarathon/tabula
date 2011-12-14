jQuery(function($){
	
	$('input.date-time-picker').AnyTime_picker({
		format: "%e-%b-%Y %H:%i:%s",
		firstDOW: 1
	});
	
	$('a.long-running').click(function(event){
		
	});
	
})