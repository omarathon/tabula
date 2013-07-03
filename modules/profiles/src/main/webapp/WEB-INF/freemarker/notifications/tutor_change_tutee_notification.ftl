<#if oldTutor??>
<#if newTutor??>
Your personal tutor has been changed from ${oldTutor.officialName} to ${newTutor.officialName}.
<#else>
${oldTutor.officialName} is no longer assigned as your personal tutor.
</#if>
<#else>
You have been assigned ${newTutor.officialName} as a personal tutor.
</#if>

You can view this information on your student profile page at <@url page='${path}'/>.