<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>
	<#list moduleInfo.assignments as info>
		<span id="admin-assignment-container-${info.assignment.id}">
			<@components.admin_assignment_info info />
		</span>
	</#list>
</#escape>