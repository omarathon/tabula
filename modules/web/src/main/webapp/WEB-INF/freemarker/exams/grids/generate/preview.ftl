<#escape x as x?html>
<#import "grid_macros.ftl" as grid />

<#function route_function dept>
	<#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

<div class="fix-area">

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

		<div class="alert alert-info">
			<h3>Over catted marks</h3>
			<p>For students who have elected to overcat you will need to review the marks generated and choose the best mark that meets all of your course regulations.</p>
			<p>'Select Edit' to add the best mark. If you download the grid without adding these marks these marks will remain blank. <a href="#" class="show-more">Show More</a></p>
			<div class="more hidden">
				<p>Each course normally consists of modules each with a CATs score and to pass that course you must achieve the minimum number of CATs.</p>
				<p>
					The University allows students to study additional modules, if they so wish for their own education and to potentially achieve a higher mark.
					Any student who studies more than the normal load of CATs has been deemed to have over-catted.
				</p>
				<p>
					So that no student is ever disadvantaged by overcatting the calculation works out the highest scoring combination of modules from those the student has taken.
					If this is higher than the mean module mark, it will be the mark they are awarded, as long as the combination satisfies the course regulations.
				</p>
				<p>
					The regulations governing each course vary widely but always have a minimum number of CATS and the maximum number of CATS to be taken.
					Usually a course is made up of sets of modules from which the student must pick modules.
					Some courses may not allow certain combinations of modules to be taken in a year or over several years.
				</p>
				<p>
					Because of the variety of valid combinations possible it is not currently possible for Tabula to be sure that the final over-catted mark it derives complies with the regulations,
					and we ask the exam board to choose the final over-catted mark to ensure that it meets with all the regulations. <a href="#" class="show-less">Less</a>
				</p>
			</div>

		</div>

		<div class="key clearfix">
			<table class="table table-condensed">
				<thead>
					<tr>
						<th colspan="2">Report</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<th>Department:</th>
						<td>${department.name}</td>
					</tr>
					<tr>
						<th>Course:</th>
						<td>${selectCourseCommand.course.code?upper_case} ${selectCourseCommand.course.name}</td>
					</tr>
					<tr>
						<th>Route:</th>
						<td>${selectCourseCommand.route.code?upper_case} ${selectCourseCommand.route.name}</td>
					</tr>
					<tr>
						<th>Year of study:</th>
						<td>${selectCourseCommand.yearOfStudy}</td>
					</tr>
					<tr>
						<th>Year weightings:</th>
						<td>
							<#list weightings as weighting>
								Year ${weighting.yearOfStudy} = ${weighting.weightingAsPercentage}<#if weighting_has_next><br /></#if>
							</#list>
						</td>
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

		<div class="fix-footer">
			<button class="btn btn-primary" type="submit" name="${GenerateExamGridMappingParameters.export}">Download for Excel</button>
		</div>
	</form>

</div>

<div class="modal fade" id="edit-overcatting-modal"></div>

<script>
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();

		$('th.first-in-category, td.first-in-category').each(function(){
			$(this).prev().addClass('last-in-category');
		});
		$('th.rotated, td.rotated').each(function() {
			var width = $(this).find('.rotate').width();
			var height = $(this).find('.rotate').height();
			$(this).css('height', width + 15).css('min-width', height + 5);
			$(this).find('.rotate').css({
				'margin-left': height / 2
			}).not('.nomargin').css({
				'margin-top': -(height)
			}).end().filter('.middle').not('.nomargin').css({
				'margin-top': width / 4
			});
		});

		$('button.edit-overcatting').each(function(){
			$(this).attr('href', '<@routes.exams.generateGrid department academicYear />/overcatting/' + $(this).data('student'))
				.data('target', '#edit-overcatting-modal');

		}).ajaxModalLink();

		$('a.show-more').on('click', function(e){
			e.preventDefault();
			$(this).parent().next('.more').removeClass('hidden').end().end()
				.hide();
		});
		$('a.show-less').on('click', function(e){
			e.preventDefault();
			$(this).closest('.more').addClass('hidden').parent().find('a.show-more').show();
		});
	});
</script>

</#escape>