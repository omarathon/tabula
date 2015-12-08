<#escape x as x?html>
	<#import "*/group_components.ftl" as components />
	<div id="profile-modal" class="modal fade profile-subset"></div>

	<div class="btn-toolbar dept-toolbar">
		<div class="btn-group dept-settings">
			<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
				<i class="icon-calendar"></i>
				${adminCommand.academicYear.label}
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu pull-right">
				<#list academicYears as year>
					<li>
						<a href="<@routes.groups.departmentAttendance department year />">
							<#if year.startYear == adminCommand.academicYear.startYear>
								<strong>${year.label}</strong>
							<#else>
								${year.label}
							</#if>
						</a>
					</li>
				</#list>
			</ul>
		</div>
	</div>

	<#macro deptheaderroutemacro dept>
		<@routes.groups.departmentAttendance dept adminCommand.academicYear />
	</#macro>
	<#assign deptheaderroute = deptheaderroutemacro in routes.groups />
	<@fmt.deptheader "Attendance" "for" department routes.groups "deptheaderroute" "with-settings" />

	<#if !modules?has_content>
		<p class="alert alert-info empty-hint"><i class="icon-lightbulb"></i> This department doesn't have any groups set up.</p>
	<#else>
		<@components.department_attendance department modules adminCommand.academicYear />

		<#-- List of students modal -->
		<div id="students-list-modal" class="modal fade"></div>
	</#if>
</#escape>