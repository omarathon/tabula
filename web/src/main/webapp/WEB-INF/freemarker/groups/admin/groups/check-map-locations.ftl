<h1>Events without a map location</h1>

<p>
	This page displays any small group events with locations that aren't recognised by Tabula.
	These locations cannot be displayed on the campus map.
</p>

<#if smallGroupSets?size gt 0>
	<#list smallGroupSets as pair>
		<#assign set = pair._1()>
		<#assign module = set.module>
		<#assign events = pair._2()>
		<h4>
			<span class="mod-code">${module.code?upper_case}</span>
			<span class="group-name">
				<a href="<@routes.groups.editset set/>">${set.name}</a>
			</span>
		</h4>
			<ul>
				<#list events as event>
					<li>
						${event.group.name}: <strong>${event.title}</strong>
						has a named location <em>'${event.location.name}'</em>
						<a href="<@routes.groups.editseteventseditevent event/>?returnTo=${(info.requestedUri!"")?url}" class="btn btn-default btn-xs">Edit</a>
					</li>
				</#list>
			</ul>
		</li>
	</#list>
<#else>
<div class="alert alert-info">
	All small group event locations are linked to a map position.
</div>
</#if>
