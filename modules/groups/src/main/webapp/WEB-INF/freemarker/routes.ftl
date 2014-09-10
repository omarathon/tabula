<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.

TODO grab values from the Routes object in code, as that's pretty equivalent and
	we're repeating ourselves here. OR expose Routes directly.

-->
<#macro _u page context='/groups'><@url context=context page=page /></#macro>

<#macro home><@_u page="/" /></#macro>
<#macro departmenthome department year=""><#compress>
	<#local p>/admin/department/${department.code}/<#if year?has_content>${year.startYear?c}/</#if></#local>
	<@_u page=p />
</#compress></#macro>
<#macro modulehome module><@_u page="/admin/module/${module.code}/" /></#macro>
<#macro depthome module><@_u page="/admin/department/${module.department.code}/#module-${module.code}" /></#macro>
<#macro moduleperms module><@_u page="/module/${module.code}/permissions" context="/admin" /></#macro>

<#macro displaysettings department><@_u page="/department/${department.code}/settings/display" context="/admin" /></#macro>
<#macro batchnotify department><@_u page="/admin/department/${department.code}/groups/release"  /></#macro>
<#macro batchopen department><@_u page="/admin/department/${department.code}/groups/selfsignup/open"  /></#macro>
<#macro batchclose department><@_u page="/admin/department/${department.code}/groups/selfsignup/close"  /></#macro>

<#macro createset module><@_u page="/admin/module/${module.code}/groups/new" /></#macro>
<#macro editset set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/edit" /></#macro>
<#macro deleteset set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/delete" /></#macro>
<#macro archiveset set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/archive" /></#macro>
<#macro allocateset set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/allocate" /></#macro>
<#macro releaseset set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/release" /></#macro>
<#macro enrolment set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/enrolment"/></#macro>
<#macro openset set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/selfsignup/open" /></#macro>
<#macro closeset set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/selfsignup/close" /></#macro>

<#macro templatespreadsheet set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/allocate/template" /></#macro>

<#macro createeditproperties set><@_u page="/admin/module/${set.module.code}/groups/new/${set.id}" /></#macro>
<#macro createsetstudents set><@_u page="/admin/module/${set.module.code}/groups/new/${set.id}/students" /></#macro>
<#macro createsetgroups set><@_u page="/admin/module/${set.module.code}/groups/new/${set.id}/groups" /></#macro>
<#macro createsetevents set><@_u page="/admin/module/${set.module.code}/groups/new/${set.id}/events" /></#macro>
<#macro createsetallocate set><@_u page="/admin/module/${set.module.code}/groups/new/${set.id}/allocate" /></#macro>
<#macro editsetproperties set><@_u page="/admin/module/${set.module.code}/groups/edit/${set.id}" /></#macro>
<#macro editsetstudents set><@_u page="/admin/module/${set.module.code}/groups/edit/${set.id}/students" /></#macro>
<#macro editsetgroups set><@_u page="/admin/module/${set.module.code}/groups/edit/${set.id}/groups" /></#macro>
<#macro editsetevents set><@_u page="/admin/module/${set.module.code}/groups/edit/${set.id}/events" /></#macro>
<#macro editsetallocate set><@_u page="/admin/module/${set.module.code}/groups/edit/${set.id}/allocate" /></#macro>

<#macro createseteventsnewevent group><@_u page="/admin/module/${group.groupSet.module.code}/groups/new/${group.groupSet.id}/events/${group.id}/new" /></#macro>
<#macro createseteventseditevent event><@_u page="/admin/module/${event.group.groupSet.module.code}/groups/new/${event.group.groupSet.id}/events/${event.group.id}/edit/${event.id}" /></#macro>
<#macro editseteventsnewevent group><@_u page="/admin/module/${group.groupSet.module.code}/groups/new/${group.groupSet.id}/events/${group.id}/new" /></#macro>
<#macro editseteventseditevent event><@_u page="/admin/module/${event.group.groupSet.module.code}/groups/new/${event.group.groupSet.id}/events/${event.group.id}/edit/${event.id}" /></#macro>

