<#ftl strip_text=true />

<#assign requestPath = (info.requestedUri.path!"") />

<#if requestPath == '/reports' || requestPath?starts_with('/reports/')>
	<#assign bodyClass="reports-page" />
	<#assign siteHeader="Reports" />
	<#assign subsite=true />
	<#assign title="Reports" />
	<#assign name="reports" />
	<#assign nonav=false />
	<#assign homeUrl><@routes.reports.home /></#assign>
<#elseif requestPath == '/admin' || requestPath?starts_with('/admin/')>
	<#assign bodyClass="admin-page" />
	<#assign siteHeader="Administration & Permissions" />
	<#assign subsite=true />
	<#assign title="Administration & Permissions" />
	<#assign name="admin" />
	<#assign nonav=false />
	<#assign homeUrl><@routes.admin.home /></#assign>
<#elseif requestPath == '/groups' || requestPath?starts_with('/groups/')>
	<#assign bodyClass="groups-page" />
	<#assign siteHeader="Small Group Teaching" />
	<#assign subsite=true />
	<#assign title="Small Group Teaching" />
	<#assign name="groups" />
	<#assign nonav=false />
	<#assign homeUrl><@routes.groups.home /></#assign>
<#else>
	<#assign bodyClass="tabula-page" />
	<#assign siteHeader="Tabula" />
	<#assign subsite=false />
	<#assign title="Tabula" />
	<#assign name="home" />
	<#assign nonav=true />
</#if>