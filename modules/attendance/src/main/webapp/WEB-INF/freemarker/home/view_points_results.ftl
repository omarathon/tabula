<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />
<#import "../attendance_variables.ftl" as attendance_variables />

<#assign validationError>
	<@spring.bind path="command.students">
		<#if status.error>
			<p class="alert alert-error"><@f.errors path="command.students" cssClass="error"/></p>
		</#if>
	</@spring.bind>
</#assign>

<#if validationError?has_content>
	<#noescape>${validationError}</#noescape>
<#elseif !command.hasBeenFiltered && command.filterTooVague>
	<p class="alert alert-info"><i class="icon-lightbulb"></i> Find points for students using the filter options above.</p>
<#elseif command.hasBeenFiltered && command.filterTooVague>
	<p class="alert"><i class="icon-warning-sign"></i> The filter you have chosen includes too many students.</p>
<#elseif pointsMap?keys?size == 0>
	<p class="alert"><i class="icon-warning-sign"></i> No points exist for the selected options.</p>
<#else>
	<#assign filterQuery = command.serializeFilter />
	<#assign returnTo><@routes.viewDepartmentPointsWithAcademicYear command.department command.academicYear filterQuery/></#assign>
	<#function permission_button_function groupedPoint>
		<#local record_url><@routes.record command.department groupedPoint.pointId filterQuery returnTo/></#local>
		<#local result>
			<@fmt.permission_button
				permission='MonitoringPoints.Record'
				scope=(groupedPoint.routes?first)._1()
				action_descr='record monitoring points'
				classes='btn btn-primary'
				href=record_url
				tooltip='Record'
			>
				Record
			</@fmt.permission_button>
		</#local>
		<#return result>
	</#function>
	<div class="monitoring-points">
		<#list attendance_variables.monitoringPointTermNames as term>
			<#if pointsMap[term]??>
				<@attendance_macros.groupedPointsInATerm pointsMap term command.department permission_button_function />
			</#if>
		</#list>
	</div>
</#if>

</#escape>