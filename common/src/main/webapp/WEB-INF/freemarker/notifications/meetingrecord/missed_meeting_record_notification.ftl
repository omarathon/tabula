${actorIsRecipient?string("You", actor.fullName)} recorded a missed<#if agentRoles?size == 1> ${agentRoles[0]}</#if> meeting<#if !studentIsRecipient || studentIsActor> with ${meetingRecord.participantNamesExcept(actor)}</#if>, which was scheduled for ${dateTimeFormatter.print(meetingRecord.meetingDate)}.

