<#escape x as x?html>
<@f.form commandName="command" action=submitUrl method="POST" cssClass="form-inline">
	<@f.hidden path="from" />
	<@f.hidden path="to" />

<div class="student-filter btn-group-group well well-small">
	<button type="button" class="clear-all-filters btn btn-link">
			<span class="icon-stack">
				<i class="icon-filter"></i>
				<i class="icon-ban-circle icon-stack-base"></i>
			</span>
	</button>

	<#macro filter path placeholder currentFilter allItems validItems=allItems prefix="" customPicker="">
		<@spring.bind path=path>
			<div class="btn-group<#if currentFilter == placeholder> empty-filter</#if>">
				<a class="btn btn-mini dropdown-toggle" data-toggle="dropdown">
					<span class="filter-short-values" data-placeholder="${placeholder}" data-prefix="${prefix}"><#if currentFilter != placeholder>${prefix}</#if>${currentFilter}</span>
					<span class="caret"></span>
				</a>
				<div class="dropdown-menu filter-list">
					<button type="button" class="close" data-dismiss="dropdown" aria-hidden="true" title="Close">×</button>
					<ul>
						<#if customPicker?has_content>
							<li>
								<#noescape>${customPicker}</#noescape>
							</li>
						</#if>
						<#list allItems as item>
							<#local isValid = (allItems?size == validItems?size)!true />
							<#if !isValid>
								<#list validItems as validItem>
									<#if ((validItem.id)!0) == ((item.id)!0)>
										<#local isValid = true />
									</#if>
								</#list>
							</#if>
							<li class="check-list-item" data-natural-sort="${item_index}">
								<label class="checkbox <#if !isValid>disabled</#if>">
									<#nested item isValid/>
								</label>
							</li>
						</#list>
					</ul>
				</div>
			</div>
		</@spring.bind>
	</#macro>

	<#macro current_filter_value path placeholder><#compress>
		<@spring.bind path=path>
			<#if status.actualValue?has_content>
				<#list status.actualValue as item><#nested item /><#if item_has_next>, </#if></#list>
			<#else>
			${placeholder}
			</#if>
		</@spring.bind>
	</#compress></#macro>

	<#function contains_by_code collection item>
		<#list collection as c>
			<#if c.code == item.code>
				<#return true />
			</#if>
		</#list>
		<#return false />
	</#function>

	<#assign placeholder = "Module" />
	<#assign modulesCustomPicker>
		<div class="module-search input-append">
			<input class="module-search-query module prevent-reload" type="text" value="" placeholder="Search for a module" />
			<span class="add-on"><i class="icon-search"></i></span>
		</div>
	</#assign>
	<#assign currentfilter><@current_filter_value "modules" placeholder; module>${module.code?upper_case}</@current_filter_value></#assign>
	<@filter path="modules" placeholder=placeholder currentFilter=currentfilter allItems=command.allModules customPicker=modulesCustomPicker; module>
		<input type="checkbox" name="${status.expression}" value="${module.code}"  data-short-value="${module.code?upper_case}"
		${contains_by_code(command.modules, module)?string('checked','')}
		>
		<@fmt.module_name module false />
	</@filter>

	<#assign placeholder = "Route" />
	<#assign routesCustomPicker>
		<div class="route-search input-append">
			<input class="route-search-query route prevent-reload" type="text" value="" placeholder="Search for a route" />
			<span class="add-on"><i class="icon-search"></i></span>
		</div>
	</#assign>
	<#assign currentfilter><@current_filter_value "routes" placeholder; route>${route.code?upper_case}</@current_filter_value></#assign>
	<@filter path="routes" placeholder=placeholder currentFilter=currentfilter allItems=command.allRoutes customPicker=routesCustomPicker; route>
		<input type="checkbox" name="${status.expression}" value="${route.code}"  data-short-value="${route.code?upper_case}"
		${contains_by_code(command.routes, route)?string('checked','')}
		>
		<@fmt.route_name route false />
	</@filter>

	<#assign placeholder = "Year of study" />
	<#assign currentfilter><@current_filter_value "yearsOfStudy" placeholder; year>${year}</@current_filter_value></#assign>
	<@filter path="yearsOfStudy" placeholder=placeholder currentFilter=currentfilter allItems=command.allYearsOfStudy prefix="Year "; yearOfStudy>
		<input type="checkbox" name="${status.expression}" value="${yearOfStudy}" data-short-value="${yearOfStudy}"
		${command.yearsOfStudy?seq_contains(yearOfStudy)?string('checked','')}
		>
	${yearOfStudy}
	</@filter>

	<#if canFilterStudents>
		<#assign placeholder = "Student" />
		<#assign studentsCustomPicker>
			<div class="student-search input-append">
				<input class="student-search-query student prevent-reload" type="text" value="" placeholder="Search for a student" data-include-groups="false" data-include-email="false" data-members-only="true" data-universityid="true" />
				<span class="add-on"><i class="icon-search"></i></span>
			</div>
		</#assign>
		<#assign currentfilter><@current_filter_value "studentMembers" placeholder; student>${student.universityId}</@current_filter_value></#assign>
		<@filter path="students" placeholder=placeholder currentFilter=currentfilter allItems=command.suggestedStudents customPicker=studentsCustomPicker; student>
			<input type="checkbox" name="${status.expression}" value="${student.universityId}"  data-short-value="${student.universityId}"
			${command.students?seq_contains(student.universityId)?string('checked','')}
			>
		${student.fullName} (${student.universityId})
		</@filter>
	</#if>

	<#if canFilterStaff>
		<#assign placeholder = "Staff" />
		<#assign staffCustomPicker>
			<div class="staff-search input-append">
				<input class="staff-search-query staff prevent-reload" type="text" value="" placeholder="Search for staff" data-include-groups="false" data-include-email="false" data-members-only="true" data-universityid="true" />
				<span class="add-on"><i class="icon-search"></i></span>
			</div>
		</#assign>
		<#assign currentfilter><@current_filter_value "staffMembers" placeholder; staffMember>${staffMember.universityId}</@current_filter_value></#assign>
		<@filter path="staff" placeholder=placeholder currentFilter=currentfilter allItems=command.suggestedStaff customPicker=staffCustomPicker; staffMember>
			<input type="checkbox" name="${status.expression}" value="${staffMember.universityId}"  data-short-value="${staffMember.universityId}"
			${command.staff?seq_contains(staffMember.universityId)?string('checked','')}
			>
		${staffMember.fullName} (${staffMember.universityId})
		</@filter>
	</#if>

	<#assign placeholder = "Event types" />
	<#assign currentfilter><@current_filter_value "eventTypes" placeholder; eventType>${eventType.displayName}</@current_filter_value></#assign>
	<@filter path="eventTypes" placeholder=placeholder currentFilter=currentfilter allItems=command.allEventTypes; eventType>
		<input type="checkbox" name="${status.expression}" value="${eventType.code}" data-short-value="${eventType.displayName}"
		${contains_by_code(command.eventTypes, eventType)?string('checked','')}
		>
		${(eventType.displayName)!}
	</@filter>
