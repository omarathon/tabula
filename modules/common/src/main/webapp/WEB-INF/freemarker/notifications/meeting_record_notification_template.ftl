${actor.fullName} has ${verbed} a record of your ${role} meeting:

${meetingRecord.title} at ${dateFormatter.print(meetingRecord.meetingDate)}
<#if reason??>

Because: "${reason}"
</#if>