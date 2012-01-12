<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them. 
-->
<#macro depthome module><@url page="/admin/department/${module.department.code}/#module-${module.code}" /></#macro>
<#macro moduleperms module><@url page="/admin/module/${module.code}/permissions" /></#macro>