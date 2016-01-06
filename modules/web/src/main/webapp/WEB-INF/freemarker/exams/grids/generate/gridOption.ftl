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

	<h2>Set grid options</h2>

	<p class="progress-arrows">
		<span class="arrow-right"><button type="submit" class="btn btn-link">Select courses</button></span>
		<span class="arrow-right arrow-left active">Set grid options</span>
		<span class="arrow-right arrow-left">Preview and download</span>
	</p>

	<p>Select the items to include in your grid</p>

	<h3>Student identification</h3>

	<div class="row">
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="universityId"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("universityId")>checked</#if>
				/> University ID</label>
			</div>
		</div>

		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="name"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("name")>checked</#if>
				/> Official name</label>
			</div>
		</div>
	</div>

	<h3>Modules</h3>

	<div class="row">
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="core"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("core")>checked</#if>
				/> Core Modules</label>
			</div>
		</div>
	</div>

	<@bs3form.errors path="gridOptionsCommand" />

	<button class="btn btn-primary" type="submit" name="${GenerateExamGridMappingParameters.gridOptions}">Next</button>
</form>

</#escape>