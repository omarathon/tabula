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
	<p>
		<a href="#" class="show-data btn" data-loading-text="Loading&hellip;" ><i class="icon-eye-open"></i> Show</a>
		<a href="#" class="download btn"><i class="icon-download"></i> Download CSV</a>
	</p>
</div>

<div class="alert alert-error" style="display: none;">
	<p>There was a problem generating the report. If the problem persists, please contact the <a href="mailto:webteam@warwick.ac.uk">ITS Web Team</a>.</p>
</div>

<div class="report-target"></div>
<form class="report-target-form" style="display: none;" method="POST" action="<@routes.allAttendanceDownload department academicYear />"></form>

<script>
	jQuery(function($){
		$.ajax('<@routes.allAttendance department academicYear />', {
			type: 'POST',
			success: function(data) {
				var $mainContent = $('#main-content');
				$mainContent.find('.loading p em').html('Downloading data&hellip;');
				$mainContent.find('.loading .bar').width('66%');
				setTimeout(function(){
					var student, point, prop;
					for (student in data.result) {
						if (data.result.hasOwnProperty(student)) {
							for (point in data.result[student]) {
								if (data.result[student].hasOwnProperty(point)) {
									$('form.report-target-form').append(
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
									$('form.report-target-form').append(
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
									$('form.report-target-form').append(
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

					$mainContent.find('div.loading').hide();
					$mainContent.find('div.complete').show();

					$mainContent.find('div.complete a.download').on('click', function(e) {
						e.preventDefault();
						$('form.report-target-form').submit();
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