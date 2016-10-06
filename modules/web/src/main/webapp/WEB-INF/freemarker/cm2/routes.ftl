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
<#macro depthome module><@_u page="#module-${module.code}" /></#macro>

<#macro downloadSubmission submission filename><@_u page="/admin/assignments/${submission.assignment.id}/submissions/download/${submission.id}/${filename?url}"/></#macro>

<#-- non admin -->
<#macro assignment assignment><@_u page="/submission/${assignment.id}"/></#macro>
<#macro extensionRequest assignment><@_u page="/assignment/${assignment.id}/extension"/></#macro>
<#macro extensionRequestAttachment assignment attachment><@_u page="/assignment/${assignment.id}/extension/supporting-file/${attachment.name?url}"/></#macro>

<#macro assignmentreceipt assignment><@_u page="/submission/${assignment.id}/resend-receipt"/></#macro>

<#macro submissionReceiptPdf submission><@_u page="/submission/${submission.assignment.id}/submission-receipt.pdf"/></#macro>
<#macro submissionReceiptPdf_in_profile assignment><@_u page="submission/${submission.assignment.id}/${submission.universityId}/submission-receipt.pdf"/></#macro>

<#macro submissionAttachment submission attachment><@_u page="/submission/${submission.assignment.id}/attachment/${attachment.name?url}" /></#macro>
<#macro submissionAttachment_in_profile submission attachment><@_u page="submission/${submission.assignment.id}/${submission.universityId}/attachment/${attachment.name?url}" /></#macro>

<#macro feedbackPdf assignment feedback><@_u page="/submission/${assignment.id}/${feedback.universityId}/feedback.pdf"/></#macro>

<#macro feedbackAttachment feedback attachment><@_u page="/submission/${feedback.assignment.id}/get/${attachment.name?url}"/></#macro>
