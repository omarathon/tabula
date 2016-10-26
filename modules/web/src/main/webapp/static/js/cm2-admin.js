(function ($) { "use strict";

	$.fn.bindFormHelpers = function() {
		var $this = $(this);

		// bind form helpers
		$this.find('input.date-time-picker').tabulaDateTimePicker();
		$this.find('input.date-picker').tabulaDatePicker();
		$this.find('input.time-picker').tabulaTimePicker();
		$this.find('input.date-time-minute-picker').tabulaDateTimeMinutePicker();
		$this.find('form.double-submit-protection').tabulaSubmitOnce();
	};

	$(function(){

		var $body = $('body');

		// expandable table rows
		$body.on('show.bs.collapse', '.detail-row', function(e){
			var $this = $(e.target);
			var hasLoaded = $this.data('loaded') || false;
			if(!hasLoaded) {
				var detailUrl = $this.data('detailurl');
				$this.data('request', $.get(detailUrl, function(data) {
					$this.find('td').html(data);
					// bind form helpers
					$this.bindFormHelpers();
					$this.data('loaded', true);
				}));
			}
		});

		// extension managemet - on submit replace html with validation errors or redirect
		$body.on('submit', 'form.modify-extension', function(e){
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
					$detailRow.bindFormHelpers();
				}
			});
		});
	});


})(jQuery);