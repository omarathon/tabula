Your extension for the assignment '${assignment.name}' for ${module.code?upper_case}, ${module.name} has been changed.
The modified submission date for this assignment is now ${newExpiryDate}. Please disregard any previous emails regarding this extension.

<#if extension.approvalComments?has_content>
The administrator left the following comments:

${extension.approvalComments}

</#if>
The modified extension deadline is now displayed at the top of the submission page:

<@url page=url context="/coursework" />