<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<#macro listStudentIdInputs>
	<#list command.staticStudentIds as id>
		<input type="hidden" name="staticStudentIds" value="${id}" />
	</#list>
	<#list command.includedStudentIds as id>
		<input type="hidden" name="includedStudentIds" value="${id}" />
	</#list>
	<#list command.excludedStudentIds as id>
		<input type="hidden" name="excludedStudentIds" value="${id}" />
	</#list>
</#macro>

<h1>Create scheme: ${scheme.displayName}</h1>

<form method="POST">
	<input type="hidden" name="filterQueryString" value="${command.filterQueryString!""}" />
	<@listStudentIdInputs />

	<p class="progress-arrows">
		<span class="arrow-right">Properties</span>
		<span class="arrow-right arrow-left active">Students</span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit points"><button type="submit" class="btn btn-link" name="${ManageSchemeMappingParameters.createAndAddPoints}">Points</button></span>
	</p>
</form>

<div class="fix-area">

	<#assign membershipItems = command.membershipItems />

	<p>
		<@fmt.p membershipItems?size "student" /> on this scheme

		<#assign popoverContent><#noescape>
			<p>You can filter to select types of students (e.g. 1st year part-time UG)
			and then either use these to populate a static list (which will not then change),
			or link this group to SITS so that the list of students will be updated automatically from there.</p>

			<p>You can also manually add students by ITS usercode or university number.</p>

			<p>You can tweak the list even when it is linked to SITS, by manually adding and excluding students.</p>
		</#noescape></#assign>
		<a class="use-popover"
		   id="popover-student-count"
		   data-content="${popoverContent}"
		   data-html="true"
		>
			<i class="icon-question-sign"></i>
		</a>
	</p>


	<form method="POST" action="<@routes.manageSelectStudents command.scheme />">
		<@listStudentIdInputs />
		<input type="hidden" name="filterQueryString" value="${command.filterQueryString!""}"/>
		<input type="hidden" name="returnTo" value="<@routes.manageAddStudents scheme />">
		<input
			type="submit"
			class="btn"
			value="Select students for scheme"
		/>
	</form>

	<#if (membershipItems?size > 0)>
		<@attendance_macros.manageStudentTable membershipItems />
	</#if>

	<div class="fix-footer submit-buttons">
		<form method="POST">
			<input type="hidden" name="filterQueryString" value="${command.filterQueryString!""}" />
			<@listStudentIdInputs />
			<input
				type="submit"
				class="btn btn-success use-tooltip"
				name="${ManageSchemeMappingParameters.createAndAddPoints}"
				value="Add points"
				title="Select which monitoring points this scheme should use"
				data-container="body"
			/>
			<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="create"
				value="Save"
				title="Save your blank scheme and add points to it later"
				data-container="body"
			/>
			<a class="btn" href="<@routes.manageHomeForYear command.scheme.department command.scheme.academicYear.startYear?c />">Cancel</a>
		</form>
	</div>

</div>

<script>
	jQuery(function($) {
		$('.fix-area').fixHeaderFooter();
	});
</script>

</#escape>