<#escape x as x?html>

<h1>Marking</h1>

<#if hasPermission && result??>
	<#if result.empty>
		<div class="alert alert-info">
			This person isn't a marker for any assignments in Tabula.
		</div>
	<#else>
		<@stylesheet "/static/css/id7/cm2.css" />

		<#assign embedded=true />
		<#assign markerInformation=result />
		<#include "/WEB-INF/freemarker/cm2/home/_marker.ftl" />
	</#if>
<#else>

	<div class="alert alert-info">
		You do not have permission to see the marking for this person.
	</div>

</#if>

</#escape>