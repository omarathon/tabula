<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>
	<span id="admin-assignment-container-${assignmentInfo.assignment.id}">
		<@components.admin_assignment_info assignmentInfo />
	</span>
</#escape>