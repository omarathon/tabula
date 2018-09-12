${actor.fullName} has ${verbed} a record of your<#if agentRoles?size == 1> ${agentRoles[0]}</#if> meeting<#if meetingRecord.participants?size gt 2> with ${meetingRecord.allParticipantNames}</#if>:

${meetingRecord.title!'A meeting you no longer have permission to view'} at ${dateFormatter.print(meetingRecord.meetingDate)}
<#if reason??>

Because: "${reason}"
</#if>
<#if meetingRecord.approved>

This meeting record has been approved.
<#else>

This meeting record is pending approval by ${meetingRecord.pendingApprovalsDescription}.
</#if>
