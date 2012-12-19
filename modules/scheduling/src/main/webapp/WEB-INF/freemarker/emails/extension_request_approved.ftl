Dear ${user.firstName}

This email confirms that your extension request has been approved for the assignment '${assignment.name}' for ${module.code?upper_case}, ${module.name}.
Your new submission date for this assignment is ${newExpiryDate}. Any submissions made after this date will be subject to the usual late penalties.

<#if extension.approvalComments?has_content>
The administrator left the following comments:

${extension.approvalComments}

</#if>
Your new deadline is now displayed at the top of the submission page:

<@url page=url context="/coursework" />


This email was sent from an automated system, and replies to it will not reach a real person.