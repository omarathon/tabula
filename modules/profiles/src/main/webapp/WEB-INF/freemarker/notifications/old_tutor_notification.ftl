<#if newTutor??>
Your tutee ${tutee.officialName} has now been reassigned to ${newTutor.officialName}.
<#else>
You are no longer assigned as personal tutor to ${tutee.officialName}.
</#if>
The student profile for ${tutee.officialName} can be found at <@url page='${path}'/>.