<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<h1>View students in ${command.department.name}</h1>

<div class="btn-toolbar dept-toolbar">
	<#if command.department.parent??>
		<a class="btn btn-medium use-tooltip" href="<@routes.viewDepartmentStudents command.department.parent />" data-container="body" title="${command.department.parent.name}">
			Parent department
		</a>
	</#if>

	<#if command.department.children?has_content>
		<div class="btn-group">
			<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
				Subdepartments
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu pull-right">
				<#list command.department.children as child>
					<li><a href="<@routes.viewDepartmentStudents child />">${child.name}</a></li>
				</#list>
			</ul>
		</div>
	</#if>
</div>

<#if updatedStudent??>
	<div class="alert alert-success">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Attendance recorded for <@fmt.profile_name updatedStudent />
	</div>
</#if>

<#assign thisPath><@routes.viewDepartmentStudents command.department /></#assign>
<@attendance_macros.academicYearSwitcher thisPath command.academicYear command.thisAcademicYear />
<#assign filter_results_path="view_students_results.ftl" />
<#include "_view_filter.ftl" />
</#escape>