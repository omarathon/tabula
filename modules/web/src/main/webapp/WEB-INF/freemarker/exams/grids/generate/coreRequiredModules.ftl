<#escape x as x?html>

<#import "/WEB-INF/freemarker/modal_macros.ftl" as modal />

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
	<#list gridOptionsCommand.predefinedColumnIdentifiers as column>
		<input type="hidden" name="predefinedColumnIdentifiers" value="${column}" />
	</#list>
	<#list gridOptionsCommand.customColumnTitles as column>
		<input type="hidden" name="customColumnTitles[${column_index}]" value="${column}" />
	</#list>
	<input type="hidden" name="nameToShow" value="${gridOptionsCommand.nameToShow}" />
	<input type="hidden" name="yearsToShow" value="${gridOptionsCommand.yearsToShow}" />
	<input type="hidden" name="marksToShow" value="${gridOptionsCommand.marksToShow}" />
	<input type="hidden" name="moduleNameToShow" value="${gridOptionsCommand.moduleNameToShow}" />

	<h2>Set grid options</h2>

	<p class="progress-arrows">
		<span class="arrow-right"><button type="submit" class="btn btn-link">Select courses</button></span>
		<span class="arrow-right arrow-left active">Set grid options</span>
		<span class="arrow-right arrow-left">Preview and download</span>
	</p>

	<div class="alert alert-info">
		<h3>Core required modules</h3>
		<p>Unfortunately Tabula cannot identify core required modules within SITS. Please select the modules from the list below.</p>
		<p><strong>Note:</strong> The chosen modules will apply to all students on route ${selectCourseCommand.route.code?upper_case} year of study ${selectCourseCommand.yearOfStudy} for ${academicYear.toString}</p>
	</div>

	<p>
		Select the items to include in your grid for Course: ${selectCourseCommand.course.code?upper_case} ${selectCourseCommand.course.name},
		Route ${selectCourseCommand.route.code?upper_case} ${selectCourseCommand.route.name},
		Year of Study: ${selectCourseCommand.yearOfStudy}
	</p>

	<h3>Identify Core Required Modules</h3>


	<table class="table table-condensed table-striped modules">
		<thead>
			<tr>
				<th class="for-check-all" style="width: 20px;"></th>
				<th>Available Modules</th>
			</tr>
		</thead>
		<tbody>
			<#list coreRequiredModulesCommand.allModules?sort_by("code") as module>
				<#assign isChecked = false />
				<#list coreRequiredModulesCommand.modules as checkedModule>
					<#if checkedModule.code == module.code>
						<#assign isChecked = true />
					</#if>
				</#list>
				<tr>
					<td><input type="checkbox" name="modules" value="${module.code}" id="module-${module.code}" <#if isChecked>checked</#if>></td>
					<td><label for="module-${module.code}"> ${module.code?upper_case} ${module.name}</label></td>
				</tr>
			</#list>
		</tbody>
	</table>

	<@bs3form.errors path="coreRequiredModulesCommand" />

	<button class="btn btn-primary" type="submit" name="${GenerateExamGridMappingParameters.coreRequiredModules}">Next</button>
</form>

<script>
	jQuery(function($){
		var $modulesTable = $('table.modules');
		var updateCheckboxes = function() {
			var checked = $modulesTable.find('td input:checked').length;
			if (checked == $modulesTable.find('td input').length) {
				$modulesTable.find('.check-all').prop('checked', true);
			}
			if (checked == 0) {
				$modulesTable.find('.check-all').prop('checked', false);
			}
		};

		$('.for-check-all').append($('<input />', { type: 'checkbox', 'class': 'check-all use-tooltip', title: 'Select all/none' }));
		$modulesTable.on('click', '.check-all', function() {
			var checkStatus = this.checked;
			$modulesTable.find('td input:checkbox').prop('checked', checkStatus);

			updateCheckboxes();
		});
	});
</script>

</#escape>