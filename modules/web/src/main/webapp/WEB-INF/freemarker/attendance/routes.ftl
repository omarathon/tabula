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
	</#if>
</#macro>

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

<#macro profile profile><@_u page="/view/${profile.universityId}" context="/profiles"/></#macro>
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
