<#compress><#escape x as x?html>

<#import "/WEB-INF/freemarker/permissions_macros.ftl" as pm />
<#import "/WEB-INF/freemarker/formatters.ftl" as fmt />
<#assign perms_url><@routes.moduleperms module /></#assign>
<#assign module_name><@fmt.module_name module /></#assign>

<div id="module-permissions-page">
	<h1>Module permissions</h1>
	<h5>for <#noescape>${module_name}</#noescape></h5>

	<@pm.alerts "addCommand" module_name users role />

	<#assign scope=module />
	<#include "_roles.ftl" />
</div>

<@pm.script />

</#escape></#compress>