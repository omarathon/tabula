(function ($) { "use strict";

/*
	Used on a form. A bunch of radio inputs are added to the label of a set of form elements. All elements with
	unselected radio buttons are disabled. The radio buttons must all share a name attribute of 'disable-radio'

	Options:
		onDisable: A function that is called when an input is disabled. The input itself is passed as a parameter
		onEnable: A function that is called when an input is enabled. The input itself is passed as a parameter
*/

	jQuery.fn.radioDisable = function(options) {
		var $ = jQuery;
		this.each(function() {
			var $form = $(this);
			if($form.is('form')) {

				var doNothing = function(){};
				var onDisable = options.onDisable || doNothing;
				var onEnable = options.onEnable || doNothing;

				var $radioButtons = $('input[name=disable-radio][type=radio]', $form);

				$radioButtons.on('change', function() {
					$radioButtons.each(function() {
						var $this = $(this);
						var $controlGroup = $(this).closest('div.control-group');
						var $inputs = $('input[name!=disable-radio]', $controlGroup)
						
						if ($this.is(':checked')) {
							$inputs.each(function(){
								$(this).removeAttr('readonly');
								onEnable($(this));
							});
						} else {
							$inputs.each(function(){
								$(this).attr('readonly', 'readonly');
								onDisable($(this));
							});
						}
					});
				});
				
				$radioButtons.each(function() {
					var $this = $(this);
					var $controlGroup = $(this).closest('div.control-group');
					var $inputs = $('input[name!=disable-radio]', $controlGroup)
					
					if ($this.is(':checked')) {
						$inputs.removeAttr('readonly');
					} else {
						$inputs.attr('readonly', 'readonly');
					}
				});
			}
		});
		return this;
	};

})(jQuery);