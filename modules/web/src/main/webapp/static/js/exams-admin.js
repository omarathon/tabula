/**
 * Scripts used only by the exams admin section.
 */
(function ($) { "use strict";

var exports = {};

exports.zebraStripeExams = function($module) {
	$module.find('.assignment-info').filter(':visible:even').addClass('alt-row');
};

exports.prepareAjaxForm = function($form, callback) {
	var $container = $form.closest('.content-container');
	var contentId = $container.attr('data-contentid');
	var $row = $('tr.item-container[data-contentid='+contentId+']');
	$form.ajaxForm({
		iframe: true,
		statusCode: {
			403: function(jqXHR) {
				$container.html("<p>Sorry, you don't have permission to see that. Have you signed out of Tabula?</p><p>Refresh the page and try again. If it remains a problem, please let us know using the 'Problems, questions?' button at the top of the page.</p>");
				$container.trigger('tabula.expandingTable.contentChanged');
			}
		},
		success: function(response) {
			var result = callback(response);

			if (!result || /^\s*$/.test(result)) {
				// reset if empty
				$row.trigger('tabula.expandingTable.toggle');
				$container.html("<p>No data is currently available.</p>");
				$row.find('.status-col dt .unsaved').remove();
				$container.removeData('loaded');
			} else {
				$container.html(result);
				$container.trigger('tabula.expandingTable.contentChanged');
			}
		},
		error: function(){alert("There has been an error. Please reload and try again.");}
	});
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