<#ftl strip_text=true />
<#--
		Just a handy place to create macros for generating URLs to various places, to save time
		if we end up changing any of them.

		TODO grab values from the Routes object in code, as that's pretty equivalent and
		we're repeating ourselves here. OR expose Routes directly.

		-->
	<#macro home><@url page="/" /></#macro>
	<#macro viewDepartment department><@url page="/${department.code}/" /></#macro>
	<#macro viewDepartmentSpecific department academicYear route pointSet><@url page="/${department.code}/?academicYear=${academicYear.toString}&route=${route.code}&set=${pointSet.id}" /></#macro>
	<#macro manageDepartment department><@url page="/manage/${department.code}/" /></#macro>
	
	<#macro record point returnTo><@url page="/${point.pointSet.route.department.code}/${point.id}/record?returnTo=${returnTo?url}"/></#macro>
	<#macro recordStudent point student returnTo><@url page="/${point.pointSet.route.department.code}/${point.id}/record?returnTo=${returnTo?url}"/>#student-${student.universityId}</#macro>
	
	<#macro createSet department academicYear><@url page="/manage/${department.code}/sets/add/${academicYear.startYear?c}"/></#macro>
	<#macro editSet pointSet><@url page="/manage/${pointSet.route.department.code}/sets/${pointSet.id}/edit"/></#macro>
	
	<#-- Non-persistent -->
	<#macro addPoint department><@url page="/manage/${department.code}/sets/add/points/add" /></#macro>
	<#macro editPoint department pointIndex><@url page="/manage/${department.code}/sets/add/points/edit/${pointIndex}" /></#macro>
	<#macro deletePoint department pointIndex><@url page="/manage/${department.code}/sets/add/points/delete/${pointIndex}" /></#macro>
	
	<#-- Persistent -->
	<#macro createPoint pointSet><@url page="/manage/${pointSet.route.department.code}/sets/${pointSet.id}/edit/points/add" /></#macro>
	<#macro updatePoint point><@url page="/manage/${point.pointSet.route.department.code}/sets/${point.pointSet.id}/edit/points/${point.id}/edit" /></#macro>
	<#macro removePoint point><@url page="/manage/${point.pointSet.route.department.code}/sets/${point.pointSet.id}/edit/points/${point.id}/delete" /></#macro>
	
	<#macro profile profile><@url page="/view/${profile.universityId}" context="/profiles"/></#macro>
	<#macro attendanceProfile><@url page="/profile" /></#macro>