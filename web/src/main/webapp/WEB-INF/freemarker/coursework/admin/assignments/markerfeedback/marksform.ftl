<#import "*/courses_macros.ftl" as courses_macros />
<#assign templateUrl><@routes.coursework.markermarkstemplate assignment=assignment marker=marker/></#assign>
<#assign formUrl><@routes.coursework.markeraddmarks assignment marker /></#assign>
<#assign cancelUrl><@routes.coursework.listmarkersubmissions assignment marker/></#assign>
<#assign generateUrl><@routes.coursework.generateGradesForMarks assignment=assignment /></#assign>
<@courses_macros.marksForm
	assignment = assignment
	templateUrl = templateUrl
	formUrl = formUrl
	commandName = "markerAddMarksCommand"
	cancelUrl = cancelUrl
	generateUrl = generateUrl
/>