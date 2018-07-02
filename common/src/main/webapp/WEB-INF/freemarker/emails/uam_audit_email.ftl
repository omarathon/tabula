<#macro deparmentAsSentence><#list departments as department>${department.departmentName}<#if department_has_next>, <#if department_index + 1 == departments?size - 1>and </#if></#if></#list></#macro>
We are contacting you because you currently hold the User Access Manager (UAM) role in Tabula. The person assigned to this role should be in a position to oversee the administration of the departments and sub-departments listed in this notification.

To satisfy data audit requirements, please complete the Tabula User Audit form here:

${url}

In the form, we ask you to confirm that you can continue to perform this role for <@deparmentAsSentence /> for the academic year ${academicYear} and that you have checked that permission levels in Tabula are accurate.

Here is a list of departments and sub-departments that you should check:

<#list departments as department>${department.departmentName} - https://tabula.warwick.ac.uk${department.permissionTreeUrl} <#if department_has_next>

</#if></#list>


- Ensure that staff in your department have the appropriate permission levels.
- Ensure that only those staff necessary have permission to view students’ personal information.
- In accepting the UAM role, you agree that you are responsible for the accuracy of these permissions - and will monitor permissions periodically. If you are unable to monitor permissions in the future, you should request that the UAM role is assigned to another person within your department.

For audit purposes, this should be done by ${permissionConfirmation}.

If you no longer wish to be the UAM or are unable to check the departmental permissions within this timeframe, please let us know as soon as possible. We’ll remove the User Access Manager permissions from your account and ask your Head of Department to assign the role to another staff member.

If you have any questions or wish to discuss this further, please contact the Tabula Team via tabula@warwick.ac.uk.