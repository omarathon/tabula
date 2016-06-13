<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.

TODO grab values from the Routes object in code, as that's pretty equivalent and
	we're repeating ourselves here. OR expose Routes directly.

-->
<#macro _u page context='/profiles'><@url context=context page=page /></#macro>

<#macro home><@_u page="/" /></#macro>

<#macro deptperms department><@_u page="/department/${department.code}/permissions" context="/admin" /></#macro>
<#macro displaysettings department><@_u page="/department/${department.code}/settings/display" context="/admin" /></#macro>

<#macro search><@_u page="/search" /></#macro>
<#macro profile profile><@_u page="/view/${profile.universityId}"/></#macro>
<#macro profile_by_id student><@_u page="/view/${student}"/></#macro>
<#macro photo profile><#if ((profile.universityId)!)?has_content><@_u page="/view/photo/${profile.universityId}.jpg"/><#else><@url resource="/static/images/no-photo.jpg" /></#if></#macro>
<#macro relationshipPhoto profile relationship><@_u page="/view/photo/${profile.universityId}/${relationship.relationshipType.urlPart}/${relationship.agent}.jpg"/></#macro>

<#macro filter_students department><@_u page="/department/${department.code}/students" /></#macro>
<#macro department_timetables department><@_u page="/department/${department.code}/timetables" /></#macro>
<#macro draft_department_timetables department academicYear endpoint><@_u page="/department/${department.code}/timetables/drafts/${academicYear.startYear?c}/${endpoint}" /></#macro>

<#macro relationship_students relationshipType><@_u page="/${relationshipType.urlPart}/students" /></#macro>
<#macro relationship_agents department relationshipType><@_u page="/department/${department.code}/${relationshipType.urlPart}" /></#macro>
<#macro relationship_missing department relationshipType><@_u page="/department/${department.code}/${relationshipType.urlPart}/missing" /></#macro>
<#macro relationship_allocate department relationshipType><@_u page="/department/${department.code}/${relationshipType.urlPart}/allocate" /></#macro>
<#macro relationship_template department relationshipType><@_u page="/department/${department.code}/${relationshipType.urlPart}/allocate/template" /></#macro>
<#macro relationship_allocate_upload department relationshipType><@_u page="/department/${department.code}/${relationshipType.urlPart}/allocate/upload" /></#macro>
<#macro relationship_allocate_preview department relationshipType><@_u page="/department/${department.code}/${relationshipType.urlPart}/allocate/preview" /></#macro>

<#macro relationship_edit relationshipType scjCode currentAgent>
	<@_u page="/${relationshipType.urlPart}/${scjCode}/edit?currentAgent=${currentAgent.universityId}" />
</#macro>

<#macro relationship_edit_no_agent relationshipType scjCode>
	<@_u page="/${relationshipType.urlPart}/${scjCode}/add" />
</#macro>

<#macro create_meeting_record scd academicYear relationshipType><@_u page="/${relationshipType.urlPart}/meeting/${scd.urlSafeId}/${academicYear.startYear?c}/create" /></#macro>
<#macro edit_meeting_record scd academicYear meeting_record><@_u page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${scd.urlSafeId}/${academicYear.startYear?c}/edit/${meeting_record.id}" /></#macro>

<#macro delete_meeting_record meeting_record><@_u page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${meeting_record.id}/delete" /></#macro>
<#macro restore_meeting_record meeting_record><@_u page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${meeting_record.id}/restore" /></#macro>
<#macro purge_meeting_record meeting_record><@_u page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${meeting_record.id}/purge" /></#macro>
<#macro save_meeting_approval meeting_record><@_u page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${meeting_record.id}/approval" /></#macro>

<#macro download_meeting_record_attachment relationshipType meeting><@_u page="/${relationshipType.urlPart}/meeting/${meeting.id}/"/></#macro>

<#macro create_scheduled_meeting_record scd academicYear relationshipType><@_u page="/${relationshipType.urlPart}/meeting/${scd.urlSafeId}/${academicYear.startYear?c}/schedule/create" /></#macro>
<#macro edit_scheduled_meeting_record meetingRecord scd academicYear relationshipType><@_u page="/${relationshipType.urlPart}/meeting/${scd.urlSafeId}/${academicYear.startYear?c}/schedule/${meetingRecord.id}/edit" /></#macro>
<#macro choose_action_scheduled_meeting_record meetingRecord scd academicYear relationshipType><@_u page="/${relationshipType.urlPart}/meeting/${scd.urlSafeId}/${academicYear.startYear?c}/schedule/${meetingRecord.id}/chooseaction" /></#macro>
<#macro confirm_scheduled_meeting_record meetingRecord scd academicYear relationshipType><@_u page="/${relationshipType.urlPart}/meeting/${scd.urlSafeId}/${academicYear.startYear?c}/schedule/${meetingRecord.id}/confirm" /></#macro>
<#macro missed_scheduled_meeting_record meetingRecord relationshipType><@_u page="/${relationshipType.urlPart}/meeting/${meetingRecord.id}/missed" /></#macro>

