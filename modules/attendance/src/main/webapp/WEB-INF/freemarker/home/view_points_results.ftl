<#escape x as x?html>
<#import "../attendance_variables.ftl" as attendance_variables />

<#assign validationError>
	<@spring.bind path="command.students">
		<#if status.error>
		<div class="alert alert-error"><@f.errors path="command.students" cssClass="error"/></div>
		</#if>
	</@spring.bind>
</#assign>

<#if validationError?has_content>
	<#noescape>${validationError}</#noescape>
<#elseif pointsMap?keys?size == 0>
<p><em>No points exist for the selected options</em></p>
<#else>
	<#assign filterQuery = command.serializeFilter />
	<#assign returnTo><@routes.viewDepartmentWithAcademicYear command.department command.academicYear filterQuery/></#assign>
<div class="monitoring-points">
	<#macro pointsInATerm term>
		<div class="striped-section">
			<h2 class="section-title">${term}</h2>
			<div class="striped-section-contents">
				<#list pointsMap[term] as groupedPoint>
					<div class="item-info row-fluid point">
						<div class="span12">
							<div class="pull-right">
								<#local record_url><@routes.record command.department groupedPoint.pointId filterQuery returnTo/></#local>
								<@fmt.permission_button
									permission='MonitoringPoints.Record'
									scope=(groupedPoint.routes?first)._1()
									action_descr='record monitoring points'
									classes='btn btn-primary'
									href=record_url>
									Record
								</@fmt.permission_button>
							</div>
							${groupedPoint.name}
							(<@fmt.monitoringPointWeeksFormat groupedPoint.validFromWeek groupedPoint.requiredFromWeek command.academicYear command.department />):
							<#if groupedPoint.routes?size == command.allRoutes?size>
								All routes
							<#else>
								<#local popoverContent>
									<p class="muted"><small>Only routes that have students matching the filter are applicable.</small></p>
									<ul class="unstyled">
										<#list command.allRoutes as route>
											<#local isInPoint = false />
											<#list groupedPoint.routes as pointRoutePair>
												<#if pointRoutePair._1().code == route.code><#local isInPoint = true /></#if>
											</#list>
											<li>
												<#if isInPoint>
													<@fmt.route_name route />
												<#else>
													<span class="muted"><@fmt.route_name route /></span>
												</#if>
											</li>
										</#list>
										<#list groupedPoint.routes as pointRoutePair>
											<#if !pointRoutePair._2()><li><span title="${pointRoutePair._1().department.name}"><@fmt.route_name pointRoutePair._1() /></span></li></#if>
										</#list>
									</ul>
								</#local>
								<a class="use-wide-popover" data-title="Applicable routes" data-content="${popoverContent}" data-html="true" data-placement="bottom">
									<@fmt.p groupedPoint.routes?size "route" />
								</a>
							</#if>
						</div>
					</div>
				</#list>
			</div>
		</div>
	</#macro>
	<#list attendance_variables.monitoringPointTermNames as term>
		<#if pointsMap[term]??>
			<@pointsInATerm term/>
		</#if>
	</#list>
</div>
</#if>
	
</#escape>