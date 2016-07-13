/**
 * Scripts used only by student profiles.
 */
(function ($) { 'use strict';

	var exports = {};
	
	// Timetable calendar exports

	function toTimestamp(now, then) {
		var yesterday = now.clone().subtract(1, 'day');
		if (now.diff(then) < 60000) { // less than a minute ago
			return then.from(now);
		} else if (now.isSame(then, 'day')) {
			return then.format('LT [Today]');
		} else if (yesterday.isSame(then, 'day')) {
			return then.format('LT [Yesterday]');
		} else if (now.isSame(then, 'year')) {
			return then.format('ddd Do MMM LT');
		} else {
			return then.format('ddd Do MMM YYYY LT');
		}
	}
	exports.toTimestamp = toTimestamp;
	
	function onViewUpdate(view, weeks, $calendar){
		updateCalendarTitle(view, weeks);
		updateDownloadButton(view, $calendar);
		$('.popover').hide();
		$calendar.find('table').attr('role','presentation');
	}
	
	function updateCalendarTitle(view, weeks){
		if (view.name == 'agendaWeek') {
			var start = view.start.getTime();
			var end = view.end.getTime();
			var week = $.grep(weeks, function(week) {
				return (week.start >= start) && (week.end <= end);
			});
			if (week.length > 0) {
				var decodedTitle = $("<div/>").html(week[0].desc).text();
				view.title = decodedTitle;
				view.calendar.updateTitle();
			} // We should have an entry for every week; in the event that one's missing
			// we'll just leave it blank. The day columns still have the date on them.
			return true;
		}
	}

	function updateDownloadButton(view, $calendar) {
		if ($calendar.data('downloadbutton')) {
			var $downloadButton = $($calendar.data('downloadbutton'));
			$downloadButton.prop(
				'href',
				GlobalScripts.setArgOnUrl(
					GlobalScripts.setArgOnUrl(
						$downloadButton.prop('href'),
						'calendarView',
						view.name
					),
					'renderDate',
					view.start.getTime()/1000
				)
			);
		}
	}
	
	function renderCalendarEvents(event, element) {
		var content = "<table class='event-info'>";
		if (event.parentType && event.parentFullName && event.parentShortName && event.parentType === "Module") {
			content = content + "<tr><th>Module</th><td>" + event.parentShortName + " " + event.parentFullName + "</td></tr>";
		}

		if (event.fullTitle && event.fullTitle.length > 0) {
			content = content + "<tr><th>Title</th><td>" + event.fullTitle + "</td></tr>";
		}

		if (event.name && event.name.length > 0) {
			content = content + "<tr><th>What</th><td>" + event.name + "</td></tr>";
		}

		content = content + "<tr><th>When</th><td>"  + event.formattedInterval + "</td></tr>";

		if (event.location && event.location.length > 0) {
			content = content + "<tr><th>Where</th><td>";

			if (event.locationId && event.locationId.length > 0) {
				content = content + "<span class='map-location' data-lid='" + event.locationId + "'>" + event.location + "</span>";
			} else {
				content = content + event.location;
			}

			content = content + "</td></tr>";
		}

		if (event.tutorNames.length > 0){
			content = content + "<tr><th>Who</th><td> " + event.tutorNames + "</td></tr>";
		}

		if (event.comments && event.comments.length > 0) {
			content = content + "<tr><th>Comments</th><td>" + event.comments + "</td></tr>";
		}

		if (event.relatedUrl && event.relatedUrl.length > 0 && event.relatedUrlTitle && event.relatedUrlTitle.length > 0) {
			content = content + "<tr><th></th><td><a href='" + event.relatedUrl + "'>" + event.relatedUrlTitle + "</a></td></tr>";
		}

		content = content + "</table>";
		$(element).tabulaPopover({html:true, container:"body", title:event.shorterTitle, content:content});
	}
	
	exports.renderCalendarEvents = renderCalendarEvents;

	function createCalendar(container, defaultViewName, weeks, eventsCallback, hasStartDate, year, month, date, defaultDate) {
		var showWeekends = (defaultViewName == "month"), $container = $(container);
		var options = {
			events: eventsCallback($container),
			defaultView: defaultViewName,
			allDaySlot: false,
			slotMinutes: 60,
			firstHour: 8,
			firstDay: 1, // monday
			timeFormat: {
				agendaWeek: '', // don't display time on event
				agendaDay: '', // don't display time on event
				// for all other views
				'': 'HH:mm'   //  5:00 - 6:30
			},
			titleFormat: {
				month: 'MMMM yyyy',
				week: "MMM d[ yyyy]{ '&#8212;'[ MMM] d yyyy}",
				day: 'dddd, MMM d, yyyy'
			},
			columnFormat: {
				month: 'ddd',
				week: 'ddd d/M',
				day: 'dddd d/M'
			},
			defaultEventMinutes: 30,
			weekends: showWeekends,
			viewRender: function(view){
				onViewUpdate(view, weeks, $container);
			},
			header: {
				left:   'title',
				center: 'month,agendaWeek,agendaDay',
				right:  'today prev,next'
			},
			weekNumbers: true,
			weekNumberCalculation: function (moment) {
				var start = moment.getTime();
				var week = $.grep(weeks, function(week) {
					return (week.start >= start);
				});

				if (week.length > 0) {
					return week[0].shortDescription;
				}

				// We should have an entry for every week; in the event that one's missing
				// we'll just leave it blank. The day columns still have the date on them.
				return '';
			},
			eventAfterRender: function(event, element, view) {
				renderCalendarEvents(event, element);
			}
		};
		if (hasStartDate) {
			options.year = year;
			options.month = month;
			options.date = date;
			options.defaultDate = defaultDate
		}
		$container.fullCalendar(options);
	}
	exports.createCalendar = createCalendar;

	// End Timetable calendar exports

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

							var searchAllDepts = $("input[type='radio'][name='searchAllDepts']:checked").val();
							xhr = $.get(target, { query : query, searchAllDepts : searchAllDepts }, function(data) {
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
			}).find('tr.expand td.can-expand').trigger('click');
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
		
		// Add in People search data
		
		$('.peoplesearch-info').each(function(){
			var $this = $(this);
			$.getJSON($this.data('href'), function(data) {
				var items = [];
				if (data.extensionNumberWithExternal) {
					items.push('<strong>Phone:</strong> ' + data.extensionNumberWithExternal + '<br/>');
				}
				if (data.room) {
					items.push('<strong>Room:</strong> ' + data.room + '<br/>');
				}
				$this.html(items.join(''));
			});
		});

		// End Add in People search data
		
		// Meeting records

		var $meetingModal = $("#meeting-modal");
		$meetingModal.on('submit', 'form', function(e){
			e.preventDefault();
			// reattach the load handler and submit the inner form in the iframe
			$meetingModal.find('iframe')
				.off('load').on('load', meetingRecordIframeHandler)
				.contents().find('form').submit();

			// hide the iframe, so we don't get a FOUC
			$meetingModal.find('.modal-body').slideUp();
		});

		function meetingRecordFrameLoad(frame) {
			var $f = $(frame).contents();

			// reset slow load spinner
			$meetingModal.tabulaPrepareSpinners();

			if ($f.find('#meeting-record-form').length == 1) {
				// unhide the iframe
				$meetingModal.find('.modal-body').slideDown();

				// reset datepicker & submit protection
				var $form = $meetingModal.find('form.double-submit-protection');
				$form.tabulaSubmitOnce();
				$form.find('.btn').removeClass('disabled');
				// wipe any existing state information for the submit protection
				$form.removeData('submitOnceSubmitted');

				// show-time
				$meetingModal.modal('show');
				$meetingModal.off('shown.bs.modal.meetingRecordFrameLoad').on('shown.bs.modal.meetingRecordFrameLoad', function(){
					$f.find('[name="title"]').focus();
				});
			} else {
				$meetingModal.modal('hide');
				document.location.reload(true);
			}
		}

		// named handler that can be unbound
		function meetingRecordIframeHandler() {
			meetingRecordFrameLoad(this);
			$(this).off('load', meetingRecordIframeHandler);
		}

		function prepareMeetingModal($this, targetUrl) {
			$.get(GlobalScripts.setArgOnUrl(targetUrl, 'modal', ''), function(data) {
				$meetingModal.html(data);
				var $mb = $meetingModal.find('.modal-body').empty();
				var iframeMarkup = "<iframe frameBorder='0' scrolling='no' style='height:100%;width:100%;' id='modal-content'></iframe>";
				$(iframeMarkup)
					.off('load').on('load', meetingRecordIframeHandler)
					.attr('src', GlobalScripts.setArgOnUrl(targetUrl, 'iframe', ''))
					.appendTo($mb);
			}).fail(function() {
				if (!$('#meeting-modal-failure').length) {
					var $error = $('<p id="meeting-modal-failure" class="alert alert-error hide"><i class="icon-warning-sign"></i> Sorry, I\'m unable to edit meeting records for this student at the moment.</p>');
					$this.before($error);
					$error.slideDown();
				}
			});
		}
		
		$('section.meetings').on('click', '.new-meeting-record, .edit-meeting-record', function(e){
			var $this = $(this);
			prepareMeetingModal($this, $this.attr('href'));
			e.preventDefault();
		}).on('click', 'ul.dropdown-menu a:not(.edit-meeting-record)', function(e) {
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
					if($this.hasClass('delete-meeting-record') || $this.hasClass('restore-meeting-record')) {
						$dropdownItems.toggleClass('disabled');
						$row.toggleClass('subtle deleted');
						$row.find('.deleted-files').toggleClass('hidden');
					} else if($this.hasClass('purge-meeting-record')) {
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
		}).on('submit', 'form.scheduled-action', function(e) {
			// Scheduled meetings
			e.preventDefault();
			var $this = $(this), checkedInput = $this.find('input:checked');
			$this.find('div.ajaxErrors').hide();
			switch (checkedInput.val()) {
				case 'confirm': {
					prepareMeetingModal($this, checkedInput.data('formhref'));
				} break;
				case 'reschedule': {
					$this.closest('tr').prev('tr').find('.edit-meeting-record').trigger('click');
				} break;
				case 'missed': {
					$.post(checkedInput.data('formhref'), $this.serialize(), function(data){
						if(data.status === 'successful') {
							document.location.reload(true);
						} else {
							$this.find('div.ajaxErrors').empty().html(data.errors.join('<br />')).show();
						}
					});
				} break;
			}
		});

		$('section.meetings input.reject').each( function() {
			var $this = $(this);
			var $form = $this.closest('form');
			var $commentBox = $form.find('.rejection-comment');
			$this.slideMoreOptions($commentBox, true);
		});
		$('section.meetings .approval').parent().tabulaAjaxSubmit(function() {
			document.location.reload(true);
		});
		
		// End Meeting records

		// Seminars

		// enable/disable the "sign up" buttons
		$('#student-groups-view')
			.find('.sign-up-button')
				.addClass('disabled use-tooltip')
				.prop('disabled',true)
				.prop('title','Please select a group')
				.end()
			.find('input.group-selection-radio')
			.on('change', function(){
				$(this).closest('.item-info').find('.sign-up-button').removeClass('disabled use-tooltip').prop('disabled',false).prop('title','');
			});

		// End Seminars
	});


	// take anything we've attached to 'exports' and add it to the global 'Profiles'
	// we use extend() to add to any existing variable rather than clobber it
	window.Profiles = jQuery.extend(window.Profiles, exports);
}(jQuery));