<#macro relationship_search_json><@_u page="/relationships/agents/search.json" /></#macro>

<#macro smallgroup group><@_u page="/groups/${group.id}/view" /></#macro>

<#macro create_member_note profile><@_u page="/${profile.universityId}/note/add" /></#macro>
<#macro edit_member_note memberNote><@_u page="/${memberNote.member.universityId}/note/${memberNote.id}/edit" /></#macro>
<#macro delete_member_note memberNote ><@_u page="/${memberNote.member.universityId}/note/${memberNote.id}/delete" /></#macro>
<#macro restore_member_note memberNote ><@_u page="/${memberNote.member.universityId}/note/${memberNote.id}/restore" /></#macro>
<#macro purge_member_note memberNote ><@_u page="/${memberNote.member.universityId}/note/${memberNote.id}/purge" /></#macro>
<#macro download_member_note_attachment memberNote><@_u page="/notes/${memberNote.id}/" /></#macro>

<#macro create_circumstances profile><@_u page="/${profile.universityId}/circumstances/add" /></#macro>
<#macro edit_circumstances circumstances><@_u page="/${circumstances.member.universityId}/circumstances/${circumstances.id}/edit" /></#macro>
<#macro delete_circumstances circumstances ><@_u page="/${circumstances.member.universityId}/circumstances/${circumstances.id}/delete" /></#macro>
<#macro restore_circumstances circumstances ><@_u page="/${circumstances.member.universityId}/circumstances/${circumstances.id}/restore" /></#macro>
<#macro purge_circumstances circumstances ><@_u page="/${circumstances.member.universityId}/circumstances/${circumstances.id}/purge" /></#macro>
<#macro download_circumstances_attachment circumstances><@_u page="/circumstances/${circumstances.id}/" /></#macro>

<#macro edit_monitoringpoint_attendance_note student point><@_u page="/attendance/note/${student.universityId}/${point.id}/edit" /></#macro>

<#macro meeting_will_create_checkpoint><@_u page="/check/meeting" context="/attendance" /></#macro>

<#macro timetable profile><@_u page="/timetable/${profile.universityId}"/></#macro>
<#macro timetable_ical profile webcal=true><#compress>
	<#local https_url><@_u context="/api/v1" page="/timetable/calendar/${profile.timetableHash}.ics" /></#local>
	<#if webcal>
		${https_url?replace('https','webcal')}
	<#else>
		${https_url}
	</#if>
</#compress></#macro>
<#macro timetable_ical_regenerate><@_u page="/timetable/regeneratehash" /></#macro>
<#macro timetable_download profile><@_u page="/view/${profile.universityId}/timetable/download"/></#macro>

<#macro mrm_link studentCourseDetails studentCourseYearDetails>
	<a href="https://mrm.warwick.ac.uk/mrm/student/student.htm?sprCode=${((studentCourseDetails.sprCode)!)?url}&acYear=${((studentCourseYearDetails.academicYear.toString)!)?url}" target="_blank">
</#macro>

<#macro permissions scope><@_u page="/permissions/${scope.urlCategory}/${scope.urlSlug}" context="/admin" /></#macro>

<#macro agentHomeForYear relationshipType academicYearString><@_u page="/agent/${relationshipType.urlPart}/${academicYearString}" context="/attendance" /></#macro>
<#macro listmarkersubmissions assignment marker><@_u context="/coursework" page="/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/${marker.warwickId}/list"/></#macro>

<#macro listMeetings relationshipType scjCode academicYear><@_u page="/view/meetings/${relationshipType.urlPart}/${scjCode}/${academicYear.startYear?c}"/></#macro>
<#macro listMeetingsTargetted relationshipType scjCode academicYear meetingId><@_u page="/view/meetings/${relationshipType.urlPart}/${scjCode}/${academicYear.startYear?c}?meeting=${meetingId}"/></#macro>

<#macro listModuleRegs scjCode academicYear><@_u page="/view/modules/${scjCode}/${academicYear.startYear?c}"/></#macro>

<#macro exportProfiles department academicYear filterString>
	<#if filterString?has_content>
		<#local filterString>?hasBeenFiltered=true&${filterString}</#local>
	</#if>
	<@_u context="/reports" page="/${department.code}/${academicYear.startYear?c}/profiles/export${filterString}"/>
</#macro>
<#macro peoplesearchData profile><@_u page="/view/peoplesearch/${profile.universityId}"/></#macro>
