<#escape x as x?html>

	<#function route_function dept>
		<#local result><@routes.admin.manualmembership dept /></#local>
		<#return result />
	</#function>
	<@fmt.id7_deptheader "Manual membership summary" route_function "for" />

	<p>
		This page shows a list of all assignments and small group sets in ${academicYear} that have manually added students. It is important that SITS holds up to date information
		about the students taking each module. Once the SITS membership for these assignments and small group sets is correct you should remove the manually added
		students.
	</p>

	<#if assignmentsByModule?has_content>
		<h2>Assignments</h2>
		<div>
			<#list assignmentsByModule?keys as module>
				<h4>
					<span class="mod-code">${module.code}</span>
					<span class="mod-name">${module.name}</span>
				</h4>
				<ul>
					<#list mapGet(assignmentsByModule, module) as assignment>
						<li>
							<#if can.do("Assignment.Update", assignment.module)>
								<a href="<@routes.cm2.assignmentstudents assignment "edit" />">${assignment.name}</a>
							<#else>
								${assignment.name}
							</#if>
							(<@fmt.p assignment.members.size "manually added student" />)
						</li>
					</#list>
				</ul>
			</#list>
		</div>
	</#if>

	<#if smallGroupSetsByModule?has_content>
		<h2>Small group sets</h2>
		<div>
		<#list smallGroupSetsByModule?keys as module>
			<h4>
				<span class="mod-code">${module.code}</span>
				<span class="mod-name">${module.name}</span>
			</h4>
			<ul>
				<#list mapGet(smallGroupSetsByModule, module) as groupset>
					<li>
						<#if can.do("SmallGroups.Update", groupset)>
							<a href="<@routes.groups.editsetstudents groupset />">${groupset.name}</a>
						<#else>
							${groupset.name}
						</#if>
						(<@fmt.p groupset.members.size "manually added student" />)
					</li>
				</#list>
			</ul>
		</#list>
		</div>
	</#if>
</#escape>