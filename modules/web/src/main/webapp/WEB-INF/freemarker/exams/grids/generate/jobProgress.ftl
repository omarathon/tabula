<#escape x as x?html>

<#function route_function dept>
	<#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

<form action="<@routes.exams.generateGrid department academicYear />" class="dirty-check" method="post">

	<input type="hidden" name="jobId" value="${jobId}" />
	<input type="hidden" name="course" value="${selectCourseCommand.course.code}" />
	<input type="hidden" name="route" value="${selectCourseCommand.route.code}" />
	<input type="hidden" name="yearOfStudy" value="${selectCourseCommand.yearOfStudy}" />
	<#list columnIDs as column>
		<input type="hidden" name="predefinedColumnIdentifiers" value="${column}" />
	</#list>

	<h2>Importing student data</h2>

	<p class="progress-arrows">
		<span class="arrow-right"><button type="submit" class="btn btn-link">Select courses</button></span>
		<span class="arrow-right arrow-left"><button type="submit" class="btn btn-link" name="${GenerateExamGridMappingParameters.selectCourse}">Set grid options</button></span>
		<span class="arrow-right arrow-left active">Preview and download</span>
	</p>

	<div class="alert alert-info">
		<div class="progress">
			<div class="progress-bar progress-bar-striped active" style="width: ${jobProgress}%;"></div>
		</div>
		<p class="job-status">${jobStatus}</p>
	</div>

	<p>
		Tabula is currently importing fresh data for the students you selected from SITS.
		If you wish you can skip this import and proceed to generate the grid.
	</p>

	<button class="btn btn-primary" type="submit" name="${GenerateExamGridMappingParameters.previewAndDownload}">Skip import and generate grid</button>
</form>

<script>
	jQuery(function($){
		var updateProgress = function(){
			$.post('<@routes.exams.generateGridProgress department academicYear />', {'jobId': '${jobId}'}, function(data){
				if (data.finished) {
					$('button[name="${GenerateExamGridMappingParameters.previewAndDownload}"]').trigger('click');
				} else {
					if (data.progress) {
						$('.progress .progress-bar').css('width', data.progress + '%');
					}
					if (data.status) {
						$('.job-status').html(data.status);
					}
					setTimeout(updateProgress, 5 * 1000);
				}
			});
		};
		updateProgress();
	});
</script>

</#escape>