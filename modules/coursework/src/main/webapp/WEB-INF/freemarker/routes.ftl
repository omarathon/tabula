<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.

TODO grab values from the Routes object in code, as that's pretty equivalent and
	we're repeating ourselves here. OR expose Routes directly.

-->

<#macro _u page context='/coursework'><@url context=context page=page /></#macro>

<#macro home><@_u page="/" /></#macro>
<#macro departmenthome department><@_u page="/admin/department/${department.code}/" /></#macro>
<#macro modulehome module><@_u page="/admin/module/${module.code}/" /></#macro>
<#macro depthome module><@_u page="/admin/department/${module.department.code}/#module-${module.code}" /></#macro>
<#macro moduleperms module><@_u page="/module/${module.code}/permissions" context="/admin" /></#macro>

<#macro ratefeedback feedback><#compress>
    <#assign assignment=feedback.assignment />
    <#assign module=assignment.module />
    <@_u page="/module/${module.code}/${assignment.id}/rate" />
</#compress></#macro>
<#macro assignmentdelete assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/delete"/></#macro>
<#macro assignmentedit assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/edit"/></#macro>

<#macro feedbackSummary assignment studentid><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/summary/${studentid}"/></#macro>
<#macro plagiarismInvestigation assignment><@_u page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/submissionsandfeedback/mark-plagiarised'/></#macro>
<#macro onlinefeedback assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/online"/></#macro>
<#macro markerOnlinefeedback assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/feedback/online"/></#macro>
<#macro onlinefeedbackform assignment studentid><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/online/${studentid}"/></#macro>
<#macro markerOnlinefeedbackform assignment studentid><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/feedback/online/${studentid}"/></#macro>
<#macro markerModerationform assignment studentid><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/feedback/online/moderation/${studentid}"/></#macro>

<#macro genericfeedback assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/generic"/></#macro>
<#macro markerFeedbackFiles assignment markerFeedback><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/feedback/download/${markerFeedback.id}/feedback-${markerFeedback.feedback.universityId}.zip"/></#macro>

<#macro feedbackZip assignment feedback attachmentExtension><@_u page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/download/${feedback.id}/feedback-${feedback.universityId}.${attachmentExtension}'/></#macro>

<#macro markingCompleted assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/marking-completed" /></#macro>

<#macro enrolment module><@_u page="/admin/module/${module.code}/assignments/enrolment"/></#macro>

<#macro createAssignment module><@_u page="/admin/module/${module.code}/assignments/new" /></#macro>
<#macro copyModuleAssignments module><@_u page="/admin/module/${module.code}/copy-assignments" /></#macro>
<#macro archiveModuleAssignments module><@_u page="/admin/module/${module.code}/archive-assignments" /></#macro>

<#macro addMarks assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marks" /></#macro>
<#macro addFeedback assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/batch" /></#macro>

<#macro listmarkersubmissions assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/list"/></#macro>
<#macro downloadmarkersubmissions assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/submissions.zip"/></#macro>
<#macro downloadfirstmarkerfeedback assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/firstmarker/feedbacks.zip"/></#macro>
<#macro uploadmarkerfeedback assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/feedback"/></#macro>
<#macro markeraddmarks assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/marks"/></#macro>

<#macro assignmentsubmissionsandfeedback assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/list"/></#macro>
<#macro assignmentsubmissionsandfeedbacktable assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/table"/></#macro>
<#macro assignmentsubmissionsandfeedbacksummary assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/summary"/></#macro>

<#macro assignMarkers assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/assign-markers" /></#macro>
<#macro releaseForMarking assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/submissionsandfeedback/release-submissions" /></#macro>

<#macro onlinemarking assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/online" /></#macro>
<#macro onlinemarkingform assignment student><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/online/${student.universityId}" /></#macro>

<#macro feedbackadd assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/add" /></#macro>
<#macro feedbackdelete assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/delete" /></#macro>
<#macro markstemplate assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marks-template" /></#macro>
<#macro markermarkstemplate assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/marks-template" /></#macro>
<#macro extensions assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/extensions" /></#macro>
<#macro extensionreviewattachment assignment userid filename><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/extensions/review-request/${userid}/supporting-file/${filename}" /></#macro>
<#macro extensionrequestattachment assignment filename><@_u page="/module/${assignment.module.code}/${assignment.id}/extension/supporting-file/${filename}" /></#macro>

<#macro extensionsettings department><@_u page="/admin/department/${department.code}/settings/extensions" /></#macro>
<#macro extensionreviewrequest assignment uniId><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/extensions/review-request/${uniId}" /></#macro>
<#macro extensionadd assignment uniId><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/extensions/add?universityId=${uniId}" /></#macro>
<#macro extensionedit assignment uniId><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/extensions/edit/${uniId}" /></#macro>
<#macro extensiondelete assignment uniId><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/extensions/delete/${uniId}" /></#macro>

<#macro feedbacktemplates department><@_u page="/admin/department/${department.code}/settings/feedback-templates" /></#macro>
<#macro feedbacktemplateedit department feedbacktemplate><@_u page="/admin/department/${department.code}/settings/feedback-templates/edit/${feedbacktemplate.id}" /></#macro>
<#macro feedbacktemplatedownload department feedbacktemplate><@_u page="/admin/department/${department.code}/settings/feedback-templates/download/${feedbacktemplate.id}/${feedbacktemplate.attachment.name}" /></#macro>
<#macro feedbacktemplatedelete department feedbacktemplate><@_u page="/admin/department/${department.code}/settings/feedback-templates/delete/${feedbacktemplate.id}" /></#macro>

<#macro markingworkflowlist department><@_u page="/admin/department/${department.code}/markingworkflows" /></#macro>
<#macro markingworkflowadd department><@markingworkflowlist department />/add</#macro>
<#macro markingworkflowedit scheme><@markingworkflowlist scheme.department />/edit/${scheme.id}</#macro>
<#macro markingworkflowdelete scheme><@markingworkflowlist scheme.department />/delete/${scheme.id}</#macro>

<#macro feedbackreport department><@_u page="/admin/department/${department.code}/reports/feedback" /></#macro>


<#macro copyDepartmentsAssignments department><@_u page="/admin/department/${department.code}/copy-assignments" /></#macro>
<#macro archiveDepartmentsAssignments department><@_u page="/admin/department/${department.code}/archive-assignments" /></#macro>

<#macro displaysettings department><@_u page="/department/${department.code}/settings/display" context="/admin" /></#macro>

<#-- non admin -->
<#macro assignment assignment><@_u page="/module/${assignment.module.code}/${assignment.id}"/></#macro>
<#macro extensionRequest assignment><@_u page="/module/${assignment.module.code}/${assignment.id}/extension"/></#macro>
<#macro assignmentreceipt assignment><@_u page="/module/${assignment.module.code}/${assignment.id}/resend-receipt"/></#macro>
<#macro assignmentrequestaccess assignment><@_u page="/module/${assignment.module.code}/${assignment.id}/request-access"/></#macro>
<#macro feedbackPdf assignment><@_u page="/module/${assignment.module.code}/${assignment.id}/feedback.pdf"/></#macro>
