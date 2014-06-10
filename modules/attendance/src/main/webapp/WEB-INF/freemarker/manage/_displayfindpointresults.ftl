<#import "../attendance_variables.ftl" as attendance_variables />

<#macro oldPointsByTerm pointsMap term>
<div class="striped-section">
	<h2 class="section-title">${term}</h2>
	<div class="striped-section-contents">
		<#list pointsMap[term] as groupedPoint>
			<div class="item-info row-fluid point">
				<div class="span12">
				${groupedPoint.templatePoint.name}
					(<a class="use-tooltip" data-html="true" title="
							<@fmt.wholeWeekDateFormat
				groupedPoint.templatePoint.validFromWeek
				groupedPoint.templatePoint.requiredFromWeek
				groupedPoint.templatePoint.pointSet.academicYear
				/>
						"><@fmt.monitoringPointWeeksFormat
							groupedPoint.templatePoint.validFromWeek
				groupedPoint.templatePoint.requiredFromWeek
				groupedPoint.templatePoint.pointSet.academicYear
				command.department
						/></a>)
					<#if !command.templateScheme??>
						<#local popoverContent>
							<ul>
								<#list groupedPoint.sets?sort_by("displayName") as set>
									<li>${set.displayName}</li>
								</#list>
							</ul>
						</#local>
						<a href="#" class="use-popover" data-content="${popoverContent}" data-html="true" data-placement="right">
							<@fmt.p groupedPoint.sets?size "scheme" />
						</a>
					</#if>
				</div>
			</div>
		</#list>
	</div>
</div>
</#macro>

<#macro pointsByTerm pointsMap term>
<div class="striped-section">
	<h2 class="section-title">${term}</h2>
	<div class="striped-section-contents">
		<#list pointsMap[term] as groupedPoint>
			<div class="item-info row-fluid point">
				<div class="span12">
				${groupedPoint.templatePoint.name}
					(<a class="use-tooltip" data-html="true" title="
							<@fmt.wholeWeekDateFormat
				groupedPoint.templatePoint.startWeek
				groupedPoint.templatePoint.endWeek
				groupedPoint.templatePoint.scheme.academicYear
				/>
						"><@fmt.monitoringPointWeeksFormat
							groupedPoint.templatePoint.startWeek
				groupedPoint.templatePoint.endWeek
				groupedPoint.templatePoint.scheme.academicYear
				command.department
						/></a>)
					<#if !command.templateScheme??>
						<#local popoverContent>
							<ul>
								<#list groupedPoint.schemes?sort_by("displayName") as scheme>
									<li>${scheme.displayName}</li>
								</#list>
							</ul>
						</#local>
						<a href="#" class="use-popover" data-content="${popoverContent}" data-html="true" data-placement="right">
							<@fmt.p groupedPoint.schemes?size "scheme" />
						</a>
					</#if>
				</div>
			</div>
		</#list>
	</div>
</div>
</#macro>

<#macro pointsByMonth pointsMap month>
<div class="striped-section">
	<h2 class="section-title">${month}</h2>
	<div class="striped-section-contents">
		<#list pointsMap[month] as groupedPoint>
			<div class="item-info row-fluid point">
				<div class="span12">
				${groupedPoint.templatePoint.name}
					(<@fmt.interval groupedPoint.templatePoint.startDate groupedPoint.templatePoint.endDate />)
					<#if !command.templateScheme??>
						<#local popoverContent>
							<ul>
								<#list groupedPoint.schemes?sort_by("displayName") as scheme>
									<li>${scheme.displayName}</li>
								</#list>
							</ul>
						</#local>
						<a href="#" class="use-popover" data-content="${popoverContent}" data-html="true" data-placement="right">
							<@fmt.p groupedPoint.schemes?size "scheme" />
						</a>
					</#if>
				</div>
			</div>
		</#list>
	</div>
</div>
</#macro>



<#if findResult.termGroupedOldPoints?keys?has_content>
	<#list attendance_variables.monitoringPointTermNames as term>
		<#if findResult.termGroupedOldPoints[term]??>
			<@oldPointsByTerm findResult.termGroupedOldPoints term />
		</#if>
	</#list>
</#if>

<#if findResult.termGroupedPoints?keys?has_content>
	<#list attendance_variables.monitoringPointTermNames as term>
		<#if findResult.termGroupedPoints[term]??>
			<@pointsByTerm findResult.termGroupedPoints term />
		</#if>
	</#list>
</#if>

<#if findResult.monthGroupedPoints?keys?has_content>
	<#list findResult.monthGroupedPoints?keys as month>
		<#if findResult.monthGroupedPoints[month]??>
			<@pointsByMonth findResult.monthGroupedPoints month />
		</#if>
	</#list>
</#if>