<#macro reportLoader reportUrl>
	<script>
		window.ReportBuilder = {};
	</script>
	<div class="loading">
		<p><em>Building report...</em></p>
	
		<div class="progress">
			<div class="bar" style="width: 33%;"></div>
		</div>
	</div>
	
	<div class="complete alert alert-success" style="display: none;">
		<p>Report complete</p>
		<div class="btn-toolbar">
			<a href="#" class="show-data btn" data-loading-text="Loading&hellip;">
				<i class="icon-eye-open"></i> Show
			</a>
			<div class="download btn-group ">
				<a href="#" class="btn dropdown-toggle" data-toggle="dropdown">
					<i class="icon-download"></i> Download&hellip;
					<span class="caret"></span>
				</a>
				<#nested />
			</div>
		</div>
	</div>
	
	<div class="alert alert-error" style="display: none;">
		<p>There was a problem generating the report. If the problem persists, please contact the <a href="mailto:webteam@warwick.ac.uk">ITS Web Team</a>.</p>
	</div>
	
	<div class="report-target"></div>
	<form class="report-target-form" style="display: none;" method="POST" action="" enctype="multipart/form-data"></form>
	
	<script>
		jQuery(function($){
			$.ajax('${reportUrl}', {
				type: 'POST',
				success: function(data) {
					var $mainContent = $('#main-content');
					$mainContent.find('.loading p em').html('Downloading data&hellip;');
					$mainContent.find('.loading .bar').width('66%');
					setTimeout(function(){
						var key, key1, key2, result = [];
						window.ReportBuilder.reportData = data;
						for (key in data) {
							if (data.hasOwnProperty(key)) {
								if ($.isArray(data[key])) {
									$.each(data[key], function(i, obj){
										for (key1 in obj) {
											if (obj.hasOwnProperty(key1)) {
												result.push(
													$('<input/>').prop({
														'type': 'hidden',
														'name': key + '[' + i + '][' + key1 + ']',
														'value': obj[key1]
													})
												);
											}
										}
									})
								} else {
									for (key1 in data[key]) {
										if (data[key].hasOwnProperty(key1)) {
											for (key2 in data[key][key1]) {
												if (data[key][key1].hasOwnProperty(key2)) {
													result.push(
															$('<input/>').prop({
																'type': 'hidden',
																'name': key + '[' + key1 + '][' + key2 + ']',
																'value': data[key][key1][key2]
															})
													);
												}
											}
										}
									}
								}
							}
						}
	
						$(result).appendTo($('form.report-target-form'));
	
						$mainContent.find('div.loading').hide();
						$mainContent.find('div.complete').show();
	
						$mainContent.find('div.complete div.download ul.dropdown-menu a').on('click', function(e) {
							e.preventDefault();
							$('form.report-target-form').prop('action', $(this).data('href')).submit();
						});

						var showReport = function(){
							var table = $('<table/>')
								.addClass('table table-bordered table-condensed table-striped table-sortable')
								.css({
									'width' : 'auto',
									'max-width' : 'none'
								})
								.append($('<thead/>').append(window.ReportBuilder.buildHeader()));

							var rows = $.map(window.ReportBuilder.reportData.students, function(student) {
								return window.ReportBuilder.buildRow(student);
							});
							$(rows).appendTo($('<tbody/>').appendTo(table));

							if (window.ReportBuilder.buildFooter != undefined) {
								table.append($('<tfoot/>').append(window.ReportBuilder.buildFooter()));
							}

							$('.report-target').append(table);
							table.wrap($('<div><div class="sb-wide-table-wrapper"></div></div'));

							$('th.rotated').each(function() {
								var $this = $(this),
									width = $this.find('.rotate').width(),
									height = $this.find('.rotate').height();
								$this.css('height', width + 20).css('width', height + 5);
								$this.find('.rotate').css('margin-top', -(width + 25));
								if ($this.is('.sortable')) {
									$this.css('height', width + 35).css('width', 'auto');
									$this.find('.rotate').css('margin-top', -(width + 40));
								}
							});
							$('td.rotated').each(function() {
								var width = $(this).find('.rotate').width();
								var height = $(this).find('.rotate').height();
								$(this).css('height', width).css('width', height + 5);
								$(this).find('.rotate').css('margin-top', -(height));
							});

							var popoutLinkHandler = function(event) {
								event.stopPropagation();
								event.preventDefault();
								if (!Shadowbox.initialized) {
									Shadowbox.initialized = true;
									Shadowbox.init(shadowboxOptions);
								}
								var tableWrapper = $(this).closest('div').find('div.sb-wide-table-wrapper');
								Shadowbox.open({
									link : this,
									content: '<div class="sb-wide-table-wrapper" style="background: white;">'
									+ tableWrapper.html()
									+ '</div>',
									player: 'html',
									width: $(window).width(),
									height: $(window).height(),
									options: {
										onFinish: function(){
											$('#sb-container').find('.table-sortable')
												.find('th.header').removeClass('header')
												.end().removeClass('tablesorter').sortableTable();
										}
									}
								});
							};

							var generatePopoutLink = function(){
								return $('<span/>')
									.addClass('sb-table-wrapper-popout')
									.append('(')
									.append(
										$('<a/>')
											.prop('href', '#')
											.html('Pop-out table')
											.on('click', popoutLinkHandler)
									).append(')');
							};

							table.parent().parent('div').prepend(generatePopoutLink()).append(generatePopoutLink());
							table.sortableTable();
						};

						$('#main-content').find('div.complete a.show-data').on('click', function(e) {
							e.preventDefault();
							var $this = $(this);
							setTimeout(function(){
								showReport();
								$this.hide();
							}, 500);
						});
					}, 500);
				},
				error: function() {
					$('#main-content').find('div.loading').hide().end().find('div.alert-error').show();
				}
			});
		});
	</script>
