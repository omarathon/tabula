<#escape x as x?html>
<#macro select_module_fields>
	<#if jobId??>
		<input type="hidden" name="jobId" value="${jobId}" />
	</#if>
		<input type="hidden" name="module" value="${module.code}" />
</#macro>
</#escape>