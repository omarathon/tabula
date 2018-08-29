${actor.fullName} has recorded that a scheduled<#if agentRoles?size == 1> ${agentRoles[0]}</#if> meeting with ${meeting.allParticipantNames} did not take place:

${meetingRecord.title!'A meeting you no longer have permission to view'} on ${dateTimeFormatter.print(meetingRecord.meetingDate)}

<#if meetingRecord.missedReason?has_content>
	"${meetingRecord.missedReason}"
</#if>