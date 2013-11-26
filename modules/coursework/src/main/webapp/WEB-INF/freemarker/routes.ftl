<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.

TODO grab values from the Routes object in code, as that's pretty equivalent and
	we're repeating ourselves here. OR expose Routes directly.

-->
<#macro home><@url page="/" /></#macro>
<#macro departmenthome department><@url page="/admin/department/${department.code}/" /></#macro>
<#macro depthome module><@url page="/admin/department/${module.department.code}/#module-${module.code}" /></#macro>
<#macro moduleperms module><@url page="/module/${module.code}/permissions" context="/admin" /></#macro>

<#macro ratefeedback feedback><#compress>
    <#assign assignment=feedback.assignment />
    <#assign module=assignment.module />
    <@url page="/module/${module.code}/${assignment.id}/rate" />
</#compress></#macro>
<#macro assignmentdelete assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/delete"/></#macro>
<#macro assignmentedit assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/edit"/></#macro>

<#macro onlinefeedback assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/online"/></#macro>
<#macro markerOnlinefeedback assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/feedback/online"/></#macro>
<#macro onlinefeedbackform assignment studentid><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/online/${studentid}"/></#macro>
<#macro markerOnlinefeedbackform assignment studentid><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/feedback/online/${studentid}"/></#macro>
<#macro markerModerationform assignment studentid><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/feedback/online/moderation/${studentid}"/></#macro>

<#macro genericfeedback assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/generic"/></#macro>
<#macro markerFeedbackFiles assignment markerFeedback><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/feedback/download/${markerFeedback.id}/feedback-${markerFeedback.feedback.universityId}.zip"/></#macro>
<#macro markingCompleted assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/marking-completed" /></#macro>

<#macro enrolment module><@url page="/admin/module/${module.code}/assignments/enrolment"/></#macro>

<#macro createAssignment module><@url page="/admin/module/${module.code}/assignments/new" /></#macro>
<#macro copyModuleAssignments module><@url page="/admin/module/${module.code}/copy-assignments" /></#macro>
<#macro archiveModuleAssignments module><@url page="/admin/module/${module.code}/archive-assignments" /></#macro>

<#macro addMarks assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marks" /></#macro>
<#macro addFeedback assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/batch" /></#macro>

<#macro listmarkersubmissions assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/list"/></#macro>
<#macro downloadmarkersubmissions assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/submissions.zip"/></#macro>
<#macro downloadfirstmarkerfeedback assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/firstmarker/feedbacks.zip"/></#macro>
<#macro uploadmarkerfeedback assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/feedback"/></#macro>
<#macro markeraddmarks assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/marks"/></#macro>

<#macro assignmentsubmissionsandfeedback assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/list"/></#macro>
<#macro assignmentsubmissionsandfeedbacktable assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/table"/></#macro>
<#macro assignmentsubmissionsandfeedbacksummary assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/summary"/></#macro>

<#macro assignMarkers assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/assign-markers" /></#macro>
<#macro releaseForMarking assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/submissionsandfeedback/release-submissions" /></#macro>

<#macro onlinemarking assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/online" /></#macro>
<#macro onlinemarkingform assignment student><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/online/${student.universityId}" /></#macro>

<#macro feedbackadd assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/add" /></#macro>
<#macro feedbackdelete assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/delete" /></#macro>
<#macro markstemplate assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marks-template" /></#macro>
<#macro markermarkstemplate assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/marks-template" /></#macro>
<#macro extensions assignment><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/extensions" /></#macro>
<#macro extensionreviewattachment assignment userid filename><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/extensions/review-request/${userid}/supporting-file/${filename}" /></#macro>
<#macro extensionrequestattachment assignment filename><@url page="/module/${assignment.module.code}/${assignment.id}/extension/supporting-file/${filename}" /></#macro>

<#macro extensionsettings department><@url page="/admin/department/${department.code}/settings/extensions" /></#macro>
<#macro extensionreviewrequest assignment uniId><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/extensions/review-request/${uniId}" /></#macro>
<#macro extensionadd assignment uniId><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/extensions/add?universityId=${uniId}" /></#macro>
<#macro extensionedit assignment uniId><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/extensions/edit/${uniId}" /></#macro>
<#macro extensiondelete assignment uniId><@url page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/extensions/delete/${uniId}" /></#macro>

<#macro feedbacktemplates department><@url page="/admin/department/${department.code}/settings/feedback-templates" /></#macro>
<#macro feedbacktemplateedit department feedbacktemplate><@url page="/admin/department/${department.code}/settings/feedback-templates/edit/${feedbacktemplate.id}" /></#macro>
<#macro feedbacktemplatedownload department feedbacktemplate><@url page="/admin/department/${department.code}/settings/feedback-templates/download/${feedbacktemplate.id}/${feedbacktemplate.attachment.name}" /></#macro>
<#macro feedbacktemplatedelete department feedbacktemplate><@url page="/admin/department/${department.code}/settings/feedback-templates/delete/${feedbacktemplate.id}" /></#macro>

<#macro markingworkflowlist department><@url page="/admin/department/${department.code}/markingworkflows" /></#macro>
<#macro markingworkflowadd department><@markingworkflowlist department />/add</#macro>
<#macro markingworkflowedit scheme><@markingworkflowlist scheme.department />/edit/${scheme.id}</#macro>
<#macro markingworkflowdelete scheme><@markingworkflowlist scheme.department />/delete/${scheme.id}</#macro>

<#macro feedbackreport department><@url page="/admin/department/${department.code}/reports/feedback" /></#macro>


<#macro copyDepartmentsAssignments department><@url page="/admin/department/${department.code}/copy-assignments" /></#macro>
<#macro archiveDepartmentsAssignments department><@url page="/admin/department/${department.code}/archive-assignments" /></#macro>

<#macro displaysettings department><@url page="/department/${department.code}/settings/display" context="/admin" /></#macro>

<#-- non admin -->
<#macro assignment assignment><@url page="/module/${assignment.module.code}/${assignment.id}"/></#macro>
<#macro extensionRequest assignment><@url page="/module/${assignment.module.code}/${assignment.id}/extension"/></#macro>
<#macro assignmentreceipt assignment><@url page="/module/${assignment.module.code}/${assignment.id}/resend-receipt"/></#macro>
<#macro assignmentrequestaccess assignment><@url page="/module/${assignment.module.code}/${assignment.id}/request-access"/></#macro>
<#macro feedbackPdf assignment><@url page="/module/${assignment.module.code}/${assignment.id}/feedback.pdf"/></#macro>
