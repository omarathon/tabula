This record of your ${role} meeting has been ${verbed} by ${actor.fullName}:

${meetingRecord.title} on ${dateFormatter.print(meetingRecord.meetingDate)}
<#if reason??>

Because: "${reason}"
</#if>