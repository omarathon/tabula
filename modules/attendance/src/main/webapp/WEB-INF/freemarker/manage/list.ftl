<#import "../attendance_macros.ftl" as attendance_macros />
<#escape x as x?html>

<@fmt.deptheader "Manage monitoring points for ${command.academicYear.toString}" "in" command.department routes "manageHomeForYear" "with-settings" />

<#if schemes?size == 0>

	<p class="muted">There are no monitoring schemes for ${command.academicYear.toString} in your department</p>

	<p>
		<a class="btn btn-primary" href="<@routes.manageNewScheme command.department command.academicYear.startYear?c />">Create scheme</a>
	</p>

<#else>

	<h2 style="display: inline-block;">Schemes</h2>
	<span class="hint">There <@fmt.p number=schemes?size singular="is" plural="are" shownumber=false/> <@fmt.p schemes?size "monitoring scheme" /> in your department</span>

	<p>
		<a class="btn" href="<@routes.manageNewScheme command.department command.academicYear.startYear?c />">Create scheme</a>
		<a class="btn" href="<@routes.manageAddPoints command.department command.academicYear.startYear?c />">Add points</a>
		<#if havePoints>
			<a class="btn" href="<@routes.manageEditPoints command.department command.academicYear.startYear?c />">Edit points</a>
		<#else>
			<a class="btn disabled">Edit points</a>
		</#if>
	</p>

	<#list schemes as scheme>
		<div class="row-fluid">
			<div class="span12">
				<span class="lead">${scheme.displayName}</span>
				<span class="muted">(<@fmt.p scheme.members?size "student" />, <@fmt.p scheme.points?size "point" />)</span>
				<a class="btn btn-primary btn-small" href="<@routes.manageEditScheme command.department command.academicYear.startYear?c scheme/>">Edit</a>
				<a class="btn btn-danger btn-small" href="<@routes.manageDeleteScheme command.department command.academicYear.startYear?c scheme/>"><i class="icon-remove"></i></a>
			</div>
		</div>
	</#list>

</#if>

</#escape>