This message confirms that your extension request has been approved for the assignment '${assignment.name}' for ${module.code?upper_case}, ${module.name}.
Your new submission date for this assignment is ${newExpiryDate}. Any submissions made after this date will be subject to the usual late penalties.

<#if extension.reviewerComments?has_content>
The administrator left the following comments:

${extension.reviewerComments}

</#if>