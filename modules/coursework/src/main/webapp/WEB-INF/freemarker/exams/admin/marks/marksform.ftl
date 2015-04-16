<#import "*/courses_macros.ftl" as courses_macros />
<#assign templateUrl><@routes.examMarkstemplate exam=exam /></#assign>

<#if marker??>
	<#assign formUrl><@routes.examMarkerAddMarks exam marker/></#assign>
<#else>
	<#assign formUrl><@routes.examAddMarks exam /></#assign>
</#if>
<#assign cancelUrl><@routes.departmentHomeWithYear module=exam.module academicYear=exam.academicYear /></#assign>
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
