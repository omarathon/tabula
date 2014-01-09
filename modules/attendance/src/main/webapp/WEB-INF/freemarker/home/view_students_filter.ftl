<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<h1>View students</h1>
<h4><span class="muted">in</span> ${command.department.name}</h4>

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

<#if reports?? && monitoringPeriod??>
	<div class="alert alert-success">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Missed points for <@fmt.p reports "student" /> in the ${monitoringPeriod} monitoring period have been recorded in SITS:eVision.
	</div>
</#if>

<#assign filterQuery = command.serializeFilter />
<#if features.attendanceMonitoringReport && can.do("MonitoringPoints.Report", command.department) >
	<div class="pull-right">
		<a href="<@routes.report command.department command.academicYear filterQuery />" class="btn btn-primary">Record in SITS:eVision</a>
	</div>
</#if>

<#assign submitUrl><@routes.viewDepartmentStudents command.department /></#assign>
<@attendance_macros.academicYearSwitcher submitUrl command.academicYear command.thisAcademicYear />

<#assign filterCommand = command />
<#assign filterCommandName = "command" />
<#assign filterResultsPath = "/WEB-INF/freemarker/home/view_students_results.ftl" />
<#include "/WEB-INF/freemarker/filter_bar.ftl" />

<script type="text/javascript">
	jQuery(function($) {
		$(document).on("tabula.filterResultsChanged", function() {
			$('.scrollable-points-table').find('table').each(function() {
				var $this = $(this);
				if (Math.floor($this.width()) > $this.parent().width()) {
					$this.wrap($('<div><div class="sb-wide-table-wrapper"></div></div>'));
					Attendance.scrollablePointsTableSetup();
				}
			});
		});
	});
</script>
</#escape>