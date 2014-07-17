<#import "*/group_components.ftl" as components />
<#escape x as x?html>

<#if department??>
	<@fmt.deptheader "Manage reusable small group allocations" "in" department routes "crossmodulegroups" "with-settings" />
<#else>
	<p>No department.</p>
</#if>

	<#if sets?size == 0>

		<p class="muted">There are no reusable small groups in your department.</p>

		<p>
			<a class="btn btn-primary" href="<@routes.createcrossmodulegroups department />">Create reusable small groups</a>
		</p>

	<#else>

		<h2 style="display: inline-block;">Reusable small group sets</h2>
		<span class="hint">There <@fmt.p number=sets?size singular="is" plural="are" shownumber=false/> <@fmt.p sets?size "reusable small group" /> in your department.</span>

		<p>
			<a class="btn" href="<@routes.createcrossmodulegroups department />">Create reusable small groups</a>
		</p>

		<#list sets?sort_by("name") as set>
			<div class="row-fluid">
				<div class="span9 hover-highlight">
					<div class="pull-right" style="line-height:30px">
						<a class="btn btn-primary btn-mini" href="<@routes.editcrossmodulegroups set />">Edit</a>
						<a class="btn btn-danger btn-mini" href="<@routes.deletecrossmodulegroups set />"><i class="icon-remove"></i></a>
					</div>
					<span class="lead">${set.name}</span>
					<span class="muted">
						<#if set.members.members?size == 0>
							(0 students)
						<#else>
							(<a href="<@routes.editcrossmodulegroupsstudents set />"><@fmt.p set.members.members?size "student" /></a>)
						</#if>
					</span>
				</div>
			</div>
		</#list>

	</#if>

</#escape>