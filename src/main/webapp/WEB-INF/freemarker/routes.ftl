<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them. 

TODO grab values from the Routes object in code, as that's pretty equivalent and 
	we're repeating ourselves here. OR expose Routes directly.

--><#compress>
<#macro depthome module><@url page="/admin/department/${module.department.code}/#module-${module.code}" /></#macro>
<#macro moduleperms module><@url page="/admin/module/${module.code}/permissions" /></#macro>

<#macro ratefeedback feedback><#compress>
    <#assign assignment=feedback.assignment />
    <#assign module=assignment.module />
    <@url page="/module/${module.code}/${assignment.id}/rate" />
</#compress></#macro>
<#macro assignmentdelete assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/delete"/></#macro>
<#macro assignmentedit assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/edit"/></#macro>
<#macro assignmentsubmissions assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/submissions/list"/></#macro>
<#macro assignmentfeedbacks assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/list"/></#macro>

<#-- non admin -->
<#macro assignment assignment><@url page="/module/${assignment.module.code}/${assignment.id}"/></#macro>
<#macro assignmentreceipt assignment><@url page="/module/${assignment.module.code}/${assignment.id}/resend-receipt"/></#macro>
</#compress>