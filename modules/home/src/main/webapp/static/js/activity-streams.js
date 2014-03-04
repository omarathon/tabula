jQuery(function($) {
	'use strict';

	var exports = window;

	function Activity(item) {
		this.item = item;
	}

	Activity.prototype.render = function() {
		var item = this.item;
		var date = new Date(item.published)
		return $('<div>', {className: 'activity'})
			.append($('<h5>', {className: 'title'}).html(item.title))
			.append($('<div>', {className: 'date'}).html(date.toString()))
			.append($('<div>', {className: 'content'}).html(item.content));
	}

	// Set up a rendered activity stream in the given container.
	// The contents will be cleared.
	function initStream($container, options) {
		var options = options || {};
		$container.html('');

		var $moreLink = $('<a>', {href:'#', className:'more-link'}).html('More&hellip;');

		function loadPage(pagination) {
			var data = jQuery.extend({}, options);
			var url = '/activity/@me';
			if (pagination) {
				data.token = pagination.token;
				data.last = pagination.field;
				data.lastDoc = pagination.doc;
			}
			jQuery.get(url, data).then(function(data) {
				$(data.items).each(function(i, item) {
					var activity = new Activity(item);
					activity.render().appendTo($container);
				});
				console.log(data);
				if (data.pagination && data.pagination.token) {
					$container.append($moreLink);
					$moreLink.off('click');
					$moreLink.on('click', function() {
						loadPage(data.pagination);
						return false;
					})
				} else {
					$moreLink.remove(); // il n'y a pas de items
				}
			});
		}

		loadPage();
	}

	exports.ActivityStreams = {
		//Activity: Activity,
		initStream: initStream
	};

});