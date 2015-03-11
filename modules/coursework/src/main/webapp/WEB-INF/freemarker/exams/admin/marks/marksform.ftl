<#import "*/courses_macros.ftl" as courses_macros />
<#assign templateUrl><@routes.examMarkstemplate exam=exam /></#assign>
<#assign formUrl><@routes.examAddMarks exam /></#assign>
<#assign cancelUrl><@routes.departmentHomeWithYear module=exam.module academicYear=exam.academicYear /></#assign>
<#assign generateUrl><@routes.generateExamGradesForMarks exam=exam /></#assign>
<@courses_macros.marksForm
	assignment = exam
	templateUrl = templateUrl
	formUrl = formUrl
	commandName = "adminAddMarksCommand"
	cancelUrl = cancelUrl
	generateUrl = generateUrl
	seatOrderMap = seatOrderMap
	showAddButton = false
/>
