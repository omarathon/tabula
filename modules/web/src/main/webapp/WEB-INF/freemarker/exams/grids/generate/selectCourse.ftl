<#escape x as x?html>

<#function route_function dept>
	<#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

<h2>Select courses for this grid</h2>

<p class="progress-arrows">
	<span class="arrow-right active">Select courses</span>
	<span class="arrow-right arrow-left">Set grid options</span>
	<span class="arrow-right arrow-left">Preview and download</span>
</p>

<div class="alert alert-info">
	<h3>Before you start</h3>
	<p>Exam grids in Tabula are generated using data stored in SITS.
		Before you create a new grid, ensure you have entered all the necessary data in SITS and verified its accuracy.</p>
</div>

<form action="<@routes.exams.generateGrid department academicYear />" class="form-inline select-course" method="post">

	<div class="well well-sm">
		<div class="row">
			<div class="form-group col-md-4">
				<select name="course" class="form-control">
					<option value="" style="display: none">Course</option>
					<#list selectCourseCommand.courses as course>
						<option value="${course.code}" <#if (selectCourseCommand.course.code)! == course.code>selected</#if>>${course.code} ${course.name}</option>
					</#list>
				</select>
			</div>
			<div class="form-group col-md-4">
				<select name="route" class="form-control" disabled>
					<option value="" style="display: none">Route</option>
					<#list selectCourseCommand.routes as route>
						<option value="${route.code}" <#if (selectCourseCommand.route.code)! == route.code>selected</#if>>${route.code?upper_case} ${route.name}</option>
					</#list>
				</select>
			</div>
			<div class="form-group col-md-4">
				<select name="yearOfStudy" class="form-control" disabled>
					<option value="" style="display: none">Year of study</option>
					<#list selectCourseCommand.yearsOfStudy as year>
						<option value="${year}" <#if (selectCourseCommand.yearOfStudy!0) == year>selected</#if>>${year}</option>
					</#list>
				</select>
			</div>
		</div>
	</div>

	<@bs3form.errors path="selectCourseCommand" />

	<button class="btn btn-primary" name="${GenerateExamGridMappingParameters.selectCourse}" type="submit" disabled>Next</button>
</form>

<script>
	jQuery(function($){
		$('.select-course').on('change', function(){
			var $courseSelect = $('select[name=course]')
				, $routeSelect = $('select[name=route]')
				, $yearSelect = $('select[name=yearOfStudy]')
				, $submit = $('button.btn-primary');
			if (($courseSelect.val() || '').length > 0) {
				$routeSelect.prop('disabled', false);
			}
			if (($routeSelect.val() || '').length > 0) {
				$yearSelect.prop('disabled', false);
			}
			if (($yearSelect.val() || '').length > 0) {
				$submit.prop('disabled', false);
			}
		}).trigger('change');
	});
</script>
</#escape>