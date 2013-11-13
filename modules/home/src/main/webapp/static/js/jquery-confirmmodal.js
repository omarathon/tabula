(function ($) { "use strict";

/*
	Used on forms. Blocks the form from submitting and displays a confirmation message in a modal. Clicking confirm
	submits the form.

	Options:
		message - the message to display in the modal.

*/

	jQuery.fn.confirmModal = function(options) {
		var $ = jQuery;
		this.each(function() {
			var $form = $(this);
			if($form.is('form')) {
				var $submitButtons = $form.find("input[type='submit']");

				if (options == false) {
					$(document).off('click.confirmModal');
				} else {
					var message = options.message || 'Are you sure?',
						confirm = options.confirm || 'Confirm',
						cancel = options.cancel || 'Cancel',
						confirmPrimary = (options.primary && !(options.primary != 'confirm')) || true,
						confirmClass = 'confirm btn' + confirmPrimary?' btn-primary':'',
						cancelClass = 'btn' + confirmPrimary?'':' btn-primary',
						modalHtml = "<div class='modal hide fade' id='confirmModal'>" +
										"<div class='modal-body'>" +
											"<h5>" + message + "</h5>" +
										"</div>" +
										"<div class='modal-footer'>" +
											"<a class='" + confirmClass + "'>" + confirm + "</a>" +
											"<a data-dismiss='modal' class='" + cancelClass + "'>" + cancel + "</a>" +
										"</div>" +
									"</div>"
					var $modal = $(modalHtml);

					$(document).on('click.confirmModal', '#confirmModal a.confirm', function() {
						$form.submit();
					});

					$(document).on('click.confirmModal', $submitButtons, function() {
						$modal.modal();
						return false;
					});
				}
			}
		});
		return this;
	}

})(jQuery);