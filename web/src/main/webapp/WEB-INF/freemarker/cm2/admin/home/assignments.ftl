<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>
	<#assign skeleton = skeleton!false />
	<#list moduleInfo.assignments as info>
		<span id="admin-assignment-container-${info.assignment.id}">
			<@components.admin_assignment_info info skeleton=skeleton/>
		</span>
	</#list>
</#escape>