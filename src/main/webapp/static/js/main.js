(function () { "use strict";

window.Supports = {};
window.Supports.multipleFiles = !!('multiple' in (document.createElement('input')));

jQuery(function ($) { 
	
	// A popup box enhanced with a few methods for user picker
	var userPicker = new WPopupBox({imageroot:'/static/js/popup/'});
	userPicker.show = function show (element) {
		this.setSize(400,400);
		$.get('/api/userpicker/form', function (data) {
			userPicker.setContent(data);
			userPicker.decorateForm();
		});
		this.show();
	};
	userPicker.decorateForm = function () {
		var $contents = $(this.contentElement),
			$firstname = $contents.find('.userpicker-firstname'),
			$lastname  = $contents.find('.userpicker-lastname')
			$results = $contents.find('.userpicker-results');
		$contents.find('input').delayedObserver(function () {
			$.get('/api/userpicker/form', function (data) {
				var $table = $('<table>');
				$table.append('<tr></tr>')
			});
		}, 0.5);
	};
	
	$('input.date-time-picker').AnyTime_picker({
		format: "%e-%b-%Y %H:%i:%s",
		firstDOW: 1
	});
	
	$('a.long-running').click(function (event) {
		
	});
	
	$('input.user-code-picker').each(function (picker) {
		var $picker = $(picker),
			$button = $('<button>Pick</button>');
		$picker.after($button).hide();
		$button.click(function(event){
			
		});
	});
	
	$('input.uni-id-picker').each(function (picker) {
		
	});
	

}); // end domready

}());