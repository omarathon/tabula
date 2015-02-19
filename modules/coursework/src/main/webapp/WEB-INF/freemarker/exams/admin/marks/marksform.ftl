<#import "*/courses_macros.ftl" as courses_macros />
<#assign templateUrl><@routes.examMarkstemplate exam=exam /></#assign>
<#assign formUrl>${url('/exams/admin/module/${module.code}/exams/${exam.id}/marks')}</#assign>
<#assign cancelUrl><@routes.depthome module=exam.module /></#assign>
<#assign generateUrl><@routes.generateExamGradesForMarks exam=exam /></#assign>
<@courses_macros.marksForm
	assignment = exam
	templateUrl = templateUrl
	formUrl = formUrl
	commandName = "adminAddMarksCommand"
	cancelUrl = cancelUrl
	generateUrl = generateUrl
/>
