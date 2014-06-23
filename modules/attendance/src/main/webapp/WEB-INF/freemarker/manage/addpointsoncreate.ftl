<#escape x as x?html>

<#assign filterQuery = findCommand.serializeFilter />
<#assign returnTo = (info.requestedUri!"")?url />

<h1>Create a scheme</h1>

<div class="add-points-to-schemes fix-area">

	<p class="progress-arrows">
		<span class="arrow-right use-tooltip" title="Edit properties">Properties</span>
		<span class="arrow-right arrow-left use-tooltip" title="Edit students"><a href="<@routes.manageAddStudents scheme />">Students</a></span>
		<span class="arrow-right arrow-left active">Points</span>
	</p>

	<#if newPoints == 0>
		<p><@fmt.p scheme.points?size "point" /> on this scheme</p>
	<#else>
		<div class="alert alert-success">
			<strong><@fmt.p newPoints "point" /></strong> added to this scheme
		</div>

		<p><@fmt.p scheme.points?size "point" /> on this scheme</p>
	</#if>

	<form method="POST">
		<input name="schemes" value="${scheme.id}" type="hidden" />
		<input name="returnTo" value="<@routes.manageNewSchemeAddPoints scheme />" type="hidden" />
		<button type="button" class="btn add-blank-point" data-href="<@routes.manageAddPointsBlank findCommand.department findCommand.academicYear.startYear?c/>">Add a point</button>
		<button type="button" class="btn copy-points" data-href="<@routes.manageAddPointsCopy findCommand.department findCommand.academicYear.startYear?c/>">Copy points</button>
		<button type="button" class="btn use-template" data-href="<@routes.manageAddPointsTemplate findCommand.department findCommand.academicYear.startYear?c/>">Use template</button>
	</form>

	<#include "_findpointsresult.ftl" />

	<div class="fix-footer submit-buttons">
		<a class="btn" href="<@routes.manageHomeForYear findCommand.department findCommand.academicYear.startYear?c />">Done</a>
	</div>

</div>
</#escape>