<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<#assign filterQuery = filterCommand.serializeFilter />

<#if features.attendanceMonitoringReport && can.do("MonitoringPoints.Report", department) >
	<div class="pull-right send-to-sits">
		<a href="<@routes.attendance.viewReport department academicYear.startYear?c filterQuery />" class="btn btn-primary">Upload to SITS:eVision</a>
	</div>
</#if>


<#macro deptheaderroutemacro dept>
	<@routes.attendance.viewStudents dept academicYear.startYear?c />
</#macro>
<#assign deptheaderroute = deptheaderroutemacro in routes/>
<@fmt.deptheader "View students" "in" department routes "deptheaderroute" />

<#if reports?? && monitoringPeriod??>
	<div class="alert alert-success">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Missed points for <@fmt.p reports "student" /> in the ${monitoringPeriod} monitoring period have been uploaded to SITS:eVision.
	</div>
</#if>

<#assign submitUrl><@routes.attendance.viewStudents department academicYear.startYear?c /></#assign>
<#assign filterCommand = filterCommand />
<#assign filterCommandName = "filterCommand" />
<#assign filterResultsPath = "/WEB-INF/freemarker/attendance/view/_students_results.ftl" />

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