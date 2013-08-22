<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.

TODO grab values from the Routes object in code, as that's pretty equivalent and
	we're repeating ourselves here. OR expose Routes directly.

-->
<#macro home><@url page="/" /></#macro>

<#macro deptperms department><@url page="/department/${department.code}/permissions" context="/admin" /></#macro>

<#macro search><@url page="/search" /></#macro>
<#macro profile profile><@url page="/view/${profile.universityId}"/></#macro>
<#macro profile_by_id student><@url page="/view/${student}"/></#macro>
<#macro photo profile><@url page="/view/photo/${profile.universityId}.jpg"/></#macro>
<#macro relationshipPhoto profile relationship><@url page="/view/photo/${relationship.agent}.jpg"/></#macro>

<#macro tutees><@url page="/tutees" /></#macro>
<#macro supervisees><@url page="/supervisees" /></#macro>
<#macro tutors department><@url page="/department/${department.code}/tutors" /></#macro>
<#macro tutors_missing department><@url page="/department/${department.code}/tutors/missing" /></#macro>
<#macro tutors_allocate department><@url page="/department/${department.code}/tutors/allocate" /></#macro>
<#macro tutor_template department><@url page="/department/${department.code}/tutors/template" /></#macro>

<#macro tutor_edit scjCode currentTutor>
	<@url page="/tutor/${scjCode}/edit?currentTutor=${currentTutor.universityId}" />
</#macro>

<#macro tutor_edit_set scjCode newTutor>
	<@url page="/tutor/${scjCode}/edit?tutor=${newTutor.universityId}" />
</#macro>

<#macro tutor_edit_replace scjCode currentTutor newTutor>
	<@url page="/tutor/${scjCode}/edit?currentTutor=${currentTutor.universityId}&tutor=${newTutor.universityId}" />
</#macro>

<#macro tutor_edit_no_tutor scjCode>
	<@url page="/tutor/${scjCode}/add" />
</#macro>

<#macro meeting_record scjCode role>
	<@url page="/${role}/meeting/${scjCode}/create" />
</#macro>
<#macro edit_meeting_record scjCode meeting_record role>
	<@url page="/${role}/meeting/${scjCode}/edit/${meeting_record.id}" />
</#macro>

<#macro delete_meeting_record meeting_record role><@url page="/${role}/meeting/${meeting_record.id}/delete" /></#macro>
<#macro restore_meeting_record meeting_record role><@url page="/${role}/meeting/${meeting_record.id}/restore" /></#macro>
<#macro purge_meeting_record meeting_record role><@url page="/${role}/meeting/${meeting_record.id}/purge" /></#macro>


<#macro save_meeting_approval meeting_record role><@url page="/${role}/meeting/${meeting_record.id}/approval" /></#macro>

<#macro smallgroup group><@url page="/groups/${group.id}/view" /></#macro>

