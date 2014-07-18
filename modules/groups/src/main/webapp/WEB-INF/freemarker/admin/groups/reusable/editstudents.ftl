<#escape x as x?html>
<#import "*/group_components.ftl" as components />

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

<h1>Edit reusable small groups: ${smallGroupSet.name}</h1>

<form method="POST">
	<input type="hidden" name="filterQueryString" value="${command.filterQueryString!""}" />
	<@listStudentIdInputs />

	<p class="progress-arrows">
		<span class="arrow-right">Properties</span>
		<span class="arrow-right arrow-left active">Students</span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit groups"><button type="submit" class="btn btn-link" name="${ManageDepartmentSmallGroupsMappingParameters.editAndAddGroups}">Groups</button></span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and allocate students to groups"><button type="submit" class="btn btn-link" name="${ManageDepartmentSmallGroupsMappingParameters.editAndAllocate}">Allocate</button></span>
	</p>
</form>

<div class="fix-area">

	<#assign membershipItems = command.membershipItems />

<p>
	<@fmt.p membershipItems?size "student" /> in these groups

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


	<form method="POST" action="<@routes.crossmodulegroupsselectstudents smallGroupSet />">
		<@listStudentIdInputs />
		<input type="hidden" name="filterQueryString" value="${command.filterQueryString!""}"/>
		<input type="hidden" name="returnTo" value="<@routes.editcrossmodulegroupsstudents smallGroupSet />">
		<input
				type="submit"
				class="btn"
				value="Select students for group set"
				/>
	</form>

	<#if (membershipItems?size > 0)>
		<@components.manageStudentTable membershipItems />
	</#if>

	<div class="fix-footer submit-buttons">
		<form method="POST">
			<input type="hidden" name="filterQueryString" value="${command.filterQueryString!""}" />
			<@listStudentIdInputs />
			<input
				type="submit"
				class="btn btn-success use-tooltip"
				name="${ManageDepartmentSmallGroupsMappingParameters.editAndAddGroups}"
				value="Save and add groups"
				title="Add groups to this set of reusable groups"
				data-container="body"
				/>
			<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="edit"
				value="Save and exit"
				title="Save your groups and add students and groups to it later"
				data-container="body"
				/>
			<a class="btn" href="<@routes.crossmodulegroups smallGroupSet.department />">Cancel</a>
		</form>
	</div>

</div>

<script>
	jQuery(function($) {
		$('.fix-area').fixHeaderFooter();
	});
</script>

</#escape>