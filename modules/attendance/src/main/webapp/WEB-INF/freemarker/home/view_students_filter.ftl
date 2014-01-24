<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<@fmt.deptheader "View students" "in" command.department routes "viewDepartmentStudents" />

<#if updatedStudent??>
	<div class="alert alert-success">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Attendance recorded for <@fmt.profile_name updatedStudent />
	</div>
</#if>

<#if reports?? && monitoringPeriod??>
	<div class="alert alert-success">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Missed points for <@fmt.p reports "student" /> in the ${monitoringPeriod} monitoring period have been uploaded to SITS:eVision.
	</div>
</#if>

<#assign filterQuery = command.serializeFilter />
<#if features.attendanceMonitoringReport && can.do("MonitoringPoints.Report", command.department) >
	<div class="pull-right send-to-sits">
		<a href="<@routes.report command.department command.academicYear filterQuery />" class="btn btn-primary">Upload to SITS:eVision</a>
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
		$('#command input').on('change', function(e) {
			$('.send-to-sits a').addClass('disabled');
		});

		$(document).on("tabula.filterResultsChanged", function() {
			var sitsUrl = $('div.studentResults').data('sits-url');
			$('.send-to-sits a').attr('href', sitsUrl);
			$('.send-to-sits a').removeClass('disabled');

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