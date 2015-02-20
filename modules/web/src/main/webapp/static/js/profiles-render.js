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
				var $spinner = $('<div class="spinner-container" />'), $this = $(this);
				$this
					.before($spinner)
					.on('focus', function(){
						container.find('.use-tooltip').trigger('mouseover');
					})
					.on('blur', function(){
						container.find('.use-tooltip').trigger('mouseout');
					})
					.typeahead({
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
							return '<img src="/profiles/view/photo/' + member[1] + '.jpg?size=tinythumbnail" class="photo pull-right"><h3 class="name">' + member[0] + '</h3><div class="description">' + member[3] + '</div>';
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

	// make chevron rotate when "More details" clicked
	$(function() {
		$(".expandable-course-details").on("click", "h6", function() {
			$(this).find("i").toggleClass("icon-chevron-right").toggleClass("icon-chevron-down").toggleClass("expanded");
		});
	});


	// MEETING RECORD STUFF
	exports.SetupMeetingRecords = function(containerSelector) {
		var $meetingsSection = (containerSelector != undefined && containerSelector.length > 0) ? $(containerSelector) : $('section.meetings');

		$(function() {
			function scrollToOpenDetails() {
				var $openMeetingDetails = $("details.open", $meetingsSection).first();
				if ($openMeetingDetails.length) {
					var offset = window.getNavigationHeight || 0;
					$("html, body").animate({
						scrollTop: $openMeetingDetails.offset().top - offset
					}, 300);
				}
			}

			function frameLoad(frame) {
				var $m = $("#modal");
				var $f = $(frame).contents();

				// reset slow load spinner
				$m.tabulaPrepareSpinners();

				if ($f.find("#meeting-record-form").length == 1) {
					// unhide the iframe
					$m.find('.modal-body').slideDown();

					// reset datepicker & submit protection
					var $form = $m.find('form.double-submit-protection');
					$form.tabulaSubmitOnce();
					$form.find(".btn").removeClass('disabled');
					// wipe any existing state information for the submit protection
					$form.removeData('submitOnceSubmitted');

					//				// firefox fix
					//				var wait = setInterval(function() {
					//					var h = $f.find("body").height();
					//					if (h > 0) {
					//						clearInterval(wait);
					//						$m.find(".modal-body").animate({ height: h });
					//					}
					//				}, 300); // this didn't work for me (ZLJ) at 150 but did at 200; upping to 300 to include safety margin

					// show-time
					$m.modal("show");
					$m.on("shown", function() {
						$f.find("[name='title']").focus();
					});
				} else if ($f.find("section.meetings").length == 1) {
					var source =$f.find("section.meetings");
					var targetClass = source.attr('data-target-container');
					var target = $("section."+targetClass);
					// bust the returned content out to the original page, and kill the modal
					target.replaceWith(source);
					$('details').details();
					// rebind all of this stuff to the new UI
					decorateMeetingRecords(source);
					$m.modal("hide");
				} else {
					/*
					 TODO more user-friendly fall-back?
					 This is where you end up with an unexpected failure, eg. permission failure, mangled URL etc.
					 The default is to reload the original profile page. Not sure if there's something more helpful
					 we could/should do here.
					 */
					$m.modal('hide');
					document.location.reload(true);
				}
			}


			function decorateMeetingRecords($innerMeetingsSection){

				// delete meeting records
				$('a.delete-meeting-record', $innerMeetingsSection).on('click', function() {
					var $this = $(this);
					var $details = $this.closest('details');

					if (!$details.hasClass("deleted")) {
						$details.addClass("processing");
						var url = $this.attr("href");
						$.post(url, function(data) {
							if (data.status == "successful") {
								$details.addClass("deleted muted");
							} else if (data.status == "error") {
								// TAB-2487 was hard to spot because nothing is done with failed ajax
								// TODO - come up with a better way of reporting JSONErrorView failures app wide
								alert(data.errors[0]); // alert the first error.
							}
							$details.removeClass("processing");
						}, "json");
					}
					return false;
				});

				// restore meeting records
				$('a.restore-meeting-record', $innerMeetingsSection).on('click', function() {
					var $this = $(this);
					var $details = $this.closest('details');

					if ($details.hasClass("deleted")) {
						$details.addClass("processing");
						var url = $this.attr("href");
						$.post(url, function(data) {
							if (data.status == "successful") {
								$details.removeClass("deleted muted");
							}
							$details.removeClass("processing");
						}, "json");
					}
					return false;
				});

				// purge meeting records
				$('a.purge-meeting-record', $innerMeetingsSection).on('click', function() {
					var $this = $(this);
					var $details = $this.closest('details');

					if ($details.hasClass("deleted")) {
						$details.addClass("processing");
						var url = $this.attr("href");
						$.post(url, function(data) {
							if (data.status == "successful") {
								$details.remove();
							}
						}, "json");
					}
					return false;
				});

				// show rejection comment box
				$('input.reject', $innerMeetingsSection).each( function() {
					var $this = $(this);
					var $form = $this.closest('form');
					var $commentBox = $form.find('.rejection-comment');
					$this.slideMoreOptions($commentBox, true);
				});

				// make modal links use ajax
				$('.meeting-record-toolbar, details.meeting.normal', $innerMeetingsSection).tabulaAjaxSubmit(function() {
					document.location.reload(true);
				});

				var $m = $("#modal");

				scrollToOpenDetails();

				/* load form into modal, with picker enabled
				 * click selector must be specific otherwise the click event will propagate up past the section element
				 * these HTML5 elements have default browser driven behaviour that is hard to override.
				 */

				// named handler that can be unbound
				var iframeHandler = function() {
					frameLoad(this);
					$(this).off('load', iframeHandler);
				};

				var getModal = function($this, targetUrl) {
					$.get(targetUrl + "?modal", function(data) {
						$m.html(data);
						var $mb = $m.find(".modal-body").empty();
						var iframeMarkup = "<iframe frameBorder='0' scrolling='no' style='height:100%;width:100%;' id='modal-content'></iframe>";
						$(iframeMarkup)
							.off('load').on('load', iframeHandler)
							.attr("src", targetUrl + "?iframe")
							.appendTo($mb);
					}).fail(function() {
						if (!$('#meeting-modal-failure').length) {
							var $error = $('<p id="meeting-modal-failure" class="alert alert-error hide"><i class="icon-warning-sign"></i> Sorry, I\'m unable to edit meeting records for this student at the moment.</p>');
							$this.before($error);
							$error.slideDown();
						}
					});
				};

				$(".new, .edit-meeting-record", $innerMeetingsSection).on("click", function(e) {
					e.preventDefault();
					var $this = $(this);
					if (!$this.closest('details').is('.deleted')) {
						getModal($this, $this.attr("href"));
					}
					return false;
				});

				$m.on('submit', 'form', function(e){
					e.preventDefault();
					// reattach the load handler and submit the inner form in the iframe
					$m.find('iframe')
						.off('load').on('load', iframeHandler)
						.contents().find('form').submit();

					// hide the iframe, so we don't get a FOUC
					$m.find('.modal-body').slideUp();
				});

				// Scheduled meetings

				$('form.scheduled-action', $innerMeetingsSection).on('submit', function(event){
					event.preventDefault();
					var $this = $(this), checkedInput = $this.find('input:checked');
					$this.find('div.ajaxErrors').hide();
					switch (checkedInput.val()) {
						case "confirm": {
							getModal($this, checkedInput.data('formhref'));
						} break;
						case "reschedule": {
							$this.closest('details').find('.meeting-record-toolbar .edit-meeting-record').trigger('click');
						} break;
						case "missed": {
							$.post(checkedInput.data('formhref'), $this.serialize(), function(data){
								if(data.status === "successful") {
									document.location.reload(true);
								} else {
									$this.find('div.ajaxErrors').empty().html(data.errors.join('<br />')).show();
								}
							});
						} break;
					}
				});

			}
			// call on page load
			decorateMeetingRecords($meetingsSection);


		});
	};
	//END MEETING RECORD APPROVAL STUFF

	//MEMBERNOTE STUFF

	//Iframe onload function call - sets size and binds iframe submit click to modal
	window.noteFrameLoad = function(frame) {

		if($(frame).contents().find("form").length == 0){
			//Handle empty response from iframe form submission
			$("#note-modal").modal('hide');
			document.location.reload(true);

		} else {
			//Bind iframe form submission to modal button
			$("#member-note-save").on('click', function(e){
				e.preventDefault();
				$("#note-modal .modal-body").find('iframe').contents().find('form').submit();
				$(this).off()  //remove click event to prevent bindings from building up
			});
		}
	}

	$(function() {
		// Bind click to load create-edit member note
		$("#membernote-details").on("click", ".create, .edit", function(e) {
			if ($(this).hasClass("disabled")) return false;

			var url = $(this).attr('data-url');
			var $modalBody =  $("#note-modal .modal-body")

			$modalBody.html('<iframe src="'+url+'" style="height:100%; width:100%;" onLoad="noteFrameLoad(this)" frameBorder="0" scrolling="no"></iframe>')
			$("#note-modal .modal-header h3 span").text($(this).attr("title"))
			$("#note-modal").modal('show')
			return false; //stop propagation and prevent default
		});

		//Bind click events for toolbar
		$('.member-note-toolbar a:not(.edit)').on('click', function(e) {

			var $this = $(this);
			var $details = $this.closest('details');
			var $toolbaritems = $this.closest('.member-note-toolbar').children();

			if($this.hasClass("disabled")) return false;

			$details.addClass("processing");
			var url = $this.attr("href");

			$.post(url, function(data) {
				if (data.status == "successful") {
					if($this.hasClass('delete') || $this.hasClass('restore')) {
						$toolbaritems.toggleClass("disabled");
						$details.toggleClass("muted deleted");
						$details.find('.deleted-files').toggleClass('hidden');
					} else if($this.hasClass('purge')) {
						$details.slideUp("slow")
					}
				}
				$details.removeClass("processing");
			}, "json");

			return false; //stop propagation and prevent default
		});

	});
	//END OF MEMBERNOTE STUFF

	// TIMETABLE STUFF
	$(function() {
		function getEvents(studentId, $container){
			return function (start, end, callback){
				var complete = false;
				setTimeout(function() {
					if (!complete) $container.fadeTo('fast', 0.3);
				}, 300);
				$.ajax({url:'/profiles/timetable/api',
					// make the from/to params compatible with what FullCalendar sends if you just specify a URL
					// as an eventSource, rather than a function. i.e. use seconds-since-the-epoch.
					data:{
						'from':start.getTime()/1000,
						'to':end.getTime()/1000,
						'whoFor':studentId
					},
					success:function(data){
						//
						// TODO
						//
						// insert code here to look through the events and magically display weekends if a weekend event is found
						// (cal.fullCalendar('option','weekends',true); doesn't work, although some other options do)
						// https://code.google.com/p/fullcalendar/issues/detail?id=293 has some discussion and patches
						//
						// TAB-3008 - Change times to Europe/London
						$.each(data, function(i, event){
							event.start = moment(moment.unix(event.start).tz('Europe/London').format('YYYY-MM-DDTHH:mm:ss')).unix();
							event.end = moment(moment.unix(event.end).tz('Europe/London').format('YYYY-MM-DDTHH:mm:ss')).unix();
						});
						callback(data);
					},
					complete: function() {
						complete = true;
						$container.fadeTo('fast', 1);
					}
				});
			};
		}
		function onViewUpdate(view,element){
			updateCalendarTitle(view, element);
			updateFullScreenLink(view, element);
			$('.popover').hide();
		}
		// relies on the variable "weeks" having been defined elsewhere, by using the WeekRangesDumperTag
		function updateCalendarTitle(view,element){
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

		function updateFullScreenLink(view, element) {
			$(element).closest('section').find('.timetable-fullscreen').each(function(){
				$(this).prop('href', $(this).prop('href').replace(/\?now=\d+/,'') + '?now=' + view.start.getTime());
			});
		}

		function createCalendar(container, defaultViewName, studentId, showViewSwitcher, currentDateMillis){
			var renderDate = (currentDateMillis) ? new Date(currentDateMillis) : new Date();
			var showWeekends = (defaultViewName == "month");
			var cal = $(container).fullCalendar({
				events: getEvents(studentId, $(container)),
				defaultView: defaultViewName,
				year: renderDate.getFullYear(),
				month: renderDate.getMonth(),
				date: renderDate.getDate(),
				allDaySlot: false,
				slotMinutes: 60,
				firstHour:8,
				firstDay: 1, //monday
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
				weekends:showWeekends,
				viewRender:onViewUpdate,
				header: {
					left:   'title',
					center: (showViewSwitcher ? 'month,agendaWeek,agendaDay' : ''),
					right:  'today prev,next'
				},
				eventAfterRender: function(event, element, view){
					var content = "<table class='event-info'>";
					if (event.fullTitle && event.fullTitle.length > 0) {
						content = content + "<tr><th>Title</th><td>" + event.fullTitle + "</td></tr>";
					}

					if (event.description && event.description.length > 0) {
						content = content + "<tr><th>What</th><td>" + event.description + "</td></tr>";
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

					content = content + "</table>";
					$(element).tabulaPopover({html:true, container:"#container",title:event.shorterTitle, content:content})
				}
			});
			$(document).on('tabbablechanged', function(e) {
				// redraw the calendar if the layout updates
				cal.fullCalendar('render');
			});
		}

		$(".fullCalendar").each(function(){
			createCalendar(
				$(this),
				$(this).data('viewname'),
				$(this).data('studentid'),
				$(this).data('showviewswitcher'),
				$(this).data('renderdate')
			);
		});
	});

	$(function(){
		$('#timetable-pane h4 a').on('click', function(e){
			e.preventDefault();
			$('#timetable-ical-modal').modal('show');
		});
	});

	// Save user preferences for default profile view
	$(function() {
		$(document).on('tabbablechanged', function(e, options) {
			if (typeof(options) === 'object' && typeof(options.layout) !== 'undefined') {
				// Update the default view
				$.post('/settings.json', {'profilesDefaultView': options.layout});
			}
		});
	});

	// take anything we've attached to "exports" and add it to the global "Profiles"
	// we use extend() to add to any existing variable rather than clobber it
	window.Profiles = jQuery.extend(window.Profiles, exports);
}(jQuery));
