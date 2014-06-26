<#import "../attendance_macros.ftl" as attendance_macros />
<#escape x as x?html>

<#macro deptheaderroutemacro dept>
	<@routes.manageHomeForYear dept command.academicYear.startYear?c />
</#macro>
<#assign deptheaderroute = deptheaderroutemacro in routes/>
<@fmt.deptheader "Manage monitoring points for ${command.academicYear.toString}" "in" command.department routes "deptheaderroute" "with-settings" />

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

	<#list schemes?sort_by("displayName") as scheme>
		<div class="row-fluid">
			<div class="span9 hover-highlight">
				<div class="pull-right" style="line-height:30px">
					<a class="btn btn-primary btn-mini" href="<@routes.manageEditScheme command.department command.academicYear.startYear?c scheme/>">Edit</a>
					<a class="btn btn-danger btn-mini" href="<@routes.manageDeleteScheme command.department command.academicYear.startYear?c scheme/>"><i class="icon-remove"></i></a>
				</div>
				<span class="lead">${scheme.displayName}</span>
				<span class="muted">
					<#if scheme.members.members?size == 0>
						(0 students,
					<#else>
						(<a href="<@routes.manageEditSchemeStudents command.department command.academicYear.startYear?c scheme />"><@fmt.p scheme.members.members?size "student" /></a>,
					</#if>
					<#if scheme.points?size == 0>
						0 points)
					<#else>
						<a href="<@routes.manageEditSchemePoints command.department command.academicYear.startYear?c scheme />"><@fmt.p scheme.points?size "point" /></a>)
					</#if>
				</span>
			</div>
		</div>
	</#list>

</#if>

</#escape>