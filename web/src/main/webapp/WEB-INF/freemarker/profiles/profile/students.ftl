<#escape x as x?html>

<h1>Students</h1>

<#if relationshipTypesMap?keys?has_content || smallGroups?has_content>
	<ul class="links">
		<#list relationshipTypesMap?keys as relationshipType>
			<#if relationshipTypesMapById[relationshipType.id]>
				<li><a href="<@routes.profiles.relationship_students relationshipType />">${relationshipType.studentRole?cap_first}s</a></li>
			</#if>
		</#list>

		<#list smallGroups as smallGroup>
			<#assign _groupSet=smallGroup.groupSet />
			<#assign _module=smallGroup.groupSet.module />
			<li><a href="<@routes.profiles.smallgroup smallGroup />">
			${_module.code?upper_case} (${_module.name}) ${_groupSet.nameWithoutModulePrefix}, ${smallGroup.name}
			</a></li>
		</#list>
	</ul>
<#else>
	<p>
		You are not currently a tutor for any group of students in Tabula. If you think this is incorrect or you need assistance, please visit our <a href="/help">help page</a>.
	</p>
</#if>

</#escape>