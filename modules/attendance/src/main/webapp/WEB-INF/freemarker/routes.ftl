<#ftl strip_text=true />
<#--
		Just a handy place to create macros for generating URLs to various places, to save time
		if we end up changing any of them.

		TODO grab values from the Routes object in code, as that's pretty equivalent and
		we're repeating ourselves here. OR expose Routes directly.

		-->
	<#macro home><@url page="/" /></#macro>
	<#macro viewDepartment department><@url page="/${department.code}/" /></#macro>
	<#macro viewDepartmentPoints department><@url page="/view/${department.code}/points/" /></#macro>
	<#macro viewDepartmentPointsWithAcademicYear department academicYear queryString="">
		<#if queryString?has_content>
			<#local queryString = "&" + queryString />
		</#if>
		<@url page="/view/${department.code}/points/?academicYear=${academicYear.toString}${queryString}" />
	</#macro>
	<#macro viewDepartmentStudents department><@url page="/view/${department.code}/students/" /></#macro>
	<#macro viewDepartmentStudentsWithAcademicYear department academicYear queryString="">
		<#if queryString?has_content>
			<#local queryString = "&" + queryString />
		</#if>
		<@url page="/view/${department.code}/students/?academicYear=${academicYear.toString}${queryString}" />
	</#macro>
	<#macro viewStudent department student academicYear><@url page="/view/${department.code}/students/${student.universityId}?academicYear=${academicYear.toString}" /></#macro>
	<#macro viewDepartmentAgents department relationshipType><@url page="/view/${department.code}/agents/${relationshipType.urlPart}" /></#macro>
	<#macro viewDepartmentAgentsStudents department relationshipType agent><@url page="/view/${department.code}/agents/${relationshipType.urlPart}/${agent.universityId}" /></#macro>

	<#macro report department academicYear queryString><@url page="/report/${department.code}?academicYear=${academicYear.toString}&${queryString}"/></#macro>
	<#macro reportConfirm department><@url page="/report/${department.code}/confirm"/></#macro>

	<#macro manageDepartment department><@url page="/manage/${department.code}/" /></#macro>
	
	<#macro record department pointId queryString returnTo><@url page="/view/${department.code}/${pointId}/record?returnTo=${returnTo?url}&${queryString}"/></#macro>
	<#macro recordStudent department student academicYear returnTo><@url page="/view/${department.code}/students/${student.universityId}/record?academicYear=${academicYear.toString}&returnTo=${returnTo?url}" /></#macro>
	<#macro recordStudentPoint point student returnTo><@url page="/${point.pointSet.route.department.code}/${point.id}/record/${student.universityId}?returnTo=${returnTo?url}"/></#macro>

	<#macro studentMeetings point member><@url page="/${point.pointSet.route.department.code}/${point.id}/meetings/${member.universityId}"/></#macro>

	<#macro agentView relationshipType><@url page="/agent/${relationshipType.urlPart}"/></#macro>
	<#macro agentStudentView student relationshipType academicYear><@url page="/agent/${relationshipType.urlPart}/${student.universityId}/?academicYear=${academicYear.toString}"/></#macro>
	<#macro agentStudentRecord student relationshipType academicYear returnTo><@url page="/agent/${relationshipType.urlPart}/${student.universityId}/record?academicYear=${academicYear.toString}&returnTo=${returnTo?url}"/></#macro>
	
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
	<#macro photo profile><#if ((profile.universityId)!)?has_content><@url page="/view/photo/${profile.universityId}.jpg" context="/profiles"/><#else><@url resource="/static/images/no-photo.jpg" /></#if></#macro>
	<#macro relationship_students relationshipType><@url page="/${relationshipType.urlPart}/students" context="/profiles" /></#macro>
