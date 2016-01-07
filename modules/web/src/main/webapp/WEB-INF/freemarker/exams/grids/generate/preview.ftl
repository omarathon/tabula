<#escape x as x?html>

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
			</tbody>
		</table>
	</div>

	<table class="table table-condensed grid">
		<tbody>
			<#if categories?keys?has_content>
				<tr class="category">
					<#assign currentSection = "" />
					<#assign currentCategory = "" />
					<#list columns as column>
						<#if column.sectionIdentifier?has_content && currentSection != column.sectionIdentifier>
							<#assign currentSection = column.sectionIdentifier />
							<td class="borderless"></td>
						</#if>
						<#if column.category?has_content>
							<#if currentCategory != column.category>
								<#assign currentCategory = column.category />
								<th class="rotated first-in-category" colspan="${categories[column.category]?size}"><div class="rotate">${column.category}</div></th>
							</#if>
						<#else>
							<td class="borderless"></td>
						</#if>
					</#list>
				</tr>
				<tr class="title-in-category">
					<#assign currentSection = "" />
					<#assign currentCategory = "" />
					<#list columns as column>
						<#if column.sectionTitleLabel?has_content>
							<#if currentSection != column.sectionIdentifier>
								<#assign currentSection = column.sectionIdentifier />
								<th>${column.sectionTitleLabel}</th>
							</#if>
						</#if>
						<#if column.category?has_content>
							<#if currentCategory != column.category>
								<#assign firstInCategory = true />
								<#assign currentCategory = column.category />
							<#else>
								<#assign firstInCategory = false />
							</#if>
							<td class="rotated <#if firstInCategory!false>first-in-category</#if>"><div class="rotate">${column.title}</div></td>
						<#else>
							<td class="borderless"></td>
						</#if>
					</#list>
				</tr>
			</#if>
			<tr>
				<#assign currentSection = "" />
				<#assign currentCategory = "" />
				<#list columns as column>
					<#if column.sectionSecondaryValueLabel?has_content && currentSection != column.sectionIdentifier>
						<#assign currentSection = column.sectionIdentifier />
						<th class="section-secondary-label">${column.sectionSecondaryValueLabel}</th>
					</#if>
					<#if column.category?has_content && currentCategory != column.category>
						<#assign firstInCategory = true />
						<#assign currentCategory = column.category />
					<#else>
						<#assign firstInCategory = false />
					</#if>
					<#if !column.category?has_content>
						<th>${column.title}</th>
					<#elseif column.renderSecondaryValue?has_content>
						<td <#if firstInCategory!false>class="first-in-category"</#if>><#noescape>${column.renderSecondaryValue}</#noescape></td>
					<#else>
						<td <#if firstInCategory!false>class="first-in-category"</#if>></td>
					</#if>
				</#list>
			</tr>
			<#list scyds as scyd>
				<tr class="student">
					<#assign currentSection = "" />
					<#assign currentCategory = "" />
					<#list columnValues as columnValue>
						<#assign column = columns[columnValue_index] />
						<#if column.sectionValueLabel?has_content && currentSection != column.sectionIdentifier && scyd_index == 0>
							<#assign currentSection = column.sectionIdentifier />
							<th rowspan="${scyds?size}" class="section-value-label">${column.sectionValueLabel}</th>
						</#if>
						<#if column.category?has_content && currentCategory != column.category>
							<#assign firstInCategory = true />
							<#assign currentCategory = column.category />
						<#else>
							<#assign firstInCategory = false />
						</#if>
						<td <#if firstInCategory!false>class="first-in-category"</#if>>
							<#if columnValue[scyd.id]?has_content><#noescape>${columnValue[scyd.id]}</#noescape></#if>
						</td>
					</#list>
				</tr>
			</#list>
		</tbody>
	</table>

	<button class="btn btn-primary" type="submit" name="${GenerateExamGridMappingParameters.export}">Download for Excel</button>
</form>

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
	});
</script>

</#escape>