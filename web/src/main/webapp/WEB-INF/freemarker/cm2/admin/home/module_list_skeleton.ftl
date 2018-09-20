<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>
	<@components.admin_assignment_list moduleInfo.module moduleInfo.assignments academicYear true true />
</#escape>