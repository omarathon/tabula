Your ${role} meeting with ${partner.fullName} is on ${dateTimeFormatter.print(meetingRecord.meetingDate)}

You can view <#if isAgent>${partner.firstName}'s<#else>your</#if> ${role} meetings at ${url}