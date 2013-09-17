(function($) {

		// toggle checkboxes depending on value of "select all" type checkbox
		jQuery.fn.selectDeselectCheckboxes = function(selectAllCheckbox) {

			var selectOrDeselect = $(selectAllCheckbox).prop('checked');
			var selectAllCheckboxes =  $("." + $(selectAllCheckbox).attr('class'))  ;
			selectAllCheckboxes.attr("checked", selectOrDeselect);

			var allCheckboxes = $(this).find('[type=checkbox]');
			allCheckboxes.attr("checked", $(selectAllCheckbox).prop('checked'));
		}

})(jQuery);