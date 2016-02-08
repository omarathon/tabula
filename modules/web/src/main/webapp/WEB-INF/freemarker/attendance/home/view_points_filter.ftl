<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<@fmt.deptheader "View monitoring points" "for" command.department routes "viewDepartmentPoints" />

<#if updatedMonitoringPoint??>
	<div class="alert alert-success">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Attendance recorded for '${updatedMonitoringPoint.name}'
	</div>
</#if>

<#assign submitUrl><@routes.attendance.viewDepartmentPoints command.department /></#assign>

<#assign filterCommand = command />
<#assign filterCommandName = "command" />
<#assign filterResultsPath = "/WEB-INF/freemarker/home/view_points_results.ftl" />
<#include "/WEB-INF/freemarker/filter_bar.ftl" />
</#escape>