<h1>Manage monitoring points for ${department.name}</h1>

<form class="form-inline">
	<label>Academic year
		<select id="academicYear">
			<#assign academicYears = [academicYear.previous.toString, academicYear.toString, academicYear.next.toString] />
			<#list academicYears as year>
				<option <#if academicYear.toString == year>selected</#if> value="${year}">${year}</option>
			</#list>
		</select>
	</label>
</form>

<form action="<@url page="/manage/${department.code}/sets/create"/>">
	<h2>Create monitoring schemes</h2>
	<label>
		<input type="radio" name="type" value="blank"/>
		Create blank scheme
		<a class="use-popover" id="popover-create-blank" data-content="Create a new scheme from scratch"><i class="icon-question-sign"></i></a>
	</label>
	<button type="submit" class="btn btn-primary">Create</button>
</form>

<h2>Edit monitoring points</h2>

<#if (setCount == 0)>
	<p><em>There are no monitoring point schemes for ${department.name}</em></p>
<#else>
	<div class="striped-section routes">
		<div class="row-fluid">
			<div class="span10">
				<h3 class="section-title">Route</h3>
			</div>
			<div class="span2">
				<h3 class="section-title">Years</h3>
			</div>
		</div>

		<div class="striped-section-contents">
			<#list routesWithSets?sort_by("code") as route>
				<#assign pointSets = pointSetsForThisYearByRoute[route.code] />
				<div class="item-info">
					<div class="row-fluid">
						<div class="span10 collapsible">
							<h3><i class="icon-fixed-width icon-chevron-right ellipsis"></i> ${route.code?upper_case} ${route.name}</h3>
						</div>
						<div class="span2">
							<#if pointSets?size == 1 && !pointSets?first.year??>
								All
							<#else>
								<#list pointSets?sort_by("year") as set>${set.year}<#if set_has_next>, </#if></#list>
							</#if>
						</div>
					</div>
					<div class="collapsible-target">
						<#if pointSets?size == 1 && !pointSets?first.year??>
							<div class="row-fluid">
								<div class="span10">
									${route.code?upper_case} All years
								</div>
								<div class="span2">
									<a href="" class="btn btn-primary btn-mini">Edit</a>
								</div>
							</div>
						<#else>
							<#list pointSets?sort_by("year") as set>
								<div class="row-fluid">
									<div class="span10">
										${route.code?upper_case} Year ${set.year}
									</div>
									<div class="span2">
										<a href="" class="btn btn-primary btn-mini">Edit</a>
									</div>
								</div>
							</#list>
						</#if>
					</div>
				</div>
			</#list>
		</div>
	</div>
</#if>