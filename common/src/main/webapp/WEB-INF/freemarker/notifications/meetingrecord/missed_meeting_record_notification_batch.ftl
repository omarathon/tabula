${meetings?size} meetings have been recorded as missed:

<#list meetings as meeting>
- ${meeting.actorIsRecipient?string("You", meeting.actor.fullName)} recorded a missed<#if meeting.agentRoles?size == 1> ${meeting.agentRoles[0]}</#if> meeting<#if !meeting.studentIsRecipient || meeting.studentIsActor> with ${meeting.meetingRecord.participantNamesExcept(meeting.actor)}</#if>, which was scheduled for ${meeting.dateTimeFormatter.print(meeting.meetingRecord.meetingDate)}.
</#list>
