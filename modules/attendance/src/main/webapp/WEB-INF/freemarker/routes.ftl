<#ftl strip_text=true />
<#--
		Just a handy place to create macros for generating URLs to various places, to save time
		if we end up changing any of them.

		TODO grab values from the Routes object in code, as that's pretty equivalent and
		we're repeating ourselves here. OR expose Routes directly.

		-->

<#macro _u page context='/attendance'><@url context=context page=page /></#macro>

	<#macro home><@_u page="/" /></#macro>
	<#macro viewDepartment department><@_u page="/${department.code}/" /></#macro>
	<#macro viewDepartmentPoints department><@_u page="/view/${department.code}/2013/points/" /></#macro>
	<#macro viewDepartmentPointsWithAcademicYear department academicYear queryString="">
		<@_u page="/view/${department.code}/2013/points/?${queryString}" />
	</#macro>
	<#macro viewDepartmentStudents department><@_u page="/view/${department.code}/2013/students/" /></#macro>
	<#macro viewDepartmentStudentsWithAcademicYear department academicYear queryString="" page="">
		<#if page?has_content>
			<#local page = "&page=" + page />
		</#if>
		<@_u page="/view/${department.code}/2013/students/?${queryString}${page}" />
	</#macro>
	<#macro viewStudent department student academicYear><@_u page="/view/${department.code}/2013/students/${student.universityId}" /></#macro>
	<#macro viewDepartmentAgents department relationshipType><@_u page="/view/${department.code}/2013/agents/${relationshipType.urlPart}" /></#macro>
	<#macro viewDepartmentAgentsStudents department relationshipType agent><@_u page="/view/${department.code}/2013/agents/${relationshipType.urlPart}/${agent.universityId}" /></#macro>

	<#macro report department academicYear queryString><@_u page="/report/${department.code}?${queryString}"/></#macro>
	<#macro reportConfirm department><@_u page="/report/${department.code}/confirm"/></#macro>

	<#macro manageDepartment department><@_u page="/manage/${department.code}/2013" /></#macro>
	
	<#macro record department pointId queryString returnTo><@_u page="/view/${department.code}/2013/${pointId}/record?returnTo=${returnTo?url}&${queryString}"/></#macro>
	<#macro recordStudent department student academicYear returnTo><@_u page="/view/${department.code}/2013/students/${student.universityId}/record?returnTo=${returnTo?url}" /></#macro>
	<#macro recordStudentPoint point student returnTo><@_u page="/${point.pointSet.route.department.code}/${point.id}/record/${student.universityId}?returnTo=${returnTo?url}"/></#macro>

	<#macro viewNote student point returnTo=""><@_u page="/note/2013/${student.universityId}/${point.id}/?returnTo=${returnTo}" /></#macro>
	<#macro editNote student point returnTo="">
		<#local returnTo><#if returnTo?has_content>?returnTo=${returnTo}</#if></#local>
		<@_u page="/note/2013/${student.universityId}/${point.id}/edit${returnTo}" />
	</#macro>

	<#macro studentMeetings point member><@_u page="/${point.pointSet.route.department.code}/${point.id}/meetings/${member.universityId}"/></#macro>

	<#macro agentView relationshipType><@_u page="/agent/${relationshipType.urlPart}/2013"/></#macro>
	<#macro agentStudentView student relationshipType academicYear><@_u page="/agent/${relationshipType.urlPart}/2013/${student.universityId}"/></#macro>
	<#macro agentStudentRecord student relationshipType academicYear returnTo><@_u page="/agent/${relationshipType.urlPart}/2013/${student.universityId}/record?returnTo=${returnTo?url}"/></#macro>
	<#macro agentPointRecord pointId relationshipType returnTo><@_u page="/agent/${relationshipType.urlPart}/2013/point/${pointId}/record?returnTo=${returnTo?url}"/></#macro>
	
	<#macro createSet department academicYear><@_u page="/manage/${department.code}/2013/sets/add/${academicYear.startYear?c}"/></#macro>
	<#macro editSet pointSet><@_u page="/manage/${pointSet.route.department.code}/2013/sets/${pointSet.id}/edit"/></#macro>
	
	<#-- Non-persistent -->
	<#macro addPoint department><@_u page="/manage/${department.code}/2013/sets/add/points/add" /></#macro>
	<#macro editPoint department pointIndex><@_u page="/manage/${department.code}/2013/sets/add/points/edit/${pointIndex}" /></#macro>
	<#macro deletePoint department pointIndex><@_u page="/manage/${department.code}/2013/sets/add/points/delete/${pointIndex}" /></#macro>
	
	<#-- Persistent -->
	<#macro createPoint pointSet><@_u page="/manage/${pointSet.route.department.code}/2013/sets/${pointSet.id}/edit/points/add" /></#macro>
	<#macro updatePoint point><@_u page="/manage/${point.pointSet.route.department.code}/2013/sets/${point.pointSet.id}/edit/points/${point.id}/edit" /></#macro>
	<#macro removePoint point><@_u page="/manage/${point.pointSet.route.department.code}/2013/sets/${point.pointSet.id}/edit/points/${point.id}/delete" /></#macro>
	
	<#macro profile profile><@_u page="/view/${profile.universityId}" context="/profiles"/></#macro>
	<#macro attendanceProfile><@_u page="/profile" /></#macro>
	<#macro photo profile><#if ((profile.universityId)!)?has_content><@_u page="/view/photo/${profile.universityId}.jpg" context="/profiles"/><#else><@_u resource="/static/images/no-photo.jpg" /></#if></#macro>
	<#macro relationship_students relationshipType><@_u page="/${relationshipType.urlPart}/students" context="/profiles" /></#macro>
