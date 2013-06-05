This record of your personal tutor meeting has been ${verbed} by ${meetingRecord.creator.fullName}:

${meetingRecord.title} on ${dateFormatter.print(meetingRecord.meetingDate)}
<#if reason??>

Because: "${reason}"

</#if>

Please visit <@url page=profileLink context="/profiles" /> to ${nextActionDescription}.