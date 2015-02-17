<#ftl strip_text=true />

<#assign requestPath = (info.requestedUri.path!"") />

<#if requestPath?starts_with('/reports/')>
	<#assign bodyClass="reports-page" />
	<#assign siteHeader="Reports" />
	<#assign subsite=true />
	<#assign title="Reports" />
	<#assign name="reports" />
	<#assign nonav=false />
<#elseif requestPath?starts_with('/admin/')>
	<#assign bodyClass="admin-page" />
	<#assign siteHeader="Administration & Permissions" />
	<#assign subsite=true />
	<#assign title="Administration & Permissions" />
	<#assign name="admin" />
	<#assign nonav=false />
<#else>
	<#assign bodyClass="tabula-page" />
	<#assign siteHeader="Tabula" />
	<#assign subsite=false />
	<#assign title="Tabula" />
	<#assign name="home" />
	<#assign nonav=true />
</#if>