<#escape x as x?html>

<#function route_function dept>
	<#local result><@routes.exams.gridsDepartmentHomeForYear dept academicYear /></#local>
	<#return result />
</#function>

<@fmt.id7_deptheader title="Manage exam board grids for ${academicYear.toString}" route_function=route_function preposition="in" />

<#if can.do("Department.ExamGrids", department)>
	<a href="<@routes.exams.generateGrid department academicYear />" class="btn btn-primary btn-lg">Generate new grid</a>
<#else>
	<div class="alert alert-danger">
		You do not have permission to generate exam board grids for ${department.name}.
	</div>
</#if>

</#escape>