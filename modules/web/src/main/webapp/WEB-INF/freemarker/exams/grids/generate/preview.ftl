<#escape x as x?html>

<#function route_function dept>
	<#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

<form action="<@routes.exams.generateGrid department academicYear />" class="dirty-check" method="post">

	<input type="hidden" name="course" value="${selectCourseCommand.course.code}" />
	<input type="hidden" name="route" value="${selectCourseCommand.route.code}" />
	<input type="hidden" name="yearOfStudy" value="${selectCourseCommand.yearOfStudy}" />
	<#list columnIDs as column>
		<input type="hidden" name="predefinedColumnIdentifiers" value="${column}" />
	</#list>

	<h2>Preview and download</h2>

	<p class="progress-arrows">
		<span class="arrow-right"><button type="submit" class="btn btn-link">Select courses</button></span>
		<span class="arrow-right arrow-left"><button type="submit" class="btn btn-link" name="${GenerateExamGridMappingParameters.selectCourse}">Set grid options</button></span>
		<span class="arrow-right arrow-left active">Preview and download</span>
	</p>

	<div class="alert alert-info">
		<h3>Your grid</h3>
		<p>
			Your grid was generated using the latest data from SITS. If you think the data is inaccurate, please verify the data in SITS.
			Also, if data changes in SITS after you have downloaded the grid, Tabula will not reflect the changes until you generate the grid again.
		</p>
	</div>

	<div class="key">
		<dl>
			<dt>Department:</dt>
			<dd>${department.name}</dd>
			<dt>Course:</dt>
			<dd>${selectCourseCommand.course.code}</dd>
			<dt>Student count:</dt>
			<dd>${scyds?size}</dd>
		</dl>

		<table class="table table-condensed table-striped">
			<thead>
				<tr>
					<th colspan="2">Key</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="exam-grid-fail">#</td>
					<td>Failed module</td>
				</tr>
				<tr>
					<td class="exam-grid-overcat">#</td>
					<td>Used in overcatting calculation</td>
				</tr>
			</tbody>
		</table>
	</div>

	<table class="table table-condensed table-striped">
		<tbody>
			<#if hasCategoryRow>
				<tr></tr>
			</#if>
			<tr>
				<#list columns as columnSet>
					<#list columnSet as column>
						<#if !column.category?has_content>
							<th>${column.title}</th>
						</#if>
					</#list>
				</#list>
			</tr>
			<#list scyds as scyd>
				<tr>
					<#list columnValues as columnSet>
						<#list columnSet as column>
							<#if column[scyd.id]?has_content>
								<td>${column[scyd.id]}</td>
							</#if>
						</#list>
					</#list>
				</tr>
			</#list>
		</tbody>
	</table>

	<button class="btn btn-primary" type="submit" name="${GenerateExamGridMappingParameters.export}">Download for Excel</button>
</form>

</#escape>