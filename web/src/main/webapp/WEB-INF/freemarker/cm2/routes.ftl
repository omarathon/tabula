<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.
TODO grab values from the Routes object in code, as that's pretty equivalent and
   we're repeating ourselves here. OR expose Routes directly.
-->
<#macro _u page context=component.context?default('/cm2')>
	<@url context=context page=page />
</#macro>
<#macro home><@_u page="/" /></#macro>
<#-- to rename - also filter options will need to be added to jump to correct module -->
<#macro depthome module><@_u page="/admin/department/${module.adminDepartment.code}/#module-${module.code}" /></#macro>
<#macro departmenthome department><@_u page="/admin/department/${department.code}/" /></#macro>
<#macro downloadSubmission submission filename><@_u page="/admin/assignments/${submission.assignment.id}/submissions/download/${submission.id}/${filename?url}"/></#macro>

<#macro filterExtensions><@_u page="/admin/extensions"/></#macro>
<#macro extensionDetail extension><@_u page="/admin/extensions/${extension.id}/detail"/></#macro>
<#macro extensiondetail assignment usercode><@_u page="/admin/assignments/${assignment.id}/extensions/${usercode}/detail" /></#macro>
<#macro extensionUpdate extension><@_u page="/admin/extensions/${extension.id}/update"/></#macro>
<#macro extensionAttachment extension filename><@_u page="/admin/extensions/${extension.id}/supporting-file/${filename?url}" /></#macro>
<#macro extensionSettings department><@_u page="/admin/department/${department.code}/settings/extensions" /></#macro>

<#macro reusableWorkflowsHome department academicYear><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/markingworkflows" /></#macro>
<#macro reusableWorkflowAdd department academicYear><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/markingworkflows/add" /></#macro>
<#macro reusableWorkflowAddToCurrentYear department academicYear workflow><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/markingworkflows/${workflow.id}/copy" /></#macro>
<#macro reusableWorkflowEdit department academicYear workflow><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/markingworkflows/${workflow.id}/edit" /></#macro>
<#macro reusableWorkflowDelete department academicYear workflow><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/markingworkflows/${workflow.id}/delete" /></#macro>
<#macro reusableWorkflowReplaceMarker department academicYear workflow><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/markingworkflows/${workflow.id}/replace" /></#macro>

<#macro feedbackreport department><@_u page="/admin/department/${department.code}/reports/feedback" /></#macro>

<#macro createassignmentdetails module><@_u page="/admin/${module.code}/assignments/new" /></#macro>
<#macro assignmentfeedback assignment mode><@_u page="/admin/assignments/${assignment.id}/${mode}/feedback" /></#macro>
<#macro assignmentstudents assignment mode><@_u page="/admin/assignments/${assignment.id}/${mode}/students" /></#macro>
<#macro assignmentmarkers assignment mode><@_u page="/admin/assignments/${assignment.id}/${mode}/markers" /></#macro>
<#macro assignmentsubmissions assignment mode><@_u page="/admin/assignments/${assignment.id}/${mode}/submissions" /></#macro>
<#macro assignmentoptions assignment mode><@_u page="/admin/assignments/${assignment.id}/${mode}/options" /></#macro>
<#macro assignmentreview assignment><@_u page="/admin/assignments/${assignment.id}/review" /></#macro>

<#macro create_sitsassignments department><@_u page="/admin/department/${department.code}/setup-assignments" /></#macro>
<#macro assignmentSharedOptions department><@_u page="/admin/department/${department.code}/shared-options" /></#macro>

<#macro editassignmentdetails assignment><@_u page="/admin/assignments/${assignment.id}/edit" /></#macro>

<#macro feedbackSummary assignment studentid><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/summary/${studentid}"/></#macro>
<#macro feedbackAudit assignment studentid><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/audit/${studentid}"/></#macro>
<#macro onlinefeedback assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/online"/></#macro>
<#macro feedbackAdjustment assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/adjustments"/></#macro>


<#macro feedbacktemplates department><@_u page="/admin/department/${department.code}/settings/feedback-templates" /></#macro>
<#macro editfeedbacktemplate department template><@_u page="/admin/department/${department.code}/settings/feedback-templates/edit/${template.id}" /></#macro>
<#macro deletefeedbacktemplate department template><@_u page="/admin/department/${department.code}/settings/feedback-templates/delete/${template.id}" /></#macro>
<#macro feedbacktemplatedownload department feedbacktemplate><@_u page="/admin/department/${department.code}/settings/feedback-templates/download/${feedbacktemplate.id}/${feedbacktemplate.attachment.name}" /></#macro>

<#-- non admin -->
<#macro assignment assignment><@_u page="/submission/${assignment.id}"/></#macro>
<#macro extensionRequest assignment><@_u page="/assignment/${assignment.id}/extension"/></#macro>
<#macro extensionRequestAttachment assignment attachment><@_u page="/assignment/${assignment.id}/extension/supporting-file/${attachment.name?url}"/></#macro>
<#macro assignmentreceipt assignment><@_u page="/submission/${assignment.id}/resend-receipt"/></#macro>
<#macro submissionReceiptPdf submission><@_u page="/submission/${submission.assignment.id}/submission-receipt.pdf"/></#macro>
<#macro submissionReceiptPdf_in_profile assignment><@_u page="submission/${submission.assignment.id}/${submission.usercode}/submission-receipt.pdf"/></#macro>
<#macro submissionAttachment submission attachment><@_u page="/submission/${submission.assignment.id}/attachment/${attachment.name?url}" /></#macro>
<#macro submissionAttachment_in_profile submission attachment><@_u page="submission/${submission.assignment.id}/${submission.usercode}/attachment/${attachment.name?url}" /></#macro>
<#macro feedbackPdf assignment feedback><@_u page="/submission/${assignment.id}/${feedback.usercode}/feedback.pdf"/></#macro>
<#macro feedbackAttachment feedback attachment><@_u page="/submission/${feedback.assignment.id}/get/${attachment.name?url}"/></#macro>
<#macro assignemnts_json module><@_u page="/admin/${module.code}/assignments" /></#macro>
<#macro enrolment assignment><@_u page="/admin/assignments/${assignment.id}/enrolment"/></#macro>

<#macro submitToTurnitin assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/turnitin"/></#macro>
<#macro turnitinLtiReport assignment attachment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/turnitin/lti-report/${attachment.id}"/></#macro>
<#macro turnitinReport assignment attachment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/turnitin/report/${attachment.id}"/></#macro>

<#macro listmarkersubmissions assignment marker><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/${marker.warwickId}/list"/></#macro>

<#macro assignmentsubmissionsandfeedback assignment><@_u page="/admin/assignments/${assignment.id}/list"/></#macro>
<#macro assignmentsubmissionsandfeedbacktable assignment><@_u page="/admin/assignments/${assignment.id}/table"/></#macro>
<#macro assignmentsubmissionsandfeedbacksummary assignment><@_u page="/admin/assignments/${assignment.id}/summary"/></#macro>

<#macro genericfeedback assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/feedback/generic"/></#macro>
<#macro uploadToSits assignment><@_u page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/upload-to-sits"/></#macro>
<#macro checkSitsUpload feedback><@_u page="/admin/module/${feedback.assignment.module.code}/assignments/${feedback.assignment.id}/feedback/${feedback.id}/check-sits"/></#macro>
