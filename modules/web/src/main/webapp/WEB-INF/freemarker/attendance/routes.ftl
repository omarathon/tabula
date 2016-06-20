<#ftl strip_text=true />
<#--
		Just a handy place to create macros for generating URLs to various places, to save time
		if we end up changing any of them.

		TODO grab values from the Routes object in code, as that's pretty equivalent and
		we're repeating ourselves here. OR expose Routes directly.

		-->

<#macro _u page context='/attendance'><@url context=context page=page /></#macro>

<#macro home academicYear="">
	<#if academicYear?has_content>
		<@_u page="/?academicYear=${academicYear}" />
	<#else>
		<@_u page="/" />
	</#if></#macro>

<#-- OLD ROUTES -->

	<#macro viewDepartment department><@_u page="/${department.code}/" /></#macro>
	<#macro viewDepartmentPoints department><@_u page="/view/${department.code}/2013/points/" /></#macro>
	<#macro viewDepartmentPointsWithAcademicYear department academicYear queryString="">
		<@_u page="/view/${department.code}/${academicYear.startYear?c}/points/?${queryString}" />
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
	<#macro recordWithAcademicYear department academicYear pointId queryString returnTo>
		<@_u page="/view/${department.code}/${academicYear.startYear?c}/${pointId}/record?returnTo=${returnTo?url}&${queryString}"/>
	</#macro>

	<#macro recordStudent department student academicYear returnTo><@_u page="/view/${department.code}/2013/students/${student.universityId}/record?returnTo=${returnTo?url}" /></#macro>
	<#macro recordStudentPoint point student returnTo><@_u page="/${point.pointSet.route.adminDepartment.code}/${point.id}/record/${student.universityId}?returnTo=${returnTo?url}"/></#macro>

	<#macro viewNote student point returnTo=""><@_u page="/note/2013/${student.universityId}/${point.id}/?returnTo=${returnTo}" /></#macro>
	<#macro editNote student point returnTo="">
		<#local returnTo><#if returnTo?has_content>?returnTo=${returnTo}</#if></#local>
		<@_u page="/note/2013/${student.universityId}/${point.id}/edit${returnTo}" />
	</#macro>

	<#macro studentMeetings point member><@_u page="/${point.pointSet.route.adminDepartment.code}/${point.id}/meetings/${member.universityId}"/></#macro>

	<#macro agentView relationshipType><@_u page="/agent/${relationshipType.urlPart}/2013"/></#macro>
	<#macro agentStudentView student relationshipType academicYear><@_u page="/agent/${relationshipType.urlPart}/2013/${student.universityId}"/></#macro>
	<#macro agentStudentRecord student relationshipType academicYear returnTo><@_u page="/agent/${relationshipType.urlPart}/2013/${student.universityId}/record?returnTo=${returnTo?url}"/></#macro>
	<#macro agentPointRecord pointId relationshipType returnTo><@_u page="/agent/${relationshipType.urlPart}/2013/point/${pointId}/record?returnTo=${returnTo?url}"/></#macro>

	<#macro createSet department academicYear><@_u page="/manage/${department.code}/2013/sets/add/${academicYear.startYear?c}"/></#macro>
	<#macro editSet pointSet><@_u page="/manage/${pointSet.route.adminDepartment.code}/2013/sets/${pointSet.id}/edit"/></#macro>

	<#-- Non-persistent -->
	<#macro addPoint department><@_u page="/manage/${department.code}/2013/sets/add/points/add" /></#macro>
	<#macro editPoint department pointIndex><@_u page="/manage/${department.code}/2013/sets/add/points/edit/${pointIndex}" /></#macro>
	<#macro deletePoint department pointIndex><@_u page="/manage/${department.code}/2013/sets/add/points/delete/${pointIndex}" /></#macro>

	<#-- Persistent -->
	<#macro createPoint pointSet><@_u page="/manage/${pointSet.route.adminDepartment.code}/2013/sets/${pointSet.id}/edit/points/add" /></#macro>
	<#macro updatePoint point><@_u page="/manage/${point.pointSet.route.adminDepartment.code}/2013/sets/${point.pointSet.id}/edit/points/${point.id}/edit" /></#macro>
	<#macro removePoint point><@_u page="/manage/${point.pointSet.route.adminDepartment.code}/2013/sets/${point.pointSet.id}/edit/points/${point.id}/delete" /></#macro>

	<#macro profile profile><@_u page="/view/${profile.universityId}" context="/profiles"/></#macro>
	<#macro attendanceProfile><@_u page="/profile" /></#macro>
	<#macro photo profile><#if ((profile.universityId)!)?has_content><@_u page="/view/photo/${profile.universityId}.jpg" context="/profiles"/><#else><@_u resource="/static/images/no-photo.jpg" /></#if></#macro>
	<#macro relationship_students relationshipType><@_u page="/${relationshipType.urlPart}/students" context="/profiles" /></#macro>

