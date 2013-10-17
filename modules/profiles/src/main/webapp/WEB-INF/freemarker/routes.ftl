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
<#macro photo profile><#if ((profile.universityId)!)?has_content><@url page="/view/photo/${profile.universityId}.jpg"/><#else><@url resource="/static/images/no-photo.jpg" /></#if></#macro>
<#macro relationshipPhoto profile relationship><@url page="/view/photo/${relationship.agent}.jpg"/></#macro>

<#macro relationship_students relationshipType><@url page="/${relationshipType.urlPart}/students" /></#macro>
<#macro relationship_agents department relationshipType><@url page="/department/${department.code}/${relationshipType.urlPart}" /></#macro>
<#macro relationship_missing department relationshipType><@url page="/department/${department.code}/${relationshipType.urlPart}/missing" /></#macro>
<#macro relationship_allocate department relationshipType><@url page="/department/${department.code}/${relationshipType.urlPart}/allocate" /></#macro>
<#macro relationship_template department relationshipType><@url page="/department/${department.code}/${relationshipType.urlPart}/template" /></#macro>

<#macro relationship_edit relationshipType scjCode currentAgent>
	<@url page="/${relationshipType.urlPart}/${scjCode}/edit?currentAgent=${currentAgent.universityId}" />
</#macro>

<#macro relationship_edit_set relationshipType scjCode newAgent>
	<@url page="/${relationshipType.urlPart}/${scjCode}/edit?agent=${newAgent.universityId}" />
</#macro>

<#macro relationship_edit_replace relationshipType scjCode currentAgent newAgent>
	<@url page="/${relationshipType.urlPart}/${scjCode}/edit?currentAgent=${currentAgent.universityId}&agent=${newAgent.universityId}" />
</#macro>

<#macro relationship_edit_no_agent relationshipType scjCode>
	<@url page="/${relationshipType.urlPart}/${scjCode}/add" />
</#macro>

<#macro meeting_record scjCode relationshipType>
	<@url page="/${relationshipType.urlPart}/meeting/${scjCode}/create" />
</#macro>
<#macro edit_meeting_record scjCode meeting_record>
	<@url page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${scjCode}/edit/${meeting_record.id}" />
</#macro>

<#macro delete_meeting_record meeting_record><@url page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${meeting_record.id}/delete" /></#macro>
<#macro restore_meeting_record meeting_record><@url page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${meeting_record.id}/restore" /></#macro>
<#macro purge_meeting_record meeting_record><@url page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${meeting_record.id}/purge" /></#macro>
<#macro save_meeting_approval meeting_record><@url page="/${meeting_record.relationship.relationshipType.urlPart}/meeting/${meeting_record.id}/approval" /></#macro>

<#macro relationship_search><@url page="/relationships/agents/search" /></#macro>
<#macro relationship_search_json><@url page="/relationships/agents/search.json" /></#macro>

<#macro smallgroup group><@url page="/groups/${group.id}/view" /></#macro>

<#macro create_member_note profile><@url page="/${profile.universityId}/note/add" /></#macro>
<#macro edit_member_note memberNote><@url page="/${memberNote.member.universityId}/note/${memberNote.id}/edit" /></#macro>
<#macro delete_member_note memberNote ><@url page="/${memberNote.member.universityId}/note/${memberNote.id}/delete" /></#macro>
<#macro restore_member_note memberNote ><@url page="/${memberNote.member.universityId}/note/${memberNote.id}/restore" /></#macro>
<#macro purge_member_note memberNote ><@url page="/${memberNote.member.universityId}/note/${memberNote.id}/purge" /></#macro>