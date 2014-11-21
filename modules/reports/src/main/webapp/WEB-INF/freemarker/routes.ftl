<#ftl strip_text=true />
<#--
		Just a handy place to create macros for generating URLs to various places, to save time
		if we end up changing any of them.

		TODO grab values from the Routes object in code, as that's pretty equivalent and
		we're repeating ourselves here. OR expose Routes directly.

		-->

<#macro _u page context='/reports'><@url context=context page=page /></#macro>

<#macro home><@_u page="/" /></#macro>
<#macro department department><@_u page="/${department.code}" /></#macro>
<#macro departmentWithYear department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}" /></#macro>

<#macro profile profile><@_u page="/view/${profile.universityId}" context="/profiles"/></#macro>

<#macro allAttendance department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/all" /></#macro>
<#macro allAttendanceShow department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/all/show" /></#macro>
<#macro allAttendanceDownloadCsv department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/all/download.csv" /></#macro>
<#macro allAttendanceDownloadXlsx department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/all/download.xlsx" /></#macro>
<#macro allAttendanceDownloadXml department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/attendance/all/download.xml" /></#macro>

<#macro allSmallGroups department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/all" /></#macro>
<#macro allSmallGroupsShow department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/all/show" /></#macro>
<#macro allSmallGroupsDownloadCsv department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/all/download.csv" /></#macro>
<#macro allSmallGroupsDownloadXlsx department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/all/download.xlsx" /></#macro>
<#macro allSmallGroupsDownloadXml department academicYear><@_u page="/${department.code}/${academicYear.startYear?c}/groups/all/download.xml" /></#macro>