<#macro createseteventdefaults set><@_u page="/admin/module/${set.module.code}/groups/new/${set.id}/events/defaults" /></#macro>
<#macro editseteventdefaults set><@_u page="/admin/module/${set.module.code}/groups/edit/${set.id}/events/defaults" /></#macro>

<#macro register event><@_u page="/event/${event.id}/register" /></#macro>
<#macro registerForWeek event week><@_u page="/event/${event.id}/register?week=${week?c}" /></#macro>
<#macro registerPdf event><@_u page="/event/${event.id}/register.pdf" /></#macro>
<#macro addAdditionalStudent event week><@_u page="/event/${event.id}/register/additional?week=${week?c}" /></#macro>
<#macro groupAttendance group><@_u page="/group/${group.id}/attendance" /></#macro>
<#macro setAttendance set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/attendance" /></#macro>
<#macro moduleAttendance module><@_u page="/admin/module/${module.code}/attendance" /></#macro>
<#macro departmentAttendance department><@_u page="/admin/department/${department.code}/attendance" /></#macro>
<#macro viewNote student occurrence returnTo=""><@_u page="/note/${student.universityId}/${occurrence.id}/?returnTo=${returnTo}" /></#macro>
<#macro editNote student occurrence returnTo="">
	<#local returnTo><#if returnTo?has_content>?returnTo=${returnTo}</#if></#local>
	<@_u page="/note/${student.universityId}/${occurrence.id}/edit${returnTo}" />
</#macro>

<#macro signup_to_group set><@_u page="/module/${set.module.code}/groups/${set.id}/signup" /></#macro>
<#macro leave_group set><@_u page="/module/${set.module.code}/groups/${set.id}/leave" /></#macro>
<#macro photo profile><#if ((profile.universityId)!)?has_content><@_u page="/view/photo/${profile.universityId}.jpg" context="/profiles" /><#else><@_u resource="/static/images/no-photo.jpg" /></#if></#macro>
<#macro relationshipPhoto profile relationship><@_u page="/view/photo/${profile.universityId}/${relationship.relationshipType.dbValue}/${relationship.agent}.jpg" context="/profiles" /></#macro>

<#macro studentslist group><@_u page="/group/${group.id}/studentspopup" /></#macro>
<#macro unallocatedstudentslist groupset><@_u page="/${groupset.id}/unallocatedstudentspopup" /></#macro>

<#macro crossmodulegroups department><@_u page="/admin/department/${department.code}/groups/reusable"  /></#macro>
<#macro createcrossmodulegroups department><@_u page="/admin/department/${department.code}/groups/reusable/new"  /></#macro>
<#macro createcrossmodulegroupsstudents set><@_u page="/admin/department/${set.department.code}/groups/reusable/new/${set.id}/students"  /></#macro>
<#macro createcrossmodulegroupsgroups set><@_u page="/admin/department/${set.department.code}/groups/reusable/new/${set.id}/groups"  /></#macro>
<#macro createcrossmodulegroupsallocate set><@_u page="/admin/department/${set.department.code}/groups/reusable/new/${set.id}/allocate"  /></#macro>
<#macro editcrossmodulegroups set><@_u page="/admin/department/${set.department.code}/groups/reusable/edit/${set.id}"  /></#macro>
<#macro editcrossmodulegroupsstudents set><@_u page="/admin/department/${set.department.code}/groups/reusable/edit/${set.id}/students"  /></#macro>
<#macro editcrossmodulegroupsgroups set><@_u page="/admin/department/${set.department.code}/groups/reusable/edit/${set.id}/groups"  /></#macro>
<#macro editcrossmodulegroupsallocate set><@_u page="/admin/department/${set.department.code}/groups/reusable/edit/${set.id}/allocate"  /></#macro>
<#macro deletecrossmodulegroups set><@_u page="/admin/department/${set.department.code}/groups/reusable/delete/${set.id}"  /></#macro>
<#macro crossmodulegroupstemplate set><@_u page="/admin/department/${set.department.code}/groups/reusable/${set.id}/template"  /></#macro>

<#macro students_json set><@_u page="/module/${set.module.code}/groups/${set.id}/students/search.json" /></#macro>

<#macro permissions scope><@_u page="/permissions/${scope.urlCategory}/${scope.urlSlug}" context="/admin" /></#macro>