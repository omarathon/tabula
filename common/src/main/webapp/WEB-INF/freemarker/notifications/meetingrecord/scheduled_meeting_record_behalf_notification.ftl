${actor.fullName} has <#if verb == 'created'>scheduled a<#else>${verb} a scheduled</#if><#if agentRoles?size == 1> ${agentRoles[0]}</#if> meeting with ${meetingRecord.allParticipantNames} on your behalf:

<#if meetingRecord??>
	${meetingRecord.title!'A meeting you no longer have permission to view'} on ${dateTimeFormatter.print(meetingRecord.meetingDate)}
<#else>
	This meeting no longer exists.
</#if>
