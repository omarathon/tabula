/**
 * Scripts used only by student profiles.
 */
(function ($) { "use strict";

	var exports = {};

	$(function() {
		$('.profile-search').each(function() {
			var container = $(this);

			var target = container.find('form').prop('action') + '.json';

			var xhr = null;
			container.find('input[name="query"]').prop('autocomplete','off').each(function() {
				var $spinner = $('<div class="spinner-container" />');
				$(this).before($spinner);

				$(this).typeahead({
					source: function(query, process) {
						if (xhr != null) {
							xhr.abort();
							xhr = null;
						}

						query = $.trim(query);
						if (query.length < 3) { process([]); return; }

						// At least one of the search terms must have more than 1 character
						var terms = query.split(/\s+/g);
						if ($.grep(terms, function(term) { return term.length > 1; }).length == 0) {
							process([]); return;
						}

						$spinner.spin('small');
						xhr = $.get(target, { query : query }, function(data) {
							$spinner.spin(false);

							var members = [];

							$.each(data, function(i, member) {
								var item = member.name + "|" + member.id + "|" + member.userId + "|" + member.description;
								members.push(item);
							});

							process(members);
						}).error(function(jqXHR, textStatus, errorThrown) { if (textStatus != "abort") $spinner.spin(false); });
					},

					matcher: function(item) { return true; },
					sorter: function(items) { return items; }, // use 'as-returned' sort
					highlighter: function(item) {
						var member = item.split("|");
						return '<img src="/profiles/view/photo/' + member[1] + '.jpg" class="photo pull-right"><h3 class="name">' + member[0] + '</h3><div class="description">' + member[3] + '</div>';
					},

					updater: function(item) {
						var member = item.split("|");
						window.location = '/profiles/view/' + member[1];

						return member[0];
					},
					minLength:3
				});
			});
		});
	});

	// take anything we've attached to "exports" and add it to the global "Profiles"
	// we use extend() to add to any existing variable rather than clobber it
	window.Profiles = jQuery.extend(window.Profiles, exports);

	// MEETING RECORD STUFF
	$(function() {

		// delete meeting records
		$('section.meetings').on('click','a.delete-meeting-record', function(e) {
			e.preventDefault();
			var $this = $(this);
			var url = $this.attr("href");
			var $summary = $this.closest('summary');

			$.post(url, function(data) {
				if (data.status == "successful") {
					$summary.addClass("deleted");
				}
			}, "json");
		});
	});

}(jQuery));