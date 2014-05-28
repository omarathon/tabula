<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<h1>Delete scheme</h1>

<@f.form id="deleteScheme" method="POST" commandName="command" class="form-horizontal">

Are you sure you want to delete scheme: ${command.scheme.name}?

	<#if command.scheme.points?size == 0>
		There are no points
	<#else><p>There are some points (${command.scheme.points?size})</p>
	</#if>

<p>&nbsp;</p>
	<input type="hidden" name="filterQueryString" value="${command.filterQueryString!""}" />
	<input
		type="submit"
		class="btn btn-primary"
		name="submit"
		value="Delete"
	/>
	<a class="btn" href="<@routes.manageHomeForYear command.scheme.department command.scheme.academicYear.startYear?c />">Cancel</a>

</@f.form>

</#escape>