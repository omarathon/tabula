jQuery(function($) {
	'use strict';

	var exports = window;

	function Activity(item) {
		this.item = item;
	}

	Activity.prototype.render = function() {
		var item = this.item;
		var date = new Date(item.published);
		var priority = 'alert-info';
		if (item.priority >= 0.5) priority = 'alert-warning';
		if (item.priority >= 0.75) priority = 'alert-danger';

		var now = moment();
		var time = moment.utc(item.published);
		var $timestamp = $('<div>', {'class':'timestamp pull-right'}).html(toTimestamp(now, time));

		return $('<div>', {'class': 'activity alert ' + priority})
			.append($('<button>', {'class':'close', title: 'Dismiss'}).html('&times;'))
			//.append($('<a>', {'class': 'url', href:item.url})
				.append($('<h4>', {'class': 'title'}).html(item.title))
			//)
//			.append($('<div>', {'class': 'date'}).html(date.toString()))
			.append($('<div>', {'class': 'content'}).html(item.content))
			.append($timestamp)
			.append($('<p>', {'class': 'url'}).append(
				$('<a></a>', {'href': item.url}).html('Further info')
			));
	}

	// Date stuff
	function toTimestamp(now, then) {
		if (now.diff(then) < 60000) { // less than a minute ago
			return then.from(now);
		} else if (now.isSame(then, 'day')) {
			return then.format('H:mma');
		} else {
			return then.format('LL H:mma');
		}
	}

	// Set up a rendered activity stream in the given container.
	// The contents will be cleared.
	function initStream($container, options) {
		var options = options || {};
		$container.html('');

		if ($container.data('activity-stream-init')) {
			return;
		}
		$container.data('activity-stream-init', true);

		var $moreLink = $('<div>', {'class':'more-link'}).append(
				$('<a>', {href:'#'}).html('More&hellip;')
		);

		function loadPage(pagination, first) {
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

				if (data.pagination && data.pagination.token) {
					$container.append($moreLink);
					if (data.items.length) {
						$moreLink.off('click');
						$moreLink.on('click', 'a', function() {
							loadPage(data.pagination);
							return false;
						})
					} else {
						// this page was empty but there's another page, so
						// get that straight away. (can happen if all notifications related
						// to obsolete objects so aren't returned)
						loadPage(data.pagination, first);
					}
				} else {
					var noMoreMsg = first ? 'No activity to show.' : 'No more activity to show.';
					$moreLink.after(noMoreMsg);
					$moreLink.remove(); // il n'y a pas de items
				}
			});
		}

		loadPage(null, true);
	}

	function autoInit() {
		$('.activity-stream').each(function(i, container) {
			var $container = $(container);
			var options = {};
			options.max = $container.data('max');
			// TODO more options
			initStream($container, options);
		});
	}

	exports.ActivityStreams = {
		//Activity: Activity,
		initStream: initStream,
		autoInit: autoInit
	};

	$(function(){
		autoInit();
	});

});