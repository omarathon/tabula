(function ($) { "use strict";

/*
	Used on forms. Blocks the form from submitting and displays a confirmation message in a modal. Clicking confirm
	submits the form.

	Options:
		message - the message to display in the modal.

*/

	jQuery.fn.confirmModal = function(options) {
		var $ = jQuery;
		this.each(function(){
			var $form = $(this);
			if($form.is('form')){
				var message = options.message  || 'Are you sure?';
				var modalHtml = "<div class='modal hide fade' id='confirmModal'>" +
									"<div class='modal-body'>" +
										"<h5>"+message+"</h5>" +
									"</div>" +
									"<div class='modal-footer'>" +
										"<a class='confirm btn btn-primary'>Confirm</a>" +
										"<a data-dismiss='modal' class='btn'>Cancel</a>" +
									"</div>" +
								"</div>"
				var $modal = $(modalHtml);

				$('a.confirm', $modal).on('click', function(){
					$form.submit();
				});

				var $submitButtons = $form.find("input[type='submit']");
				$submitButtons.on('click', function(){
					$modal.modal();
					return false;
				});
			}
		});
		return this;
	}

})(jQuery);