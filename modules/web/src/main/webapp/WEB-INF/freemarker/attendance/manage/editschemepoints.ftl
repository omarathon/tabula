<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<#assign filterQuery = findCommand.serializeFilter />
<#assign returnTo><@routes.attendance.manageEditSchemePoints scheme.department scheme.academicYear.startYear?c scheme /></#assign>

<h1>Edit scheme: ${scheme.displayName}</h1>

<div class="add-points-to-schemes fix-area">

	<p class="progress-arrows">
		<span class="arrow-right use-tooltip" title="Edit properties"><a href="<@routes.attendance.manageEditScheme findCommand.department findCommand.academicYear.startYear?c scheme />">Properties</a></span>
		<span class="arrow-right arrow-left use-tooltip" title="Edit students"><a href="<@routes.attendance.manageEditSchemeStudents findCommand.department findCommand.academicYear.startYear?c scheme />">Students</a></span>
		<span class="arrow-right arrow-left active">Points</span>
	</p>

	<#if newPoints == 0>
		<p><@fmt.p scheme.points?size "point" /> on this scheme</p>
	<#else>
		<div class="alert alert-success">
			<strong><@fmt.p newPoints "point" /></strong> ${actionCompleted!"added to this scheme"}
			<a class="btn" href="<@routes.attendance.manageEditPoints findCommand.department findCommand.academicYear.startYear?c schemesParam />">Edit points</a>
		</div>

		<p><@fmt.p scheme.points?size "point" /> on this scheme</p>
	</#if>

	<form method="POST">
		<input name="schemes" value="${scheme.id}" type="hidden" />
		<input name="returnTo" value="<@routes.attendance.manageEditSchemePoints findCommand.department findCommand.academicYear.startYear?c scheme />" type="hidden" />
		<button type="button" class="btn add-blank-point" data-href="<@routes.attendance.manageAddPointsBlank findCommand.department findCommand.academicYear.startYear?c />">Add a point</button>
		<button type="button" class="btn copy-points" data-href="<@routes.attendance.manageAddPointsCopy findCommand.department findCommand.academicYear.startYear?c/>">Copy points</button>
		<button type="button" class="btn use-template" data-href="<@routes.attendance.manageAddPointsTemplate findCommand.department findCommand.academicYear.startYear?c/>">Use template</button>
	</form>

	<#include "_findpointsresult.ftl" />

	<div class="fix-footer submit-buttons">
		<a class="btn" href="<@routes.attendance.manageHomeForYear findCommand.department findCommand.academicYear.startYear?c />">Done</a>
	</div>

</div>

<script>
	jQuery(function($) {
		$('.fix-area').fixHeaderFooter();
	});
</script>

</#escape>