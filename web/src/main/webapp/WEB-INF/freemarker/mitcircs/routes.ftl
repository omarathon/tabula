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
<#macro messages submission><@_u page="/admin/${department.code}/view/${submission.key}/messages" /></#macro>

<#macro studenthome student><@_u context='/profiles' page="/view/${student.universityId}/personalcircs"/></#macro>

<#macro viewsubmission submission><@_u context='/profiles' page="/view/${submission.student.universityId}/personalcircs/mitcircs/view/${submission.key}" /></#macro>
<#macro newsubmission student><@_u context='/profiles' page="/view/${student.universityId}/personalcircs/mitcircs/new" /></#macro>
<#macro editsubmission submission><@_u context='/profiles' page="/view/${submission.student.universityId}/personalcircs/mitcircs/edit/${submission.key}" /></#macro>
<#macro pendingevidence submission><@_u context='/profiles' page="/view/${submission.student.universityId}/personalcircs/mitcircs/pendingevidence/${submission.key}" /></#macro>
<#macro renderAttachment submission file><@_u context='/profiles' page="/view/${submission.student.universityId}/personalcircs/mitcircs/${submission.key}/supporting-file/${file.name}" /></#macro>

<#macro affectedAssessments student><@_u context='/profiles' page="/view/${student.universityId}/personalcircs/affected-assessments" /></#macro>