</div>
</@f.form>

<div class="alert alert-error" style="display: none;"></div>

<div class="calendar-outer">
	<div class="calendar-loading hidden-print">
		<i class="icon-spinner icon-spin"></i><em> Loading&hellip;</em>
	</div>
	<div class="calendar" data-viewname="month"></div>
</div>

<style type="text/css">
	@import url("<@url resource="/static/css/fullcalendar.css" />");
	@import url("<@url resource="/static/css/fullcalendar-custom.css" />");

	.fc-event.allday {
		font-weight: bold;
		color: white !important;
		border-color: #185c54 !important;
		font-size: .95em;
	}

	.calendar-outer {
		position: relative;
	}
	.calendar {
		background: white;
		position: relative;
		z-index: 1;
	}
	.calendar-loading {
		position: absolute;
		top: 50%;
		font-size: 4em;
		line-height: 4em;
		margin-top: -2em;
		left: 50%;
		width: 400px;
		margin-left: -200px;
		text-align: center;
		display: none;
	}
</style>

<@script "/static/js/fullcalendar.js" />
<script type="text/javascript">
	// TIMETABLE STUFF
	jQuery(function($) {
		var weeks = ${weekRangesDumper()};

		var $form = $('#command');

		function getEvents($container){
			return function (start, end, callback){
				var complete = false;
				setTimeout(function() {
					if (!complete) {
						$('.calendar-loading').show();
						$container.fadeTo('fast', 0.3);
					}
				}, 300);
				var startToSend = new Date(start.getTime());
				startToSend.setDate(startToSend.getDate() - 1);
				var endToSend = new Date(end.getTime());
				endToSend.setDate(endToSend.getDate() + 1);
				$('#from').val(startToSend.getTime()/1000);
				$('#to').val(endToSend.getTime()/1000);
				$.ajax({
					url:'${submitUrl}',
					type: 'POST',
					// make the from/to params compatible with what FullCalendar sends if you just specify a URL
					// as an eventSource, rather than a function. i.e. use seconds-since-the-epoch.
					data: $form.serialize(),
					success:function(data){
						if (data.lastUpdated) {
							// Update the last updated timestamp
							$container.find('> .fc-last-updated').remove();

							var now = moment();
							var time = moment(data.lastUpdated);

							$container.append(
									$('<div />').addClass('fc-last-updated').addClass('pull-right').html('Last updated: ' + toTimestamp(now, time))
							);
						}

						var events = data.events, errorDiv = $('div.alert-error');
						// TAB-3008 - Change times to Europe/London
						$.each(events, function(i, event){
							event.start = moment(moment.unix(event.start).tz('Europe/London').format('YYYY-MM-DDTHH:mm:ss')).unix();
							event.end = moment(moment.unix(event.end).tz('Europe/London').format('YYYY-MM-DDTHH:mm:ss')).unix();
						});
						if (data.errors.length > 0) {
							errorDiv.empty().show();
							$.each(data.errors, function(i, error) {
								errorDiv.append(error).append('<br />');
							});
						} else {
							errorDiv.hide();
						}
						callback(events);
					},
					complete: function() {
						complete = true;
						$container.fadeTo('fast', 1);
					}
				});
			};
		}
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
		function onViewUpdate(view, element){
			updateCalendarTitle(view, element);
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

		function createCalendar(container, defaultViewName){
			var showWeekends = (defaultViewName == "month");
			var cal = $(container).fullCalendar({
				events: getEvents($(container)),
				defaultView: defaultViewName,
				<#if startDate??>
					year: ${startDate.getYear()?c},
					month: ${(startDate.getMonthOfYear() - 1)?c},
					date: ${startDate.getDayOfMonth()?c},
					defaultDate: '${startDate.toString("YYYY-MM-dd")}', // This is here for FullCalendar 2 support or if it's ever backported to 1.6.x
				</#if>
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
				viewRender: onViewUpdate,
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

					content = content + "</table>";
					$(element).tabulaPopover({html:true, container:"#container",title:event.shorterTitle, content:content})
				}
			});
		}

		var $calendar = $(".calendar");
		createCalendar($calendar, $calendar.data('viewname'));

		var prependClearLink = function($list) {
			if (!$list.find('input:checked').length) {
				$list.find('.clear-this-filter').remove();
			} else {
				if (!$list.find('.clear-this-filter').length) {
					$list.find('> ul').prepend(
							$('<li />').addClass('clear-this-filter')
									.append(
											$('<button />').attr('type', 'button')
													.addClass('btn btn-link')
													.html('<i class="icon-ban-circle"></i> Clear selected items')
													.on('click', function(e) {
														$list.find('input:checked').each(function() {
															var $checkbox = $(this);
															$checkbox.prop('checked', false);
															updateFilter($checkbox);
														});

														doRequest($list.closest('form'));
													})
									).append($('<hr />'))
					);
				}
			}
		};

		var updateFilter = function($el) {
			// Update the filter content
			var $list = $el.closest('ul');
			var shortValues = $list.find(':checked').map(function() { return $(this).data('short-value'); }).get();
			var $fsv = $el.closest('.btn-group').find('.filter-short-values');
			if (shortValues.length) {
				$el.closest('.btn-group').removeClass('empty-filter');
				$fsv.html($fsv.data("prefix") + shortValues.join(', '));
			} else {
				$el.closest('.btn-group').addClass('empty-filter');
				$fsv.html($fsv.data('placeholder'));
			}

			updateClearAllButton($el);
		};

		var updateClearAllButton = function($el) {
			var $filterList = $el.closest(".student-filter");

			if ($filterList.find(".empty-filter").length == $filterList.find(".btn-group").length) {
				$('.clear-all-filters').attr("disabled", "disabled");
			} else {
				$('.clear-all-filters').removeAttr("disabled");
			}
		};

		var doRequest = function() {
			if (typeof history.pushState !== 'undefined')
				history.pushState(null, null, $form.attr('action') + '?' + $form.serialize());

			$calendar.fullCalendar('refetchEvents');
		};
		window.doRequest = doRequest;

		$form.on('change', 'input', function(e) {
			// Load the new results
			var $input = $(this);

			if ($input.is('.prevent-reload')) return;

			doRequest();
			updateFilter($input);
		});

		// Re-order elements inside the dropdown when opened
		$('.filter-list').closest('.btn-group').find('.dropdown-toggle').on('click.dropdown.data-api', function(e) {
			var $this = $(this);
			if (!$this.closest('.btn-group').hasClass('open')) {
				// Re-order before it's opened!
				var $list = $this.closest('.btn-group').find('.filter-list');
				var items = $list.find('li.check-list-item').get();

				items.sort(function(a, b) {
					var aChecked = $(a).find('input').is(':checked');
					var bChecked = $(b).find('input').is(':checked');

					if (aChecked && !bChecked) return -1;
					else if (!aChecked && bChecked) return 1;
					else return $(a).data('natural-sort') - $(b).data('natural-sort');
				});

				$.each(items, function(item, el) {
					$list.find('> ul').append(el);
				});

				prependClearLink($list);
			}
		});

		$('.clear-all-filters').on('click', function() {
			$('.filter-list').each(function() {
				var $list = $(this);

				$list.find('input:checked').each(function() {
					var $checkbox = $(this);
					$checkbox.prop('checked', false);
					updateFilter($checkbox);
				});

				prependClearLink($list);
			});

			doRequest();
		});

		var updateFilterFromPicker = function($picker, name, value, shortValue, labelText) {
			if (value === undefined || value.length === 0)
				return;

			if (labelText === undefined || labelText.length === 0) {
				labelText = $picker.val();
			}

			shortValue = shortValue || value;

			var $ul = $picker.closest('ul');

			var $li = $ul.find('input[value="' + value + '"]').closest('li');
			if ($li.length) {
				$li.find('input').prop('checked', true);
				if ($ul.find('li.check-list-item:first').find('input').val() !== value) {
					$li.insertBefore($ul.find('li.check-list-item:first'));
				}
			} else {
				var $newLI = $('<li/>').addClass('check-list-item').append(
						$('<label/>').addClass('checkbox').append(
								$('<input/>').attr({
									'type':'checkbox',
									'name':name,
									'value':value,
									'checked':true
								}).data('short-value', shortValue)
						).append(
								" " + labelText
						)
				);
				var firstLI = $ul.find('li.check-list-item:first');
				if (firstLI.length > 0) {
					$newLI.insertBefore($ul.find('li.check-list-item:first'))
				} else {
					$ul.append($newLI);
				}
			}

			doRequest();
			updateFilter($picker);
		};

		$('.module-search-query').on('change', function(){
			var $picker = $(this);
			if ($picker.data('modulecode') === undefined || $picker.data('modulecode').length === 0)
				return;

			updateFilterFromPicker($picker, 'modules', $picker.data('modulecode'), $picker.data('modulecode').toUpperCase());

			$picker.data('modulecode','').val('');
		}).modulePicker({});

		$('.route-search-query').on('change', function(){
			var $picker = $(this);
			if ($picker.data('routecode') === undefined || $picker.data('routecode').length === 0)
				return;

			updateFilterFromPicker($picker, 'routes', $picker.data('routecode'), $picker.data('routecode').toUpperCase());

			$picker.data('routecode','').val('');
		}).routePicker({});

		$('.student-search-query').on('change', function(){
			var $picker = $(this);
			if ($picker.data('fullname') === undefined || $picker.data('fullname').length === 0)
				return;

			updateFilterFromPicker($picker, 'students', $picker.val(), $picker.val(), $picker.data('fullname') + ' (' + $picker.val() + ')');

			$picker.data('fullname','').val('').data('flexiPicker').richResultField.edit();
		}).flexiPicker({});

		$('.staff-search-query').on('change', function(){
			var $picker = $(this);
			if ($picker.data('fullname') === undefined || $picker.data('fullname').length === 0)
				return;

			updateFilterFromPicker($picker, 'staff', $picker.val(), $picker.val(), $picker.data('fullname') + ' (' + $picker.val() + ')');

			$picker.data('fullname','').val('').data('flexiPicker').richResultField.edit();
		}).flexiPicker({});
	});
</script>
</#escape>