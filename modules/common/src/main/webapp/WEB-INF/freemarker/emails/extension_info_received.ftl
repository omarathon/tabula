${student.fullName} (${student.warwickId}) has replied to your request for further information regarding their extension request for the assignment '${assignment.name}' for ${assignment.module.code?upper_case} ${assignment.module.name}.

They have requested an extension until ${requestedExpiryDate} and gave the following reasons:

${reasonForRequest}

<#include "_extension_request_further_details.ftl" />