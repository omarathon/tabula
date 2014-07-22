${actor.fullName} has <#if verb == 'created'>scheduled a<#else>${verb} a scheduled</#if> ${role} meeting with you:

<#if meetingRecord??>
	${meetingRecord.title} on ${dateTimeFormatter.print(meetingRecord.meetingDate)}
<#else>
	This meeting no longer exists.
</#if>