</#macro>

<#macro attendanceMonitoringReportScript>
	<script>
		jQuery(function($){
			if (window.ReportBuilder == undefined)
				return false;

			window.ReportBuilder.buildHeader = function(){
				var container = $('<tr/>');
				container.append(
					$('<th/>').addClass('sortable').html('First name')
				).append(
					$('<th/>').addClass('sortable').html('Last name')
				).append(
					$('<th/>').addClass('sortable').html('University ID')
				);
				$.each(window.ReportBuilder.reportData.points, function(i, point){
					container.append(
						$('<th/>').addClass('point rotated').append(
							$('<div/>').addClass('rotate').html(point.name + ' (' + point.intervalString + ')')
						)
					)
				});
				container.append(
					$('<th/>').addClass('sortable').append($('<i/>').addClass('icon-warning-sign icon-fixed-width late').prop('title', 'Unrecorded'))
				).append(
					$('<th/>').addClass('sortable').append($('<i/>').addClass('icon-remove icon-fixed-width unauthorised').prop('title', 'Missed monitoring points'))
				);
				return container;
			};

			window.ReportBuilder.buildRow = function(student) {
				var container = $('<tr/>');
				container.append(
					$('<td/>').html(student.firstName)
				).append(
					$('<td/>').html(student.lastName)
				).append(
					$('<td/>').append(
						$('<a/>').prop({
							'href' : '/profiles/view/' + student.universityId,
							'target' : '_blank'
						}).html(student.universityId)
					)
				);
				var attendance = window.ReportBuilder.reportData.attendance[student.universityId], unrecordedCount = 0, missedCount = 0;
				$.each(window.ReportBuilder.reportData.points, function(i, point){
					if (attendance[point.id] == undefined) {
						container.append($('<td/>').append($('<i/>').addClass('icon-fixed-width')));
					} else {
						if (attendance[point.id] === 'attended') {
							container.append($('<td/>').append($('<i/>').addClass('icon-fixed-width icon-ok attended')));
						} else if (attendance[point.id] === 'authorised') {
							container.append($('<td/>').append($('<i/>').addClass('icon-fixed-width icon-remove-circle authorised')));
						} else if (attendance[point.id] === 'unauthorised') {
							container.append($('<td/>').append($('<i/>').addClass('icon-fixed-width icon-remove unauthorised')));
							missedCount++;
						} else if (point.late == 'true') {
							container.append($('<td/>').append($('<i/>').addClass('icon-fixed-width icon-warning-sign late')));
							unrecordedCount++;
						} else {
							container.append($('<td/>').append($('<i/>').addClass('icon-fixed-width icon-minus unrecorded')));
						}
					}
				});
				container.append(
					$('<td/>').addClass('unrecorded').append(
						$('<span/>').addClass('badge badge-' + ((unrecordedCount > 2) ? 'important' : ((unrecordedCount > 0) ? 'warning' : 'success'))).html(unrecordedCount)
					)
				).append(
					$('<td/>').addClass('missed').append(
						$('<span/>').addClass('badge badge-' + ((missedCount > 2) ? 'important' : ((missedCount > 0) ? 'warning' : 'success'))).html(missedCount)
					)
				);
				return container;
			};
		});
	</script>
</#macro>

