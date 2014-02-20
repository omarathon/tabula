<#escape x as x?html>
	<#import "*/group_components.ftl" as components />
	<div id="profile-modal" class="modal fade profile-subset"></div>

	<@fmt.deptheader "Attendance" "for" department routes "departmentAttendance" "with-settings" />

	<#if !modules?has_content>
		<p class="alert alert-info empty-hint"><i class="icon-lightbulb"></i> This department doesn't have any groups set up.</p>
	<#else>
		<@components.department_attendance department modules />

		<#-- List of students modal -->
		<div id="students-list-modal" class="modal fade">
		</div>
	</#if>
</#escape>