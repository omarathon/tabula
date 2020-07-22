<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.

TODO grab values from the Routes object in code, as that's pretty equivalent and
	we're repeating ourselves here. OR expose Routes directly.

-->

<#macro _u page context=component.context!('/exams')>
  <@url context=context page=page />
</#macro>

<#macro home><@_u page="/" /></#macro>

<#-- Shared -->
<#macro modulePermissions module><@_u context="/admin" page="/module/${module.code}/permissions"  /></#macro>
<#macro departmentDisplaySettings department><@_u context="/admin" page="/department/${department.code}/settings/display" /></#macro>
<#macro departmentNotificationSettings department><@_u context="/admin" page="/department/${department.code}/settings/notification" /></#macro>
<#macro assignMarkersSmallGroups exam><@_u context="/groups" page="/admin/marker-allocation/exam/${exam.id}" /></#macro>

<#-- Exams -->
<#macro examsHome><@_u page="/" /></#macro>
<#macro examsDepartmentHome department><@_u page="/admin/${department.code}/${academicYear.startYear?c}" /></#macro>
<#macro examsDepartmentHomeForYear department academicYear><@_u page="/admin/${department.code}/${academicYear.startYear?c}" /></#macro>
<#macro createExam module academicYear><@_u page="/admin/${module.adminDepartment.code}/${academicYear.startYear?c}/${module.code}/create" /></#macro>

<#-- Grids -->
<#macro gridsHome><@_u page="/grids" /></#macro>
<#macro gridsDepartmentHomeForYear department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}" /></#macro>
<#macro generateGrid department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate" /></#macro>
<#macro generateGridSkipImport department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/import/skip" /></#macro>
<#macro generateGridPreview department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/preview" /></#macro>
<#macro gridCheckStudent department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/preview/checkstudent" /></#macro>
<#macro generateGridOptions department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/options" /></#macro>
<#macro generateGridCoreRequired department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/corerequired" /></#macro>
<#macro generateModuleGrid department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/module/generate" /></#macro>
<#macro generateModuleGridSkipImport department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/module/generate/import/skip" /></#macro>
<#macro generateModuleGridPreview department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/module/generate/preview" /></#macro>
<#macro generateGridProgress department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/progress" /></#macro>
<#macro generateModuleGridProgress department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/module/generate/progress" /></#macro>
<#macro generateGridOvercatting department academicYear scyd><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/overcatting/${scyd.id}" /></#macro>
<#macro uploadYearMarks department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/upload"/></#macro>
<#macro manageNormalLoads department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/normalload"/></#macro>
<#macro manageWeightings department academicYear><@_u page="/grids/${department.code}/${academicYear.startYear?c}/weightings"/></#macro>
<#macro downloadGridDocument department academicYear jobInstance><@_u page="/grids/${department.code}/${academicYear.startYear?c}/generate/documents/${jobInstance.id}/download" /></#macro>

<#macro benchmarkDetails scyd yearMarksToUse="sitsIfAvailable" groupByLevel=true><@_u page="/grids/${scyd.enrolmentDepartment.code}/${scyd.academicYear.startYear?c}/${scyd.studentCourseDetails.urlSafeId}/benchmarkdetails?yearMarksToUse=${yearMarksToUse}&groupByLevel=${groupByLevel?c}" /></#macro>
