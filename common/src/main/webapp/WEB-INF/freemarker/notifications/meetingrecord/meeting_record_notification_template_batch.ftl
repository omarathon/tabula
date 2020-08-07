${meetings?size} meetings have been ${verbed}:

<#list meetings as meeting>
- ${meeting.meetingRecord.title!'A meeting you no longer have permission to view'}<#if meeting.agentRoles?size == 1> ${meeting.agentRoles[0]}</#if> meeting<#if meeting.meetingRecord.participants?size gt 2> with ${meeting.meetingRecord.allParticipantNames}<#else> with ${meeting.actor.fullName}</#if> on ${meeting.dateTimeFormatter.print(meeting.meetingRecord.meetingDate)}<#if meeting.reason??> because "${meeting.reason}"</#if>. <#if meeting.meetingRecord.approved>This meeting record has been approved.<#elseif meeting.meetingRecord.rejected>This meeting record is pending revision.<#else>This meeting record is pending approval by ${meeting.meetingRecord.pendingApprovalsDescription}.</#if>
</#list>