<#-- NEW ROUTES -->

<#macro agentHome><@_u page="/agent"/></#macro>
<#macro agentHomeYears relationshipType><@_u page="/agent/${relationshipType.urlPart}"/></#macro>
<#macro agentHomeForYear relationshipType academicYearString><@_u page="/agent/${relationshipType.urlPart}/${academicYearString}"/></#macro>
<#macro agentStudent relationshipType academicYearString student><@_u page="/agent/${relationshipType.urlPart}/${academicYearString}/${student.universityId}"/></#macro>
<#macro agentRecord relationshipType academicYearString student returnTo><@_u page="/agent/${relationshipType.urlPart}/${academicYearString}/${student.universityId}/record?returnTo=${returnTo}"/></#macro>
<#macro agentRecordPoints relationshipType academicYearString point returnTo><@_u page="/agent/${relationshipType.urlPart}/${academicYearString}/point/${point.id}?returnTo=${returnTo}"/></#macro>

<#macro manageHome><@_u page="/manage"/></#macro>
<#macro manageHomeYears department><@_u page="/manage/${department.code}"/></#macro>
<#macro manageHomeForYear department academicYearString><@_u page="/manage/${department.code}/${academicYearString}"/></#macro>

<#macro manageNewScheme department academicYearString><@_u page="/manage/${department.code}/${academicYearString}/new"/></#macro>
<#macro manageAddStudents scheme>
	<@_u page="/manage/${scheme.department.code}/${scheme.academicYear.startYear?c}/new/${scheme.id}/students"/>
</#macro>
<#macro manageAddStudentsAllStudents scheme>
	<@_u page="/manage/${scheme.department.code}/${scheme.academicYear.startYear?c}/new/${scheme.id}/students/all"/>
</#macro>
<#macro manageNewSchemeAddPoints scheme>
	<@_u page="/manage/${scheme.department.code}/${scheme.academicYear.startYear?c}/new/${scheme.id}/points"/>
</#macro>

<#macro manageEditScheme department academicYearString scheme><@_u page="/manage/${department.code}/${academicYearString}/${scheme.id}/edit"/></#macro>
<#macro manageEditSchemeStudents department academicYearString scheme><@_u page="/manage/${department.code}/${academicYearString}/${scheme.id}/edit/students"/></#macro>
<#macro manageEditSchemePoints department academicYearString scheme><@_u page="/manage/${department.code}/${academicYearString}/${scheme.id}/edit/points"/></#macro>
<#macro manageDeleteScheme department academicYearString scheme><@_u page="/manage/${department.code}/${academicYearString}/${scheme.id}/delete"/></#macro>

<#macro manageSelectStudents scheme>
	<@_u page="/manage/${scheme.department.code}/${scheme.academicYear.startYear?c}/${scheme.id}/students/select"/>
</#macro>

<#macro manageAddPoints department academicYearString><@_u page="/manage/${department.code}/${academicYearString}/addpoints"/></#macro>
<#macro manageAddPointsBlank department academicYearString><@_u page="/manage/${department.code}/${academicYearString}/addpoints/new"/></#macro>
<#macro manageAddPointsCopy department academicYearString><@_u page="/manage/${department.code}/${academicYearString}/addpoints/copy"/></#macro>
<#macro manageAddPointsTemplate department academicYearString><@_u page="/manage/${department.code}/${academicYearString}/addpoints/template"/></#macro>

<#macro manageEditPoints department academicYearString schemesParam="">
	<#if schemesParam?has_content><#local schemesParam = "?" + schemesParam /></#if>
	<@_u page="/manage/${department.code}/${academicYearString}/editpoints${schemesParam}"/></#macro>
<#macro manageEditPoint point filterQuery returnTo><@_u page="/manage/${point.scheme.department.code}/${point.scheme.academicYear.startYear?c}/editpoints/${point.id}/edit?returnTo=${returnTo}&${filterQuery}"/></#macro>
<#macro manageDeletePoint point filterQuery returnTo><@_u page="/manage/${point.scheme.department.code}/${point.scheme.academicYear.startYear?c}/editpoints/${point.id}/delete?returnTo=${returnTo}&${filterQuery}"/></#macro>

