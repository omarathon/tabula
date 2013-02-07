<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them. 

TODO grab values from the Routes object in code, as that's pretty equivalent and 
	we're repeating ourselves here. OR expose Routes directly.

--><#compress>
<#macro home><@url page="/" /></#macro>
<#macro profile profile><@url page="/view/${profile.universityId}"/></#macro>
<#macro photo profile><@url page="/view/photo/${profile.universityId}.jpg"/></#macro>
<#macro tutorPhoto profile><@url page="/view/photo/${profile.universityId}/tutor.jpg"/></#macro>

<#macro tutor_template department><@url page="/admin/department/${department.code}/tutors/template" /></#macro>
<#macro tutor_edit studentUniId tutor><@url page="/tutor/${studentUniId}/edit?tutorUniId=${tutor.universityId}" /></#macro>
<#macro tutor_edit_no_tutor studentUniId><@url page="/tutor/${studentUniId}/edit" /></#macro>
<!-- <#macro tutor_clear studentUniId><@url page="/tutor/${studentUniId}/edit?tutorUniId=clear" /></#macro> -->
<#macro profile studentUniId><@url page="/view/${studentUniId}"/></#macro>
</#compress>
