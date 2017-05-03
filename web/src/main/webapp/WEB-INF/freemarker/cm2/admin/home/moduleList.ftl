<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>
	<#list modules as moduleInfo>
		<@components.admin_assignment_list moduleInfo.module moduleInfo.assignments false />
	</#list>
</#escape>