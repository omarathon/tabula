(function ($) {
	'use strict';

$.fn.enableFilters = function(options) {

	options = options || {};

	var resultSelector = options.resultSelector || '.filter-results';

	this.each(function() {
		var $this = $(this);
		var $form = $this.find('form');
		var $results = $(resultSelector);
		var $clearAll = $this.find('.clear-all');

		// bind event handlers
		$this.on('change', function(e) {
			var $checkbox = $(e.target);
			doRequest();
			updateFilterText($checkbox);
		});

		$clearAll.on('click', function(){
			$this.find('input:checked').each(function() {
				var $checkbox = $(this);
				$checkbox.prop('checked', false);
				updateFilterText($checkbox);
			});
			doRequest();
		});
		toggleClearAll();

		function doRequest() {
			// update the url with the new filter values
			if (typeof history.pushState !== 'undefined') {
				history.pushState(null, document.title, $form.attr('action') + '?' + $form.serialize());
			}

			// abort any currently running requests
			if ($form.data('request')) {
				$form.data('request').abort();
				$form.data('request', null);
			}

			// grey out results while loading
			$results.addClass('loading');

			$form.data('request', $.post($form.attr('action'), $form.serialize(), function(data) {
				$results.html(data);
				$form.data('request', null);
				$results.removeClass('loading');
			}));
			toggleClearAll();
		}

		function toggleClearAll() {
			$clearAll.prop('disabled', ($this.find('input:checked').size() === 0));
		}

		function updateFilterText($checkbox) {
			var $filter = $checkbox.closest('.filter');
			var shortValues = $filter.find('input:checked').map(function() { return $(this).data('short-value'); }).get();
			var $fsv = $filter.find('.filter-short-values');
			if (shortValues.length) {
				$filter.removeClass('empty');
				$fsv.html($fsv.data("prefix") + shortValues.join(', '));
			} else {
				$filter.addClass('empty');
				$fsv.html($fsv.data('placeholder'));
			}
		}

	});
};

$(function(){
	$('.filters').enableFilters();

	$('body').on('show.bs.collapse', function(e){
		var $this = $(e.target);
		var hasLoaded = $this.data('loaded') || false;
		if(!hasLoaded) {
			var detailUrl = $this.data('detailurl');
			$this.data('request', $.get(detailUrl, function(data) {
				$this.find('td').html(data);

				// bind form helpers
				$this.find('input.date-time-picker').tabulaDateTimePicker();
				$this.find('input.date-picker').tabulaDatePicker();
				$this.find('input.time-picker').tabulaTimePicker();
				$this.find('input.date-time-minute-picker').tabulaDateTimeMinutePicker();
				$this.find('form.double-submit-protection').tabulaSubmitOnce();

				$this.data('loaded', true);
			}));
		}
	});

	// handle validation for embedded extension update forms
	$('body').on('submit', 'form.modify-extension', function(e){
		e.preventDefault();
		var $form = $(e.target);
		var $detailRow = $form.closest('tr.detail-row').find('td');

		var formData = $form.serializeArray();
		var $buttonClicked =  $(document.activeElement);
		formData.push({ name: $buttonClicked.attr('name'), value: $buttonClicked.val() });

		$.post($form.attr('action'), formData, function(data) {
			if (data.success) {
				window.location.replace(data.redirect);
			} else {
				$detailRow.html(data);
			}
		});
	});
});

})(jQuery);