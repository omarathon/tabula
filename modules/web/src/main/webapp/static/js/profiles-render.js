/**
 * Scripts used only by student profiles.
 */
(function ($) { 'use strict';

	var exports = {};

	$(function() {
		$('.profile-search').each(function() {
			var container = $(this);

			var target = container.find('form').prop('action') + '.json';

			var xhr = null;
			container.find('input[name="query"]').prop('autocomplete','off').each(function() {
				var $spinner = $('<div class="spinner-container" />'), $this = $(this);
				$this
					.before($spinner)
					.on('focus', function(){
						container.find('.use-tooltip').tooltip('show');
					})
					.on('blur', function(){
						container.find('.use-tooltip').tooltip('hide');
					})
					.bootstrap3Typeahead({
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
									var item = member.name + '|' + member.id + '|' + member.userId + '|' + member.description;
									members.push(item);
								});

								process(members);
							}).error(function(jqXHR, textStatus, errorThrown) { if (textStatus != 'abort') $spinner.spin(false); });
						},

						matcher: function(item) { return true; },
						sorter: function(items) { return items; }, // use 'as-returned' sort
						highlighter: function(item) {
							var member = item.split('|');
							return '<img src="/profiles/view/photo/' + member[1] + '.jpg?size=tinythumbnail size-tinythumbnail" class="photo pull-right"><h3 class="name">' + member[0] + '</h3><div class="description">' + member[3] + '</div>';
						},

						updater: function(item) {
							var member = item.split('|');
							window.location = '/profiles/view/' + member[1];

							return member[0];
						},
						minLength:3
					});
			});
		});

		$('table.expanding-row-pairs').each(function(){
			$(this).find('tbody tr').each(function(i){
				if (i % 2 === 0) {
					var $selectRow = $(this), $expandRow = $selectRow.next('tr');
					$selectRow.data('expandRow', $expandRow.remove()).find('td:first').addClass('can-expand').prepend(
						$('<i/>').addClass('fa fa-fw fa-caret-right')
					);
				}
			}).end().on('click', 'td.can-expand', function(){
				var $row = $(this).closest('tr');
				if ($row.is('.expanded')) {
					$row.removeClass('expanded').next('tr').remove().end()
						.find('td i.fa-caret-down').removeClass('fa-caret-down').addClass('fa-caret-right');
				} else {
					$row.addClass('expanded').after($row.data('expandRow'))
						.find('td i.fa-caret-right').removeClass('fa-caret-right').addClass('fa-caret-down');
				}
			});
		});

		// MEMBER NOTE / EXTENUATING CIRCUMSTANCES STUFF

		$('section.member-notes, section.circumstances').on('click', 'a.create, a.edit', function(e) {
			// Bind click to load create-edit member note

			var $this = $(this);
			if ($this.hasClass('disabled')) {
				e.preventDefault();
				e.stopPropagation();
				return false;
			}

			var url = $this.attr('data-url'), $modal = $('#note-modal'), $modalBody = $modal.find('.modal-body');

			$modalBody.html('<iframe src="'+url+'" style="height:100%; width:100%;" frameBorder="0" scrolling="no"></iframe>')
				.find('iframe').on('load', function(){
					if($(this).contents().find('form').length == 0){
						//Handle empty response from iframe form submission
						$('#note-modal').modal('hide');
						document.location.reload(true);
					} else {
						//Bind iframe form submission to modal button
						$('#member-note-save').on('click', function(e){
							e.preventDefault();
							$('#note-modal').find('.modal-body').find('iframe').contents().find('form').submit();
							$(this).off();  //remove click event to prevent bindings from building up
						});
					}
				});
			$modal.find('.modal-header h3 span').text($this.attr('title')).end()
				.modal('show');
			e.preventDefault();
			e.stopPropagation();
			
		}).on('click', 'ul.dropdown-menu a:not(.edit)', function(e) {
			// Bind click events for dropdown

			var $this = $(this);
			if($this.hasClass('disabled')) {
				e.preventDefault();
				e.stopPropagation();
				return false;
			}

			var $row = $this.closest('tr'),
				$dropdownItems = $this.closest('ul').find('a'),
				$loading = $this.closest('td').find('i.fa-spinner'),
				url = $this.attr('href');

			$loading.toggleClass('invisible');

			$.post(url, function(data) {
				if (data.status == 'successful') {
					if($this.hasClass('delete') || $this.hasClass('restore')) {
						$dropdownItems.toggleClass('disabled');
						$row.toggleClass('subtle deleted');
						$row.find('.deleted-files').toggleClass('hidden');
					} else if($this.hasClass('purge')) {
						if ($row.hasClass('expanded')) {
							$row.find('td.can-expand').trigger('click');
						}
						$row.hide();
					}
				}
				$loading.toggleClass('invisible');
			}, 'json');

			e.preventDefault();
			e.stopPropagation();
		});

		// END OF MEMBER NOTE / EXTENUATING CIRCUMSTANCES STUFF
	});


	// take anything we've attached to 'exports' and add it to the global 'Profiles'
	// we use extend() to add to any existing variable rather than clobber it
	window.Profiles = jQuery.extend(window.Profiles, exports);
}(jQuery));
