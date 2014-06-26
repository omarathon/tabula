<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<#macro deptheaderroutemacro dept>
	<@routes.viewPoints dept academicYear.startYear?c />
</#macro>
<#assign deptheaderroute = deptheaderroutemacro in routes/>
<@fmt.deptheader "View monitoring points" "for" filterCommand.department routes "deptheaderroute" />

<#if updatedMonitoringPoint??>
<div class="alert alert-success">
	<button type="button" class="close" data-dismiss="alert">&times;</button>
	Attendance recorded for '${updatedMonitoringPoint.name}'
</div>
</#if>

<#assign submitUrl><@routes.viewPoints filterCommand.department filterCommand.academicYear.startYear?c /></#assign>

<#assign filterCommand = filterCommand />
<#assign filterCommandName = "filterCommand" />
<#assign filterResultsPath = "/WEB-INF/freemarker/view/_points_results.ftl" />
<#include "/WEB-INF/freemarker/filter_bar.ftl" />
</#escape>