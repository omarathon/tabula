/**
 * Scripts used only by the exams admin section.
 */
(function ($) { "use strict";

var exports = {};

exports.zebraStripeExams = function($module) {
	$module.find('.assignment-info').filter(':visible:even').addClass('alt-row');
};

window.Exams = jQuery.extend(window.Exams, exports);

$(function(){

	// code for tabs
	$('.nav.nav-tabs a').click(function (e) {
		e.preventDefault();
		$(this).tab('show');
	});

	/*** Admin home page ***/
	$('.dept-show').click(function(event){
		event.preventDefault();
		var hideButton = $(this).find("a");

		$('.module-info.empty').toggle('fast', function() {
			if($('.module-info.empty').is(":visible")) {
				hideButton.html('<i class="icon-eye-close"></i> Hide');
				hideButton.attr("data-original-title", hideButton.attr("data-title-hide"));

			} else {
				hideButton.html('<i class="icon-eye-open"></i> Show');
				hideButton.attr("data-original-title", hideButton.attr("data-title-show"));
			}
		});

	});

})

}(jQuery));