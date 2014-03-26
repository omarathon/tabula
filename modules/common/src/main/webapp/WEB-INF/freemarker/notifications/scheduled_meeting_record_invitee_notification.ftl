${actor.fullName} has <#if verb == 'created'>scheduled a<#else>${verb} a scheduled</#if> ${role} meeting with you:

${meetingRecord.title} on ${dateTimeFormatter.print(meetingRecord.meetingDate)}