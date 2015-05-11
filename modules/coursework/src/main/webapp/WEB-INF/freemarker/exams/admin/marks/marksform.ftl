<#import "*/courses_macros.ftl" as courses_macros />

<#if marker??>
	<#assign formUrl><@routes.examMarkerAddMarks exam marker/></#assign>
	<#assign templateUrl><@routes.examMarkerMarksTemplate exam marker/></#assign>
	<#assign cancelUrl><@routes.home /></#assign>
<#else>
	<#assign formUrl><@routes.examAddMarks exam /></#assign>
	<#assign templateUrl><@routes.examMarkstemplate exam=exam /></#assign>
	<#assign cancelUrl><@routes.departmentHomeWithYear module=exam.module academicYear=exam.academicYear /></#assign>
</#if>
<#assign generateUrl><@routes.generateExamGradesForMarks exam=exam /></#assign>
<@courses_macros.marksForm
	assignment = exam
	templateUrl = templateUrl
	formUrl = formUrl
	commandName = "adminAddMarksCommand"
	cancelUrl = cancelUrl
	generateUrl = generateUrl
	seatNumberMap = seatNumberMap
	showAddButton = false
/>
