<#import 'form_fields.ftl' as form_fields />
<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>

<#function route_function dept>
	<#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

<form action="<@routes.exams.generateGrid department academicYear />" class="dirty-check grid-options" method="post">

	<@form_fields.select_course_fields />

	<h2>Set grid options</h2>

	<p class="progress-arrows">
		<span class="arrow-right"><button type="submit" class="btn btn-link">Select courses</button></span>
		<span class="arrow-right arrow-left active">Set grid options</span>
		<span class="arrow-right arrow-left">Preview and download</span>
	</p>

	<p>
		Select the items to include in your grid for
		<#if selectCourseCommand.courses?size == 1>
			Course: ${selectCourseCommand.courses?first.code?upper_case} ${selectCourseCommand.courses?first.name}
				<#else>
			Courses:
					<#assign popover>
				<ul><#list selectCourseCommand.courses?sort_by('code') as course>
					<li>${course.code?upper_case} ${course.name}</li>
				</#list></ul>
					</#assign>
			<a class="use-popover" href="#" data-html="true" data-content="${popover}" data-container="body">${selectCourseCommand.courses?size} courses</a>
		</#if>,
		Year of Study: ${selectCourseCommand.yearOfStudy},
		<#if !selectCourseCommand.routes?has_content>
			All routes
		<#elseif selectCourseCommand.routes?size == 1>
			Route: ${selectCourseCommand.routes?first.code?upper_case} ${selectCourseCommand.routes?first.name}
		<#else>
			Routes:
			<#assign popover>
				<ul><#list selectCourseCommand.routes?sort_by('code') as route>
					<li>${route.code?upper_case} ${route.name}</li>
				</#list></ul>
			</#assign>
			<a class="use-popover" href="#" data-html="true" data-content="${popover}" data-container="body">${selectCourseCommand.routes?size} routes</a>
		</#if>
	</p>

	<h3>Grid layout <@fmt.help_popover id="gridlayout" content="<dl><dt>Short grid</dt><dd>This layout shows only those modules taken by each student.</dd><br><dt>Full grid </dt><dd>This layout shows all modules taken by the cohort.</dd></dl>" html=true/></h3>

	<div class="row">
		<div class="col-md-3">
			<div class="radio">
				<label><input type="radio" name="layout" value="short"
					<#if gridOptionsCommand.layout == 'short'>checked</#if>
				/> Short grid &nbsp;&nbsp;<a href="#short-form-layout-example" data-toggle="modal">Example</a></label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="radio">
				<label><input type="radio" name="layout" value="full"
					<#if gridOptionsCommand.layout == 'full'>checked</#if>
				/> Full grid &nbsp;&nbsp;<a href="#full-layout-example" data-toggle="modal">Example</a></label>
			</div>
		</div>
	</div>

	<div id="short-form-layout-example" class="modal fade">
		<@modal.wrapper>
			<@modal.header><h3 class="modal-title">Short grid</h3></@modal.header>
			<@modal.body>
				<p>This layout shows only those modules taken by each student.</p>
				<img src="<@url resource="/static/images/examgrids/short-grid.png"/>">
			</@modal.body>
		</@modal.wrapper>
	</div>

	<div id="full-layout-example" class="modal fade">
		<@modal.wrapper>
			<@modal.header><h3 class="modal-title">Full grid</h3></@modal.header>
			<@modal.body>
				<p>This layout shows all modules taken by the cohort.</p>
				<img src="<@url resource="/static/images/examgrids/full-grid.png"/>">
			</@modal.body>
		</@modal.wrapper>
	</div>


	<h3>Student identification</h3>

	<div class="row">
		<div class="col-md-3">
			<div class="radio">
				<label><input type="radio" name="nameToShow" value="full"
					<#if gridOptionsCommand.nameToShow.toString == 'full'>checked</#if>
				/> Official name</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="radio">
				<label><input type="radio" name="nameToShow" value="both"
					<#if gridOptionsCommand.nameToShow.toString == 'both'>checked</#if>
				/> First and last name</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="radio">
				<label><input type="radio" name="nameToShow" value="none"
					<#if gridOptionsCommand.nameToShow.toString == 'none'>checked</#if>
				/> No name</label>
			</div>
		</div>
	</div>
	<div class="row">
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="universityId" checked disabled
				/> University ID</label>
			</div>
		</div>

		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="sprCode"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("sprCode")>checked</#if>
				/> SPR code</label>
			</div>
		</div>

		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="course"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("course")>checked</#if>
				/> Course</label>
			</div>
		</div>

		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="route"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("route")>checked</#if>
				/> Route</label>
			</div>
		</div>

		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="startyear"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("startyear")>checked</#if>
				/> Start Year</label>
			</div>
		</div>
	</div>

	<hr />

	<h3>Years</h3>

	<div class="row">
		<div class="col-md-3">
			<div class="radio">
				<label><input type="radio" name="yearsToShow" value="current"
					<#if gridOptionsCommand.yearsToShow == 'current'>checked</#if>
				/> Current year</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="radio">
				<label><input type="radio" name="yearsToShow" value="all"
					<#if gridOptionsCommand.yearsToShow == 'all'>checked</#if>
				/> All years</label>
			</div>
		</div>
	</div>

	<hr />

	<h3>Modules</h3>

	<div class="row">
		<div class="col-md-3">
			<div class="radio">
				<label><input type="radio" name="marksToShow" value="all"
					<#if gridOptionsCommand.marksToShow == 'all'>checked</#if>
				/> Show component marks</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="radio">
				<label><input type="radio" name="marksToShow" value="overall"
					<#if gridOptionsCommand.marksToShow == 'overall'>checked</#if>
				/> Only show overall mark</label>
			</div>
		</div>
	</div>

	<div class="row">
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="core" checked disabled
				/> Core Modules</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="corerequired"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("corerequired")>checked</#if>
				/> Core Required Modules</label>
				<p class="help-block">(You can confirm these in the next step)</p>
			</div>
		</div>
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="coreoptional"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("coreoptional")>checked</#if>
				/> Core Optional Modules</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="optional"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("optional")>checked</#if>
				/> Optional Modules</label>
			</div>
		</div>
	</div>

	<div class="row">

		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="moduleNameToShow" value="nameAndCode"
					<#if gridOptionsCommand.moduleNameToShow == 'nameAndCode'>checked</#if>
				/> Show module names</label>
			</div>
		</div>


		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="modulereports"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("modulereports")>checked</#if>
				/> Module Reports</label>
			</div>
		</div>
	</div>

	<hr />

	<h3>CATS breakdowns <@fmt.help_popover id="catsbreakdowns" content="CATS totals across all modules that scored above / below given thresholds" /></h3>

	<div class="row">
		<div class="col-md-1">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="30cats"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("30cats")>checked</#if>
				/> <=30</label>
			</div>
		</div>
		<div class="col-md-1">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="40cats"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("40cats")>checked</#if>
				/> >=40</label>
			</div>
		</div>
		<div class="col-md-1">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="50cats"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("50cats")>checked</#if>
				/> >=50</label>
			</div>
		</div>
		<div class="col-md-1">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="60cats"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("60cats")>checked</#if>
				/> >=60</label>
			</div>
		</div>
		<div class="col-md-1">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="70cats"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("70cats")>checked</#if>
				/> >=70</label>
			</div>
		</div>
		<div class="col-md-1">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="totalCats"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("totalCats")>checked</#if>
				/> Total <@fmt.help_popover id="totalCats" content="Number of CATS taken by a student." /></label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="passedCats"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("passedCats")>checked</#if>
				/> Passed CATS <@fmt.help_popover id="passedCats" content="Total CATS scored from modules passed." /></label>
			</div>
		</div>
	</div>

	<hr />

	<h3>Marking</h3>

	<div class="row">
		<div class="col-md-3">
			<div class="radio">
				<label><input type="radio" name="yearMarksToUse" value="sits"
					<#if gridOptionsCommand.yearMarksToUse == 'sits'>checked</#if>
				/> Uploaded year marks</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="radio">
				<label><input type="radio" name="yearMarksToUse" value="calculated"
					<#if gridOptionsCommand.yearMarksToUse == 'calculated'>checked</#if>
				/> Calculate year marks</label>
			</div>
		</div>
	</div>

	<div class="row">
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="previous"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("previous")>checked</#if>
				/> Marks from previous year(s)</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="currentyear" checked disabled
				/> Current year mean mark <@fmt.help_popover id="currentyear" content="Year mark calculated from module marks using CATS weighting." /></label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="overcatted"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("overcatted")>checked</#if>
				/> Overcatted year mark <@fmt.help_popover id="overcatted" content="Result and best mark from applying the overcatting calculation." /></label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="board"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("board")>checked</#if>
				/> Board agreed mark <@fmt.help_popover id="board" content="Agreed mark from SITS after the exam board has taken place." /></label>
			</div>
		</div>
	</div>

	<div class="row">
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="finalOverallMark"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("finalOverallMark")>checked</#if>
				/> Final overall mark <@fmt.help_popover id="finalOverallMark" content="For final-year students, the average mark calculated according to year weighting." /></label>
			</div>
		</div>
	</div>

	<hr />

	<h3>Suggested Actions</h3>

	<div class="row">
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="suggestedresult"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("suggestedresult")>checked</#if>
				/> Suggested result <@fmt.help_popover id="suggestedresult" content="Suggested course of action: proceed, resit or pass." /></label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="suggestedgrade"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("suggestedgrade")>checked</#if>
				/> Suggested final year grade <@fmt.help_popover id="suggestedgrade" content="Suggested degree class for final year students." /></label>
			</div>
		</div>
	</div>

	<hr />

	<h3>Administration</h3>

	<div class="row">
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="mitigating"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("mitigating")>checked</#if>
				/> Mitigating Circumstances</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="comments"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("comments")>checked</#if>
				/> Comments</label>
			</div>
		</div>
	</div>

	<hr />

	<h3>Additional columns</h3>

	<div class="customColumnTitles">
		<#list gridOptionsCommand.customColumnTitles as customColumnTitle>
			<div class="row form-group customColumnTitle">
				<div class="col-md-4">
					<input type="hidden" name="customColumnTitles[${customColumnTitle_index}]" value="${customColumnTitle}" />
					${customColumnTitle}
				</div>
				<div class="col-md-2"><button class="btn btn-danger">Delete</button></div>
			</div>
		</#list>

		<div class="well well-sm">
			<div class="row">
				<div class="col-md-4"><input class="form-control" placeholder="Enter a column name" /></div>
				<div class="col-md-2"><button type="button" class="btn btn-default">Add</button></div>
			</div>
		</div>
	</div>

	<@bs3form.errors path="gridOptionsCommand" />

	<button class="btn btn-primary" type="submit" name="${GenerateExamGridMappingParameters.gridOptions}">Next</button>
</form>

<script>
	jQuery(function($){
		var fixCustomColumnIndexes = function(){
			$('.customColumnTitle').each(function(index){
				$(this).find('input').attr('name', 'customColumnTitles[' + index + ']');
			});
		};
		$('.customColumnTitles').on('click', 'button.btn-danger', function(){
			$(this).closest('.row').remove();
			fixCustomColumnIndexes();
		}).on('click', 'button.btn-default', function(){
			var newTitle = $(this).closest('.row').find('input').val();
			$('<div/>').addClass('row form-group customColumnTitle').append(
				$('<div/>').addClass('col-md-4').append(
					newTitle
				).append(
					$('<input/>').attr({
						'type' : 'hidden',
						'value' : newTitle
					})
				)
			).append(
				$('<div/>').addClass('col-md-2').append(
					$('<button/>').addClass('btn btn-danger').html('Delete')
				)
			).insertBefore($('.customColumnTitles .well'));
			$(this).closest('.row').find('input').val('');
			fixCustomColumnIndexes();
		});
	});
</script>

</#escape>