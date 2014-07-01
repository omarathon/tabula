<#escape x as x?html>
<#import "../attendance_variables.ftl" as attendance_variables />
<#import "../attendance_macros.ftl" as attendance_macros />

<#if groupedPointMap?keys?size == 0>
	<p><em>No monitoring points found for this academic year.</em></p>
<#else>
	<#if can.do("MonitoringPoints.Record", student)>
		<a class="btn btn-primary" href="<@routes.profileRecord student academicYear.startYear?c />">Record attendance</a>
	</#if>

	<div class="monitoring-points-profile striped-section collapsible <#if expand!true>expanded</#if>">
		<h3 class="section-title">Monitoring points</h3>
		<div class="missed-info">
			<#if !hasAnyMissed>
				<#if is_the_student>
					You have missed 0 monitoring points.
				<#else>
				${student.firstName} has missed 0 monitoring points.
				</#if>
			<#else>
				<#macro missedWarning term>
					<#if (missedPointCountByTerm[term]?? && missedPointCountByTerm[term] > 0)>
						<div class="missed">
							<i class="icon-warning-sign"></i>
							<#if is_the_student>
								You have
							<#else>
								${student.firstName} has
							</#if>
							missed <@fmt.p missedPointCountByTerm[term] "monitoring point" /> in ${term}
						</div>
					</#if>
				</#macro>
				<#list attendance_variables.monitoringPointTermNames as term>
					<@missedWarning term />
				</#list>
				<#list monthNames as month>
					<@missedWarning month />
				</#list>
			</#if>
		</div>

		<div class="striped-section-contents">
			<#list attendance_variables.monitoringPointTermNames as term>
				<#if groupedPointMap[term]??>
					<div class="item-info row-fluid term">
						<div class="span12">
							<h4>${term}</h4>
							<table class="table">
								<tbody>
									<#list groupedPointMap[term] as pointPair>
										<#assign point = pointPair._1() />
										<tr class="point">
											<td class="point" title="${point.name} (<@fmt.monitoringPointWeeksFormat point.startWeek point.endWeek point.scheme.academicYear department />)">
												${point.name}
												(<a class="use-tooltip" data-html="true" title="
													<@fmt.wholeWeekDateFormat
														point.startWeek
														point.endWeek
														point.scheme.academicYear
													/>
												"><@fmt.monitoringPointWeeksFormat
													point.startWeek
													point.endWeek
													point.scheme.academicYear
													department
												/></a>)
											</td>
											<td class="state">
												<#if pointPair._2()??>
													<@attendance_macros.checkpointLabel department=department checkpoint=pointPair._2() />
												<#else>
													<@attendance_macros.checkpointLabel department=department point=pointPair._1() student=student />
												</#if>
											</td>
										</tr>
									</#list>
								</tbody>
							</table>
						</div>
					</div>
				</#if>
			</#list>
			<#list monthNames as month>
				<#if groupedPointMap[month]??>
					<div class="item-info row-fluid term">
						<div class="span12">
							<h4>${month}</h4>
							<table class="table">
								<tbody>
									<#list groupedPointMap[month] as pointPair>
									<#assign point = pointPair._1() />
									<tr class="point">
										<td class="point" title="${point.name} (<@fmt.interval point.startDate point.endDate />)">
											${point.name}
											(<@fmt.interval point.startDate point.endDate />)
										</td>
										<td class="state">
											<#if pointPair._2()??>
												<@attendance_macros.checkpointLabel department=department checkpoint=pointPair._2() />
											<#else>
												<@attendance_macros.checkpointLabel department=department point=pointPair._1() student=student />
											</#if>
										</td>
									</tr>
									</#list>
								</tbody>
							</table>
						</div>
					</div>
				</#if>
			</#list>
		</div>
	</div>
</#if>

</#escape>