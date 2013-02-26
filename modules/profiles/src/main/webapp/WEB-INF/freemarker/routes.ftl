<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them. 

TODO grab values from the Routes object in code, as that's pretty equivalent and 
	we're repeating ourselves here. OR expose Routes directly.

--><#compress>
<#macro home><@url page="/" /></#macro>
<#macro search><@url page="/search" /></#macro>
<#macro profile profile><@url page="/view/${profile.universityId}"/></#macro>
<#macro profile_by_id student><@url page="/view/${student}"/></#macro>
<#macro photo profile><@url page="/view/photo/${profile.universityId}.jpg"/></#macro>
<#macro tutorPhoto profile><@url page="/view/photo/${profile.universityId}/tutor.jpg"/></#macro>

<#macro tutees><@url page="/tutees" /></#macro>
<#macro tutors department><@url page="/department/${department.code}/tutors" /></#macro>
<#macro tutors_missing department><@url page="/department/${department.code}/tutors/missing" /></#macro>

<#macro tutor_upload department><@url page="/department/${department.code}/tutors/upload" /></#macro>
<#macro tutor_template department><@url page="/department/${department.code}/tutors/template" /></#macro>
<#macro tutor_edit student tutor><@url page="/tutor/${student}/edit?tutor=${tutor.universityId}" /></#macro>
<#macro tutor_edit_no_tutor student><@url page="/tutor/${student}/edit" /></#macro>

</#compress>
