${student.fullName} has requested an extension for the assignment '${assignment.name}' for ${assignment.module.code?upper_case}, ${assignment.module.name}.

They have requested until ${requestedExpiryDate} to complete the assignment. The following reasons were given for the request:

${reasonForRequest}

<#include "_extension_request_further_details.ftl" />

To review this extension request and any supporting documentation, please click the link below.

<@url page=path context="/coursework" />