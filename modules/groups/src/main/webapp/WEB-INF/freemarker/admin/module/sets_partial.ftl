<#escape x as x?html>
	<#import "*/group_components.ftl" as components />
	
	<@components.groupsets_info moduleItem=data.moduleItems?first />
</#escape>