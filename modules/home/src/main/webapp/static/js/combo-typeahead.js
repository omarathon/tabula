(function($) { 'use strict';

	/**
	 * Automatically convert a <select> element to a Bootstrap Typeahead by annotating it with
	 * data-provide="typeahead".
	 *
	 * @see http://getbootstrap.com/2.3.2/javascript.html#typeahead
	 */
	$.fn.comboTypeahead = function(options) {
		this.each(function() {
			var $this = $(this);

			// Defaults
			var allOptions = {
				items: 8,
				minLength: 1
			};

			$.extend(allOptions, options || {});

			var currentValue = $this.find(':selected').text().trim() || '';

			var items =
				$this.find('option')
				 	.filter(function() { return $(this).val(); })
					.map(function() {
						return $(this).text().trim();
					})
					.get();

			// Create the typeahead element
			var $input = $('<input />', {
				'type': 'text',
				'class': $this.attr('class'),
				'style': $this.attr('style'),
				'placeholder': $this.attr('placeholder'),
				'value': currentValue
			})
				.attr("autocomplete", "off")
				.typeahead({
					source: items,
					items: allOptions.items,
					minLength: allOptions.minLength,
					updater: function(item) {
						$this.find(':selected').prop('selected', false);

						$this.find('option')
							.filter(function() {
								return $(this).val() && $(this).text().trim() == item;
							})
							.prop('selected', true);

						$this.trigger('change');

						return item;
					}
				})
				.on('keyup', function(e) {
					if ($.inArray(e.keyCode, [40,38,16,17,18,9,13,27]) == -1) {
						$this.find(':selected').prop('selected', false);
						$this.trigger('change');
					}
				});

			$this.hide().after($input);
		});
		return this;
	};

	$(function() {
		$('select[data-provide="typeahead"]').comboTypeahead();
	});
})(jQuery);