/**
 * Scripts used only by the exams admin section.
 */
(function ($) { "use strict";

var exports = {};

$(function(){

	// code for tabs
	$('.nav.nav-tabs a').click(function (e) {
		e.preventDefault();
		$(this).tab('show');
	});

})

}(jQuery));