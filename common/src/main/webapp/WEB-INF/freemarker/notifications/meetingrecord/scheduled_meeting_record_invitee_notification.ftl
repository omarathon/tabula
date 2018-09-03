${actor.fullName} has <#if verb == 'created'><#if ((actor.warwickId)!"") == ((meetingRecord.student.universityId)!"")>requested<#else>scheduled</#if> a<#else>${verb} a</#if><#if agentRoles?size == 1> ${agentRoles[0]}</#if> meeting with you:

<#if meetingRecord??>
	${meetingRecord.title!'A meeting you no longer have permission to view'} on ${dateTimeFormatter.print(meetingRecord.meetingDate)}
<#else>
	This meeting no longer exists.
</#if>