<#escape x as x?html>
	<#import "*/group_components.ftl" as components />
	
	<h1 class="with-settings">
		Attendance for ${department.name}
	</h1>
	
	<div class="btn-toolbar dept-toolbar">
		<#if department.parent??>
			<a class="btn btn-medium use-tooltip" href="<@routes.departmenthome department.parent />" data-container="body" title="${department.parent.name}">
				Parent department
			</a>
		</#if>

		<#if department.children?has_content>
			<div class="btn-group">
				<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
					Subdepartments
					<span class="caret"></span>
				</a>
				<ul class="dropdown-menu pull-right">
					<#list department.children as child>
						<li><a href="<@routes.departmenthome child />">${child.name}</a></li>
					</#list>
				</ul>
			</div>
		</#if>
	</div>
	
	<@components.department_attendance department modules />
	
	<#-- List of students modal -->
	<div id="students-list-modal" class="modal fade">
	</div>
</#escape>