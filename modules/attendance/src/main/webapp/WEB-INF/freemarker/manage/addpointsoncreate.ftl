<#escape x as x?html>

<h1>Create a scheme</h1>

<p class="progress-arrows">
	<span class="arrow-right">Properties</span>
	<span class="arrow-right arrow-left">Students</span>
	<span class="arrow-right arrow-left active">Points</span>
</p>

<div class="add-points-to-schemes">

	<p>Add points to this monitoring scheme</p>

	<#if newPoints == 0>
		<p><@fmt.p scheme.points?size "point" /> on this scheme</p>
	<#else>
		<div class="alert alert-success">
			<strong><@fmt.p newPoints "point" /></strong> added to this scheme
		<#-- TODO add Edit points button -->
		</div>

		<p><@fmt.p scheme.points?size "point" /> on this scheme</p>
	</#if>

	<form method="POST">
		<input name="schemes" value="${scheme.id}" type="hidden" />
		<input name="returnTo" value="<@routes.manageNewSchemeAddPoints scheme />" type="hidden" />
		<button type="button" class="btn add-blank-point" data-href="<@routes.manageAddPointsBlank command.department command.academicYear.startYear?c/>">Add a point</button>
		<button type="button" class="btn copy-points" data-href="<@routes.manageAddPointsCopy command.department command.academicYear.startYear?c/>">Copy points</button>
		<button type="button" class="btn use-template" data-href="<@routes.manageAddPointsTemplate command.department command.academicYear.startYear?c/>">Use template</button>
	</form>

	<p>
		<a class="btn" href="<@routes.manageHomeForYear command.department command.academicYear.startYear?c />">Done</a>
	</p>

</div>
</#escape>