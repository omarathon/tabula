<#escape x as x?html>
<#import "../reports_macros.ftl" as reports_macros />

<h1>Unrecorded monitoring points</h1>

<#assign reportUrl><@routes.reports.unrecordedAttendance department academicYear /></#assign>
<@reports_macros.reportLoader reportUrl>
	<ul class="dropdown-menu">

		<li>
			<a href="#" data-href="<@routes.reports.unrecordedAttendanceDownloadCsv department academicYear />">
				<i class="icon-table fa fa-table"></i> CSV
			</a>
		</li>
		<li>
			<a href="#" data-href="<@routes.reports.unrecordedAttendanceDownloadXlsx department academicYear />">
				<i class="icon-list-alt fa fa-list-alt"></i> Excel
			</a>
		</li>
		<li>
			<a href="#" data-href="<@routes.reports.unrecordedAttendanceDownloadXml department academicYear />">
				<i class="icon-code fa fa-code"></i> XML
			</a>
		</li>
	</ul>
</@reports_macros.reportLoader>
<@reports_macros.attendanceMonitoringReportScript />

</#escape>