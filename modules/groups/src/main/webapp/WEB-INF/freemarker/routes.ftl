<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.

TODO grab values from the Routes object in code, as that's pretty equivalent and
	we're repeating ourselves here. OR expose Routes directly.

-->
<#macro home><@url page="/" /></#macro>
<#macro departmenthome department><@url page="/admin/department/${department.code}/" /></#macro>
<#macro depthome module><@url page="/admin/department/${module.department.code}/#module-${module.code}" /></#macro>
<#macro moduleperms module><@url page="/module/${module.code}/permissions" context="/admin" /></#macro>

<#macro displaysettings department><@url page="/department/${department.code}/settings/display" context="/admin" /></#macro>
<#macro batchnotify department><@url page="/admin/department/${department.code}/groups/release"  /></#macro>
<#macro batchopen department><@url page="/admin/department/${department.code}/groups/selfsignup/open"  /></#macro>
<#macro batchclose department><@url page="/admin/department/${department.code}/groups/selfsignup/close"  /></#macro>

<#macro createset module><@url page="/admin/module/${module.code}/groups/new" /></#macro>
<#macro editset set><@url page="/admin/module/${set.module.code}/groups/${set.id}/edit" /></#macro>
<#macro deleteset set><@url page="/admin/module/${set.module.code}/groups/${set.id}/delete" /></#macro>
<#macro archiveset set><@url page="/admin/module/${set.module.code}/groups/${set.id}/archive" /></#macro>
<#macro allocateset set><@url page="/admin/module/${set.module.code}/groups/${set.id}/allocate" /></#macro>
<#macro releaseset set><@url page="/admin/module/${set.module.code}/groups/${set.id}/release" /></#macro>
<#macro enrolment module><@url page="/admin/module/${module.code}/groups/enrolment"/></#macro>
<#macro openset set><@url page="/admin/module/${set.module.code}/groups/${set.id}/selfsignup/open" /></#macro>
<#macro closeset set><@url page="/admin/module/${set.module.code}/groups/${set.id}/selfsignup/close" /></#macro>

<#macro register event><@url page="/event/${event.id}/register" /></#macro>
<#macro registerForWeek event week><@url page="/event/${event.id}/register?week=${week?c}" /></#macro>
<#macro groupAttendance group><@url page="/group/${group.id}/attendance" /></#macro>
<#macro setAttendance set><@url page="/admin/module/${set.module.code}/groups/${set.id}/attendance" /></#macro>
<#macro moduleAttendance module><@url page="/admin/module/${module.code}/attendance" /></#macro>
<#macro departmentAttendance department><@url page="/admin/department/${department.code}/attendance" /></#macro>

<#macro signup_to_group set><@url page="/module/${set.module.code}/groups/${set.id}/signup" /></#macro>
<#macro leave_group set><@url page="/module/${set.module.code}/groups/${set.id}/leave" /></#macro>
<#macro photo profile><#if ((profile.universityId)!)?has_content><@url page="/view/photo/${profile.universityId}.jpg" context="/profiles" /><#else><@url resource="/static/images/no-photo.jpg" /></#if></#macro>
<#macro relationshipPhoto profile relationship><@url page="/view/photo/${profile.universityId}/${relationship.relationshipType.dbValue}/${relationship.agent}.jpg" context="/profiles" /></#macro>

<#macro studentslist group><@url page="/group/${group.id}/studentspopup" /></#macro>
<#macro unallocatedstudentslist groupset><@url page="/${groupset.id}/unallocatedstudentspopup" /></#macro>