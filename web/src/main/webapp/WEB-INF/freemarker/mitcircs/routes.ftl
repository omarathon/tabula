<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.
TODO grab values from the Routes object in code, as that's pretty equivalent and
   we're repeating ourselves here. OR expose Routes directly.
-->
<#macro _u page context='/mitcircs'><@url context=context page=page /></#macro>

<#macro home><@_u page="/" /></#macro>
<#macro adminhome department><@_u page="/admin/${department.code}" /></#macro>

<#macro studenthome><@_u page="/profile" /></#macro>
<#macro studenthome student><@_u page="/profile/${student.universityId}" /></#macro>
<#macro newsubmission student><@_u page="/profile/${student.universityId}/new" /></#macro>
<#macro editsubmission submission><@_u page="/profile/${submission.student.universityId}/edit/${submission.key}" /></#macro>
<#macro renderAttachment submission file><@_u page="/profile/${submission.student.universityId}/${submission.key}/supporting-file/${file.name}" /></#macro>
