<#escape x as x?html>
<#import "grid_macros.ftl" as grid />

<#function route_function dept>
	<#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

<form action="<@routes.exams.generateGrid department academicYear />" class="dirty-check exam-grid-preview" method="post">

	<input type="hidden" name="course" value="${selectCourseCommand.course.code}" />
	<input type="hidden" name="route" value="${selectCourseCommand.route.code}" />
	<input type="hidden" name="yearOfStudy" value="${selectCourseCommand.yearOfStudy}" />
	<#list gridOptionsCommand.predefinedColumnIdentifiers as column>
		<input type="hidden" name="predefinedColumnIdentifiers" value="${column}" />
	</#list>
	<#list gridOptionsCommand.customColumnTitles as column>
		<input type="hidden" name="customColumnTitles[${column_index}]" value="${column}" />
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
		<table class="table table-condensed">
			<tbody>
				<tr>
					<th>Department:</th>
					<td>${department.name}</td>
				</tr>
				<tr>
					<th>Course:</th>
					<td>${selectCourseCommand.course.code}</td>
				</tr>
				<tr>
					<th>Year of study:</th>
					<td>${selectCourseCommand.yearOfStudy}</td>
				</tr>
				<tr>
					<th>Student Count:</th>
					<td>${scyds?size}</td>
				</tr>
				<tr>
					<th>Grid Generated:</th>
					<td><@fmt.date date=generatedDate relative=false /></td>
				</tr>
			</tbody>
		</table>

		<table class="table table-condensed">
			<thead>
				<tr>
					<th colspan="2">Key</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td><span class="exam-grid-fail">#</span></td>
					<td>Failed module</td>
				</tr>
				<tr>
					<td><span class="exam-grid-overcat">#</span></td>
					<td>Used in overcatting calculation</td>
				</tr>
				<tr>
					<td><span class="exam-grid-override">#</span></td>
					<td>Manually adjusted and not stored in SITS</td>
				</tr>
			</tbody>
		</table>
	</div>

	<table class="table table-condensed grid">
		<tbody>
			<#if categories?keys?has_content>
				<tr class="category">
					<@grid.categoryRow categories columns />
				</tr>
				<tr class="title-in-category">
					<@grid.titleInCategoryRow categories columns />
				</tr>
			</#if>
			<tr>
				<@grid.headerRow columns />
			</tr>
			<#list scyds as scyd>
				<tr class="student">
					<#assign isFirstSCYD = scyd_index == 0 />
					<@grid.entityRows scyd isFirstSCYD scyds?size columns columnValues />
				</tr>
			</#list>
		</tbody>
	</table>

	<button class="btn btn-primary" type="submit" name="${GenerateExamGridMappingParameters.export}">Download for Excel</button>
</form>

<div class="modal fade" id="edit-overcatting-modal"></div>

<script>
	jQuery(function($){
		$('th.rotated, td.rotated').each(function() {
			var width = $(this).find('.rotate').width();
			var height = $(this).find('.rotate').height();
			$(this).css('height', width + 15).css('width', height + 5);
			$(this).find('.rotate').css({
				'margin-top': -(height),
				'margin-left': height / 2
			});
		});

		$('button.edit-overcatting').each(function(){
			$(this).attr('href', '<@routes.exams.generateGrid department academicYear />/overcatting/' + $(this).data('student'))
				.data('target', '#edit-overcatting-modal');

		}).ajaxModalLink();
	});
</script>

</#escape>