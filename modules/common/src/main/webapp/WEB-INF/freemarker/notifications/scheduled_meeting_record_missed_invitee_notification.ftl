${actor.fullName} has recorded that a scheduled ${role} meeting with you did not take place:

${meetingRecord.title} on ${dateTimeFormatter.print(meetingRecord.meetingDate)}

<#if meetingRecord.missedReason?has_content>
	"${meetingRecord.missedReason}"
</#if>