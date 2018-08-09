${actorIsRecipient?string("You", "Your ${role}")} recorded a missed meeting<#if !studentIsRecipient && student??> with ${student.fullName}</#if>, which was scheduled for ${dateFormatter.print(meetingRecord.meetingDate)}.

