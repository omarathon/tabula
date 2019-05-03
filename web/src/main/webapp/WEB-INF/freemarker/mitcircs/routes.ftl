<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.
TODO grab values from the Routes object in code, as that's pretty equivalent and
   we're repeating ourselves here. OR expose Routes directly.
-->
<#macro _u page context='/mitcircs'><@url context=context page=page /></#macro>

<#macro home><@_u page="/" /></#macro>
<#macro adminhome department academicYear="">
  <#if academicYear?has_content>
    <@_u page="/admin/${department.code}/${academicYear.startYear?c}" />
  <#else>
    <@_u page="/admin/${department.code}" />
  </#if>
</#macro>
<#macro studenthome student><@_u context='/profiles' page="/view/${student.universityId}/personalcircs"/></#macro>

<#macro newsubmission student><@_u context='/profiles' page="/view/${student.universityId}/personalcircs/mitcircs/new" /></#macro>
<#macro editsubmission submission><@_u context='/profiles' page="/view/${submission.student.universityId}/personalcircs/mitcircs/edit/${submission.key?c}" /></#macro>
<#macro pendingevidence submission><@_u context='/profiles' page="/view/${submission.student.universityId}/personalcircs/mitcircs/pendingevidence/${submission.key?c}" /></#macro>

<#-- Helper for new/edit -->
<#macro affectedAssessments student><@_u context='/profiles' page="/view/${student.universityId}/personalcircs/affected-assessments" /></#macro>

<#-- View submission (as the student) -->
<#macro viewsubmission submission><@_u context='/profiles' page="/view/${submission.student.universityId}/personalcircs/mitcircs/view/${submission.key?c}" /></#macro>

<#-- Review submission (as the MCO) -->
<#macro reviewSubmission submission><@_u page = "/submission/${submission.key?c}" /></#macro>
<#macro notes submission><@_u page="/submission/${submission.key?c}/notes" /></#macro>
<#macro deleteNote note><@_u page="/submission/${note.submission.key?c}/notes/${note.id}/delete" /></#macro>

<#-- These get posted to from both the view (as the student) and the review (as the MCO) -->
<#macro messages submission><@_u page="/submission/${submission.key?c}/messages" /></#macro>
<#macro renderAttachment submission file><@_u page="/submission/${submission.key?c}/supporting-file/${file.name}" /></#macro>