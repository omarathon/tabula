<#escape x as x?html>
	<#import "*/group_components.ftl" as components />
	
	<@components.single_module moduleItem=data.moduleItems?first canManageDepartment=data.canManageDepartment expand_by_default=true />
</#escape>