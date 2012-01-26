(function () { "use strict";

window.Supports = {};
window.Supports.multipleFiles = !!('multiple' in (document.createElement('input')));

jQuery(function ($) {
	
	function _dbg(msg) {
		if (window.console && console.debug) console.debug(msg);
	}
	var dbg = (window.console && console.debug) ? _dbg : function(){};
	
	
	// Lazily loaded user picker object.
	var _userPicker = null;
	function getUserPicker() {
		if (_userPicker == null) {
			// A popup box enhanced with a few methods for user picker
			_userPicker = new WPopupBox({imageroot:'/static/libs/popup/'});
			_userPicker.show = function show (element) {
				this.setSize(400,400);
				$.get('/api/userpicker/form', function (data) {
					userPicker.setContent(data);
					userPicker.decorateForm();
				});
				this.show();
			};
			_userPicker.decorateForm = function () {
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
		}
		return _userPicker;
	}
	
	
	
	$('input.date-time-picker').AnyTime_picker({
		format: "%e-%b-%Y %H:%i:%s",
		firstDOW: 1
	});
	
	$('a.long-running').click(function (event) {
		
	});
	
	$('input.user-code-picker').each(function (i, picker) {
		dbg("found a picker");
		var $picker = $(picker),
			$button = $('<button>Search for user</button>'),
			userPicker = getUserPicker();
		$picker.after($button).hide();
		$button.click(function(event){
			event.preventDefault();
			userPicker.show();
			userPicker.positionBelow(event.target)
		});
	});
	
	$('input.uni-id-picker').each(function (picker) {
		
	});
	

}); // end domready

}());