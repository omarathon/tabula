${actor.fullName} has marked that the following scheduled ${role} meeting did not take place:

${meetingRecord.title} on ${dateTimeFormatter.print(meetingRecord.meetingDate)}

<#if meetingRecord.missedReason?has_content>
	"${meetingRecord.missedReason}"
</#if>