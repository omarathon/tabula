<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.

TODO grab values from the Routes object in code, as that's pretty equivalent and
	we're repeating ourselves here. OR expose Routes directly.

--><#compress>
<#macro home><@url page="/" /></#macro>
<#macro departmenthome department><@url page="/department/${department.code}/" /></#macro>
<#macro depthome module><@url page="/department/${module.department.code}/#module-${module.code}" /></#macro>

<#macro deptperms department><@url page="/department/${department.code}/permissions" /></#macro>
<#macro moduleperms module><@url page="/module/${module.code}/permissions" /></#macro>
<#macro routeperms route><@url page="/route/${route.code}/permissions" /></#macro>

<#macro displaysettings department><@url page="/department/${department.code}/settings/display" /></#macro>

<#macro createsubdepartment department><@url page="/department/${department.code}/subdepartment/new" /></#macro>
<#macro createmodule department><@url page="/department/${department.code}/module/new" /></#macro>
<#macro sortmodules department><@url page="/department/${department.code}/sort-modules" /></#macro>
<#macro sortroutes department><@url page="/department/${department.code}/sort-routes" /></#macro>

</#compress>
