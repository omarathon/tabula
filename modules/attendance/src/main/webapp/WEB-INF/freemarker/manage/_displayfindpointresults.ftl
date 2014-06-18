<#import "*/attendance_variables.ftl" as attendance_variables />
<#import "*/attendance_macros.ftl" as attendance_macros />

<#if findResult.termGroupedOldPoints?keys?has_content>
	<#list attendance_variables.monitoringPointTermNames as term >
		<#if findResult.termGroupedOldPoints[term]??>
			<@attendance_macros.groupedPointsBySection findResult.termGroupedOldPoints term ; groupedPoint >
				<div class="span12">
				${groupedPoint.templatePoint.name}
				(<a class="use-tooltip" data-html="true" title="
					<@fmt.wholeWeekDateFormat
						groupedPoint.templatePoint.validFromWeek
						groupedPoint.templatePoint.requiredFromWeek
						groupedPoint.templatePoint.pointSet.academicYear />
				">	<@fmt.monitoringPointWeeksFormat
						groupedPoint.templatePoint.validFromWeek
						groupedPoint.templatePoint.requiredFromWeek
						groupedPoint.templatePoint.pointSet.academicYear
						command.department />
				</a>)
				<#if !templateScheme??>
					<a href="#" class="use-popover" data-content=
						"<ul>
							<#list groupedPoint.sets?sort_by("displayName") as set>
								<li>${set.displayName}</li>
							</#list>
						</ul>"
						data-html="true" data-placement="right">
						<@fmt.p groupedPoint.sets?size "scheme" />
					</a>
				</#if>
			</div>
			</@attendance_macros.groupedPointsBySection>
		</#if>
	</#list>
</#if>

<#if findResult.termGroupedPoints?keys?has_content >
	<#list attendance_variables.monitoringPointTermNames as term>
		<#if findResult.termGroupedPoints[term]??>
			<@attendance_macros.groupedPointsBySection findResult.termGroupedPoints term ; groupedPoint >
				<div class="span12">
					${groupedPoint.templatePoint.name}
						(<a class="use-tooltip" data-html="true" title="
							<@fmt.wholeWeekDateFormat
								groupedPoint.templatePoint.startWeek
								groupedPoint.templatePoint.endWeek
								groupedPoint.templatePoint.scheme.academicYear	/>
						 ">	<@fmt.monitoringPointWeeksFormat
								groupedPoint.templatePoint.startWeek
								groupedPoint.templatePoint.endWeek
								groupedPoint.templatePoint.scheme.academicYear
								command.department/>
						</a>)
					<#if !templateScheme??><@groupedPointSchemePopover groupedPoint /></#if>
				</div>
			</@attendance_macros.groupedPointsBySection>
		</#if>
	</#list>
</#if>

<#if findResult.monthGroupedPoints?keys?has_content>
	<#list monthNames as month>
		<#if findResult.monthGroupedPoints[month]??>
			<@attendance_macros.groupedPointsBySection findResult.monthGroupedPoints month ; groupedPoint >
				<div class="span12">
					${groupedPoint.templatePoint.name}
					(<@fmt.interval groupedPoint.templatePoint.startDate groupedPoint.templatePoint.endDate />)
					<#if !templateScheme??><@groupedPointSchemePopover groupedPoint /></#if>
				</div>
			</@attendance_macros.groupedPointsBySection>
		</#if>
	</#list>
</#if>

