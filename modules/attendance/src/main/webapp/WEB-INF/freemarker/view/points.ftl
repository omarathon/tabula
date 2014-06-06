<#escape x as x?html>
	<#import "../attendance_macros.ftl" as attendance_macros />

	<@fmt.deptheader "View monitoring points" "for" filterCommand.department routes "viewDepartmentPoints" />

	<#if updatedMonitoringPoint??>
	<div class="alert alert-success">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Attendance recorded for '${updatedMonitoringPoint.name}'
	</div>
	</#if>

	<#assign submitUrl><@routes.viewDepartmentPointsWithAcademicYear filterCommand.department filterCommand.academicYear filterQuery/></#assign>

	<#assign filterCommand = filterCommand />
	<#assign filterCommandName = "filterCommand" />
	<#assign filterResultsPath = "/WEB-INF/freemarker/view/_points_results.ftl" />
	<#include "/WEB-INF/freemarker/filter_bar.ftl" />
</#escape>