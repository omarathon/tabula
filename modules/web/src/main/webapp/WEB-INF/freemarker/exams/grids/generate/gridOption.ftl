<#escape x as x?html>

<#function route_function dept>
	<#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

<form action="<@routes.exams.generateGrid department academicYear />" class="dirty-check grid-options" method="post">

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

	<p>
		Select the items to include in your grid for Course: ${selectCourseCommand.course.code?upper_case} ${selectCourseCommand.course.name},
		Route ${selectCourseCommand.route.code?upper_case} ${selectCourseCommand.route.name},
		Year of Study: ${selectCourseCommand.yearOfStudy}
	</p>

	<h3>Student identification</h3>

	<div class="row">
		<div class="col-md-3">
			<div class="radio">
				<label><input type="radio" name="nameToShow" value="full"
					<#if gridOptionsCommand.nameToShow == 'full'>checked</#if>
				/> Official name</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="radio">
				<label><input type="radio" name="nameToShow" value="both"
					<#if gridOptionsCommand.nameToShow == 'both'>checked</#if>
				/> First and last name</label>
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
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="modulereports"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("modulereports")>checked</#if>
				/> Module Reports</label>
			</div>
		</div>
	</div>

	<hr />

	<h3>CATS</h3>

	<div class="row">
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="cats"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("cats")>checked</#if>
				/> Total CATS</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="passedCats"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("passedCats")>checked</#if>
				/> Passed CATS</label>
			</div>
		</div>
	</div>

	<hr />

	<h3>Marking</h3>

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
				/> Current year mean mark</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="overcatted" checked disabled
				/> Overcatted year mark</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="board" checked disabled
				/> Board agreed mark</label>
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
				/> Suggested result</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="finalOverallMark"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("finalOverallMark")>checked</#if>
				/> Final overall mark</label>
			</div>
		</div>
		<div class="col-md-3">
			<div class="checkbox">
				<label><input type="checkbox" name="predefinedColumnIdentifiers" value="suggestedgrade"
					<#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("suggestedgrade")>checked</#if>
				/> Suggested final year grade</label>
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
				$(this).find('input').prop('name', 'customColumnTitles[' + index + ']');
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