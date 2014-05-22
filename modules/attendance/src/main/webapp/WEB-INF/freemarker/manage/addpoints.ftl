<#escape x as x?html>

<h1>Add points</h1>

<div class="add-points-to-schemes">
	<form method="POST">
		<input name="returnTo" value="<@routes.manageAddPoints command.department command.academicYear.startYear?c />" type="hidden" />

		<#if newPoints == 0>
			<p>Choose scheme/s to add points</p>
		<#else>
			<div class="alert alert-success">
				<strong><@fmt.p newPoints "point" /></strong> added to <strong><@fmt.p changedSchemes "scheme"/></strong>
				<#-- TODO add Edit points button -->
			</div>
		</#if>
		<p>
			<button type="button" class="btn add-blank-point" data-href="<@routes.manageAddPointsBlank command.department command.academicYear.startYear?c/>">Add a point</button>
			<button type="button" class="btn copy-points" data-href="<@routes.manageAddPointsCopy command.department command.academicYear.startYear?c/>">Copy points</button>
			<button type="button" class="btn use-template" data-href="<@routes.manageAddPointsTemplate command.department command.academicYear.startYear?c/>">Use template</button>
		</p>

		<table class="table table-bordered table-striped table-condensed table-hover table-sortable table-checkable tabula-darkRed sb-no-wrapper-table-popout">
			<thead>
				<tr>
					<th class="for-check-all" style="width: 20px;"></th>
					<th>Monitoring schemes</th>
				</tr>
			</thead>
			<tbody>
				<#list schemeMap?keys?sort_by("displayName") as scheme>
					<tr>
						<td>
							<input name="schemes" value="${scheme.id}" type="checkbox" data-pointstyle="${scheme.pointStyle.dbValue}" <#if mapGet(schemeMap, scheme)>checked</#if>/>
						</td>
						<td>
							${scheme.displayName}
							<span class="muted">(<@fmt.p scheme.members.members?size "student" />, <@fmt.p scheme.points?size "${scheme.pointStyle.description?lower_case} point" />)</span>
						</td>
					</tr>
				</#list>
			</tbody>
		</table>
	</form>
</div>

<a class="btn" href="<@routes.manageHomeForYear command.department command.academicYear.startYear?c />">Done</a>

</#escape>