<#macro smallGroupReportScript>
	<script>
		jQuery(function($){
			if (window.ReportBuilder == undefined)
				return false;

			window.ReportBuilder.buildHeader = function(){
				var container = $('<tr/>');
				container.append(
					$('<th/>').addClass('sortable').html('First name')
				).append(
					$('<th/>').addClass('sortable').html('Last name')
				).append(
					$('<th/>').addClass('sortable').html('University ID')
				);
				$.each(window.ReportBuilder.reportData.events, function(i, event){
					container.append(
						$('<th/>').addClass('event rotated').append(
							$('<div/>').addClass('rotate').html(
								event.moduleCode + ' '
								+ event.setName + ' '
								+ event.format + ' '
								+ event.groupName + ' '
								+ event.dayString + '  Week ' + event.week
							)
						)
					)
				});
				container.append(
					$('<th/>').addClass('sortable').append($('<i/>').addClass('icon-warning-sign icon-fixed-width late').prop('title', 'Unrecorded'))
				).append(
					$('<th/>').addClass('sortable').append($('<i/>').addClass('icon-remove icon-fixed-width unauthorised').prop('title', 'Missed events'))
				);
				return container;
			};

			window.ReportBuilder.buildRow = function(student) {
				var container = $('<tr/>');
				container.append(
					$('<td/>').html(student.firstName)
				).append(
					$('<td/>').html(student.lastName)
				).append(
					$('<td/>').append(
						$('<a/>').prop({
							'href' : '/profiles/view/' + student.universityId,
							'target' : '_blank'
						}).html(student.universityId)
					)
				);
				var attendance = window.ReportBuilder.reportData.attendance[student.universityId], unrecordedCount = 0, missedCount = 0;
				$.each(window.ReportBuilder.reportData.events, function(i, event){
					if (attendance[event.id] == undefined) {
						container.append($('<td/>').append($('<i/>').addClass('icon-fixed-width')));
					} else {
						if (attendance[event.id] === 'attended') {
							container.append($('<td/>').append($('<i/>').addClass('icon-fixed-width icon-ok attended')));
						} else if (attendance[event.id] === 'authorised') {
							container.append($('<td/>').append($('<i/>').addClass('icon-fixed-width icon-remove-circle authorised')));
						} else if (attendance[event.id] === 'unauthorised') {
							container.append($('<td/>').append($('<i/>').addClass('icon-fixed-width icon-remove unauthorised')));
							missedCount++;
						} else if (event.late == 'true') {
							container.append($('<td/>').append($('<i/>').addClass('icon-fixed-width icon-warning-sign late')));
							unrecordedCount++;
						} else {
							container.append($('<td/>').append($('<i/>').addClass('icon-fixed-width icon-minus unrecorded')));
						}
					}
				});
				container.append(
					$('<td/>').addClass('unrecorded').append(
						$('<span/>').addClass('badge badge-' + ((unrecordedCount > 2) ? 'important' : ((unrecordedCount > 0) ? 'warning' : 'success'))).html(unrecordedCount)
					)
				).append(
					$('<td/>').addClass('missed').append(
						$('<span/>').addClass('badge badge-' + ((missedCount > 2) ? 'important' : ((missedCount > 0) ? 'warning' : 'success'))).html(missedCount)
					)
				);
				return container;
			};

			window.ReportBuilder.buildFooter = function() {
				var container = $('<tr/>');
				container.append(
					$('<th/>').prop('colspan', 3).css('text-align', 'right').html('Tutor/s')
				);
				$.each(window.ReportBuilder.reportData.events, function(i, event){
					container.append(
						$('<td/>').addClass('tutors rotated').append(
							$('<div/>').addClass('rotate').html(event.tutors)
						)
					)
				});
				container.append($('<td/>')).append($('<td/>'));
				return container;
			}
		});
	</script>
</#macro>

<#macro smallGroupByModuleReportScript>
<script>
	jQuery(function($){
		if (window.ReportBuilder == undefined)
			return false;

		window.ReportBuilder.buildHeader = function(){
			var container = $('<tr/>');
			container.append(
				$('<th/>').addClass('sortable').html('First name')
			).append(
				$('<th/>').addClass('sortable').html('Last name')
			).append(
				$('<th/>').addClass('sortable').html('University ID')
			);
			$.each(window.ReportBuilder.reportData.modules, function(i, module){
				container.append(
					$('<th/>').addClass('module rotated sortable').append(
						$('<div/>').addClass('rotate').html(module.code.toUpperCase() + ' ' + module.name)
					)
				)
			});
			return container;
		};

		window.ReportBuilder.buildRow = function(student) {
			var container = $('<tr/>');
			container.append(
				$('<td/>').html(student.firstName)
			).append(
				$('<td/>').html(student.lastName)
			).append(
				$('<td/>').append(
					$('<a/>').prop({
						'href' : '/profiles/view/' + student.universityId,
						'target' : '_blank'
						}).html(student.universityId)
				)
			);
			var counts = window.ReportBuilder.reportData.counts[student.universityId];
			$.each(window.ReportBuilder.reportData.modules, function(i, module){
				if (counts[module.id] == undefined) {
					container.append($('<td/>').append($('<i/>').addClass('icon-fixed-width')));
				} else {
					container.append(
						$('<td/>').addClass('unrecorded').append(
							$('<span/>').addClass('badge badge-' + ((counts[module.id] > 2) ? 'important' : ((counts[module.id] > 0) ? 'warning' : 'success'))).html(counts[module.id])
						)
					);
				}
			});
			return container;
		};
	});
</script>
</#macro>