<#macro noteView academicYearString student point returnTo=""><@_u page="/note/${academicYearString}/${student.universityId}/${point.id}/?returnTo=${returnTo}" /></#macro>
<#macro groupsNoteView student occurrence returnTo=""><@_u context="/groups" page="/note/${student.universityId}/${occurrence.id}/?returnTo=${returnTo}" /></#macro>
<#macro noteEdit academicYearString student point><@_u page="/note/${academicYearString}/${student.universityId}/${point.id}/edit"/></#macro>
<#macro bulkNoteEdit academicYearString point students><@_u page="/note/${academicYearString}/bulk/${point.id}/edit"/><#list students as student><#if student_index ==0>?<#else>&</#if>students=${student.universityId}</#list></#macro>

<#macro profileHome><@_u page="/profile"/></#macro>
<#macro profileYears student><@_u page="/profile/${student.universityId}"/></#macro>
<#macro profileForYear student academicYearString><@_u page="/profile/${student.universityId}/${academicYearString}"/></#macro>
<#macro profileRecord student academicYearString returnTo="">
	<#local returnTo><#if returnTo?has_content>?returnTo=${returnTo}</#if></#local>
	<@_u page="/profile/${student.universityId}/${academicYearString}/record${returnTo}"/>
</#macro>
<#macro profileMeetings student academicYearString point><@_u page="/profile/${student.universityId}/${academicYearString}/${point.id}/meetings"/></#macro>
<#macro profileGroups student academicYearString point><@_u page="/profile/${student.universityId}/${academicYearString}/${point.id}/groups"/></#macro>

<#macro viewHome><@_u page="/view"/></#macro>
<#macro viewHomeYears department><@_u page="/view/${department.code}"/></#macro>
<#macro viewHomeForYear department academicYearString><@_u page="/view/${department.code}/${academicYearString}"/></#macro>

<#macro viewReport department academicYearString queryString><@_u page="/view/${department.code}/${academicYearString}/report?${queryString}"/></#macro>
<#macro viewReportConfirm department academicYearString><@_u page="/view/${department.code}/${academicYearString}/report/confirm"/></#macro>

<#macro viewStudents department academicYearString queryString="" page="" sortOrder="">
	<#local args = [] />
	<#if queryString?has_content>
		<#local args = args + [queryString] />
	</#if>
	<#if page?has_content>
		<#local args = args + ["page=" + page ] />
	</#if>
	<#if sortOrder?has_content>
		<#local args = args + ["sortOrder=" + sortOrder ] />
	</#if>
	<#local query><#if args?has_content>?</#if><#list args as arg>${arg}<#if arg_has_next>&</#if></#list></#local>
	<@_u page="/view/${department.code}/${academicYearString}/students${query}"/>
</#macro>
<#macro viewSingleStudent department academicYearString student><@_u page="/view/${department.code}/${academicYearString}/students/${student.universityId}" /></#macro>
<#macro viewRecordStudent department academicYearString student returnTo="">
	<#local returnTo><#if returnTo?has_content>?returnTo=${returnTo}</#if></#local>
	<@_u page="/view/${department.code}/${academicYearString}/students/${student.universityId}/record${returnTo}"/>
</#macro>

<#macro viewPoints department academicYearString filterQuery="">
	<#local filterQuery><#if filterQuery?has_content>?${filterQuery}&hasBeenFiltered=true</#if></#local>
	<@_u page="/view/${department.code}/${academicYearString}/points${filterQuery}"/>
</#macro>
<#macro viewRecordPoints department academicYearString point queryString returnTo><@_u page="/view/${department.code}/${academicYearString}/points/${point.id}/record?returnTo=${returnTo?url}&hasBeenFiltered=true&${queryString}"/></#macro>

<#macro viewAgentsHome department academicYearString><@_u page="/view/${department.code}/${academicYearString}/agents"/></#macro>
<#macro viewAgents department academicYearString relationshipType><@_u page="/view/${department.code}/${academicYearString}/agents/${relationshipType.urlPart}"/></#macro>
<#macro viewAgent department academicYearString relationshipType agent><@_u page="/view/${department.code}/${academicYearString}/agents/${relationshipType.urlPart}/${agent.universityId}"/></#macro>
