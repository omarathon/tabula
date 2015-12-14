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
<#macro addMarks exam><@_u page="/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/marks" /></#macro>
<#macro markerAddMarks exam marker><@_u page="/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/marker/${marker.warwickId}/marks" /></#macro>
<#macro generateGradesForMarks exam><@_u page="/admin/module/${exam.module.code}/exams/${exam.id}/generate-grade"/></#macro>
<#macro markstemplate exam><@_u page="/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/marks-template" /></#macro>
<#macro markerMarksTemplate exam marker><@_u page="/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/marker/${marker.warwickId}/marks-template" /></#macro>
<#macro createExam module academicYear><@_u page="/admin/module/${module.code}/${academicYear.startYear?c}/exams/new" /></#macro>
<#macro editExam exam><@_u page="/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/edit" /></#macro>
<#macro moduleHomeWithYear module academicYear><@_u page="/admin/module/${module.code}/${academicYear.startYear?c}" /></#macro>
<#macro departmentHomeWithYear department academicYear><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}" /></#macro>
<#macro viewExam exam><@_u page="/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}" /></#macro>
<#macro uploadToSits exam><@_u page="/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/upload-to-sits"/></#macro>
<#macro feedbackAdjustment exam><@_u page="/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/feedback/adjustments"/></#macro>
<#macro feedbackAdjustmentForm exam studentid><@_u page="/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/feedback/adjustments/${studentid}"/></#macro>
<#macro bulkAdjustment exam><@_u page="/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/feedback/bulk-adjustment"/></#macro>
<#macro bulkAdjustmentTemplate exam><@_u page="/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/feedback/bulk-adjustment/template"/></#macro>
<#macro markingWorkflowList department><@_u page="/admin/department/${department.code}/markingworkflows" /></#macro>
<#macro markingWorkflowAdd department><@_u page="/admin/department/${department.code}/markingworkflows/add" /></#macro>
<#macro markingWorkflowEdit department markingWorkflow><@_u page="/admin/department/${department.code}/markingworkflows/edit/${markingWorkflow.id}" /></#macro>
<#macro markingWorkflowDelete department markingWorkflow><@_u page="/admin/department/${department.code}/markingworkflows/delete/${markingWorkflow.id}" /></#macro>
<#macro markingWorkflowReplace department markingWorkflow><@_u page="/admin/department/${department.code}/markingworkflows/edit/${markingWorkflow.id}/replace" /></#macro>
<#macro assignMarkers exam><@_u page="/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/assign-markers" /></#macro>
<#macro assignMarkersTemplate exam><@_u page="/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/assign-markers/template" /></#macro>
<#macro releaseForMarking exam><@_u page="/admin/module/${exam.module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/release-for-marking" /></#macro>
<#macro assignMarkersSmallGroups exam><@_u context="/groups" page="/admin/marker-allocation/exam/${exam.id}" /></#macro>
<#macro exportExcel module exam><@_u page="/admin/module/${module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/export.xlsx" /></#macro>
<#macro exportCSV module exam><@_u page="/admin/module/${module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/export.csv" /></#macro>
<#macro exportXML module exam><@_u page="/admin/module/${module.code}/${exam.academicYear.startYear?c}/exams/${exam.id}/export.xml" /></#macro>

<#macro modulePermissions module><@_u page="/module/${module.code}/permissions" context="/admin" /></#macro>
<#macro departmentDisplaySettings department><@_u page="/department/${department.code}/settings/display" context="/admin" /></#macro>
<#macro departmentNotificationSettings department><@_u page="/department/${department.code}/settings/notification" context="/admin" /></#macro>