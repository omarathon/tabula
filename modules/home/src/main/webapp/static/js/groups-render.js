/**
* Scripts used only by the SGT render/admin section.
*/
(function ($) { "use strict";

var exports = {};

exports.wireMapLocations = function($container) {
	$container.find('.map-location[data-lid]').each(function() {
		var $el = $(this);

		var mapUrl = '//campus.warwick.ac.uk/?lite=1&search=' + encodeURIComponent($el.text()) + '&lid=' + encodeURIComponent($el.data('lid'));

		var $icon =
			$('<i />').addClass('icon-map-marker');

		var $a = $('<a />').attr('href', '#');

		$el.append('&nbsp;').append($a.append($icon));

		$a.tabulaPopover({
			trigger: 'click',
			container: '#container',
			template: '<div class="popover wide"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>',
			html: true,
			content: '<iframe width="300" height="400" frameborder="0" src="' + mapUrl + '"></iframe>'
		});
	});
};

// take anything we've attached to "exports" and add it to the global "Groups"
// we use extend() to add to any existing variable rather than clobber it
window.Groups = jQuery.extend(window.Groups, exports);

$(function() {
	exports.wireMapLocations($('#container'));
});

}(jQuery));

