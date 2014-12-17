<#escape x as x?html>
<#import "../reports_macros.ftl" as reports_macros />

<h1>Missed monitoring points</h1>

<#assign reportUrl><@routes.missedAttendance department academicYear /></#assign>
<@reports_macros.reportLoader reportUrl>
	<ul class="dropdown-menu">
		<li>
			<a href="#" data-href="<@routes.missedAttendanceDownloadCsv department academicYear />">
				<i class="icon-table"></i> CSV
			</a>
		</li>
		<li>
			<a href="#" data-href="<@routes.missedAttendanceDownloadXlsx department academicYear />">
				<i class="icon-list-alt"></i> Excel
			</a>
		</li>
		<li>
			<a href="#" data-href="<@routes.missedAttendanceDownloadXml department academicYear />">
				<i class="icon-code"></i> XML
			</a>
		</li>
	</ul>
</@reports_macros.reportLoader>
<@reports_macros.attendanceMonitoringReportScript />

</#escape>