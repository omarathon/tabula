<#escape x as x?html>

<h1>All attendance</h1>

<div class="loading">
	<p><em>Building report...</em></p>

	<div class="progress">
		<div class="bar" style="width: 33%;"></div>
	</div>
</div>

<div class="complete alert alert-success" style="display: none;">
	<p>Report complete</p>
	<div class="btn-toolbar">
		<a href="#" class="show-data btn" data-loading-text="Loading&hellip;" ><i class="icon-eye-open"></i> Show</a>
		<div class="download btn-group ">
			<a href="#" class="btn dropdown-toggle" data-toggle="dropdown">
				<i class="icon-download"></i> Download&hellip;
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu">
				<li>
					<a href="#" data-href="<@routes.allAttendanceDownloadCsv department academicYear />">
						<i class="icon-table"></i> CSV
					</a>
				</li>
				<li>
					<a href="#" data-href="<@routes.allAttendanceDownloadXlsx department academicYear />">
						<i class="icon-list-alt"></i> Excel
					</a>
				</li>
				<li>
					<a href="#" data-href="<@routes.allAttendanceDownloadXml department academicYear />">
						<i class="icon-code"></i> XML
					</a>
				</li>
			</ul>
		</div>
	</div>
</div>

<div class="alert alert-error" style="display: none;">
	<p>There was a problem generating the report. If the problem persists, please contact the <a href="mailto:webteam@warwick.ac.uk">ITS Web Team</a>.</p>
</div>

<div class="report-target"></div>
<form class="report-target-form" style="display: none;" method="POST" action=""></form>

<script>
	jQuery(function($){
		$.ajax('<@routes.allAttendance department academicYear />', {
			type: 'POST',
			success: function(data) {
				var $mainContent = $('#main-content');
				$mainContent.find('.loading p em').html('Downloading data&hellip;');
				$mainContent.find('.loading .bar').width('66%');
				setTimeout(function(){
					var student, point, prop, result = [];
					for (student in data.result) {
						if (data.result.hasOwnProperty(student)) {
							for (point in data.result[student]) {
								if (data.result[student].hasOwnProperty(point)) {
									result.push(
										$('<input/>').prop({
											'type':'hidden',
											'name':'result[' + student + '][' + point + ']',
											'value':data.result[student][point]
										})
									);
								}
							}
						}
					}

					for (student in data.students) {
						if (data.students.hasOwnProperty(student)) {
							for (prop in data.students[student]) {
								if (data.students[student].hasOwnProperty(prop)) {
									result.push(
										$('<input/>').prop({
											'type':'hidden',
											'name':'students[' + student + '][' + prop + ']',
											'value':data.students[student][prop]
										})
									);
								}
							}
						}
					}

					for (point in data.points) {
						if (data.points.hasOwnProperty(point)) {
							for (prop in data.points[point]) {
								if (data.points[point].hasOwnProperty(prop)) {
									result.push(
										$('<input/>').prop({
											'type':'hidden',
											'name':'points[' + point + '][' + prop + ']',
											'value':data.points[point][prop]
										})
									);
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
					$mainContent.find('div.complete a.show-data').on('click', function(e) {
						e.preventDefault();
						var $this = $(this);
						$.post('<@routes.allAttendanceShow department academicYear />', data, function(html) {
							$('.report-target').html(html);
							$this.hide();
						});
					});
				}, 500);
			},
			error: function() {
				$('#main-content').find('div.loading').hide().end().find('div.alert-error').show();
			}
		});
	});
</script>

</#escape>