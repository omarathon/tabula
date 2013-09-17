(function($) {

		// toggle checkboxes depending on value of "select all" type checkbox
		jQuery.fn.selectDeselectCheckboxes = function(selectAllCheckbox) {
			var allCheckboxes = $(this).find('[type=checkbox]');
			allCheckboxes.attr("checked", $(selectAllCheckbox).prop('checked'));
		}

})(jQuery);