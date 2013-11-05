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
	<#assign returnTo><@routes.viewDepartmentWithAcademicYear command.department command.academicYear /></#assign>
<div class="monitoring-points">
	<#macro pointsInATerm term>
		<div class="striped-section">
			<h2 class="section-title">${term}</h2>
			<div class="striped-section-contents">
				<#list pointsMap[term] as groupedPoint>
					<div class="item-info row-fluid point">
						<div class="span12">
							<div class="pull-right">
								<a class="btn btn-primary" href="<@routes.record command.department groupedPoint.pointId returnTo/>">
									Record
								</a>
							</div>
							${groupedPoint.name}
							(<@fmt.monitoringPointWeeksFormat groupedPoint.validFromWeek groupedPoint.requiredFromWeek command.academicYear command.department />):
							<#if groupedPoint.routes?size == command.allRoutes?size>
								All routes
							<#else>
								<#assign popoverContent>
									<ul class="unstyled">
										<#list command.allRoutes as route>
											<#assign isInPoint = false />
											<#list groupedPoint.routes as pointRoute>
												<#if pointRoute.code == route.code><#assign isInPoint = true /></#if>
											</#list>
											<li>
												<#if isInPoint>
													<strong><@fmt.route_name route /></strong>
												<#else>
													<@fmt.route_name route />
												</#if>
											</li>
										</#list>
									</ul>
								</#assign>
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