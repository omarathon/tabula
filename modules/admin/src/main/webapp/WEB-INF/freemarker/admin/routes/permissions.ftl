<#compress><#escape x as x?html>

<#import "/WEB-INF/freemarker/permissions_macros.ftl" as pm />
<#import "/WEB-INF/freemarker/formatters.ftl" as fmt />
<#assign perms_url><@routes.routeperms route /></#assign>
<#assign route_name><@fmt.route_name route /></#assign>

<div id="route-permissions-page">
	<h1>Route permissions</h1>
	<h5>for <#noescape>${route_name}</#noescape></h5>

	<@pm.alerts "addCommand" route_name users role />
	
	<#assign scope=route />
	<#include "_roles.ftl" />
</div>

<@pm.script />

</#escape></#compress>