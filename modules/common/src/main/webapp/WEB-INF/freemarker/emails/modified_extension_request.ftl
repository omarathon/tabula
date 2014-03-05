${student.fullName} has amended their extension request for the assignment '${assignment.name}' for ${assignment.module.code?upper_case}, ${assignment.module.name}. The date or reason fields may have been changed

They have requested until ${requestedExpiryDate} to complete the assignment. The following reasons were given for the request:

${reasonForRequest}

<#include "_extension_request_further_details.ftl" />