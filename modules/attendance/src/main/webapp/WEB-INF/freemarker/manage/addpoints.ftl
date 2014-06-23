<#escape x as x?html>

<h1>Add points</h1>

<div class="add-points-to-schemes">
	<form method="POST">
		<input name="returnTo" value="<@routes.manageAddPoints command.department command.academicYear.startYear?c />" type="hidden" />

		<#if newPoints == 0>
			<p>Choose which schemes to add points to</p>
		<#else>
			<div class="alert alert-success">
				<strong><@fmt.p newPoints "point" /></strong> added to <strong><@fmt.p changedSchemes "scheme"/></strong>
				<a class="btn" href="<@routes.manageEditPoints command.department command.academicYear.startYear?c schemesParam />">Edit points</a>
			</div>
		</#if>

		<p>
			<button type="button" class="btn add-blank-point" data-href="<@routes.manageAddPointsBlank command.department command.academicYear.startYear?c/>">Add a point</button>
			<button type="button" class="btn copy-points" data-href="<@routes.manageAddPointsCopy command.department command.academicYear.startYear?c/>">Copy points</button>
			<button type="button" class="btn use-template" data-href="<@routes.manageAddPointsTemplate command.department command.academicYear.startYear?c/>">Use template</button>

			<span class="alert alert-warning" style="display: none;">You cannot add the same point to schemes which use different date formats</span>
		</p>

		<table class="week table table-bordered table-striped table-condensed table-hover table-sortable table-checkable tabula-darkRed sb-no-wrapper-table-popout">
			<thead>
				<tr>
					<th class="for-check-all" style="width: 20px;"></th>
					<th>Monitoring schemes - term week points</th>
				</tr>
			</thead>
			<tbody>
				<#list schemeMaps.weekSchemes?keys?sort_by("displayName") as scheme>
					<tr>
						<td>
							<input name="schemes" value="${scheme.id}" type="checkbox" <#if mapGet(schemeMaps.weekSchemes, scheme)>checked</#if>/>
						</td>
						<td>
							${scheme.displayName}
							<span class="muted">(<@fmt.p scheme.members.members?size "student" />, <@fmt.p scheme.points?size "point" />)</span>
						</td>
					</tr>
				</#list>
			</tbody>
		</table>

		<table class="date table table-bordered table-striped table-condensed table-hover table-sortable table-checkable tabula-darkRed sb-no-wrapper-table-popout">
			<thead>
			<tr>
				<th class="for-check-all" style="width: 20px;"></th>
				<th>Monitoring schemes - calendar date points</th>
			</tr>
			</thead>
			<tbody>
				<#list schemeMaps.dateSchemes?keys?sort_by("displayName") as scheme>
				<tr>
					<td>
						<input name="schemes" value="${scheme.id}" type="checkbox" <#if mapGet(schemeMaps.dateSchemes, scheme)>checked</#if>/>
					</td>
					<td>
					${scheme.displayName}
						<span class="muted">(<@fmt.p scheme.members.members?size "student" />, <@fmt.p scheme.points?size "point" />)</span>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</form>
</div>

<a class="btn" href="<@routes.manageHomeForYear command.department command.academicYear.startYear?c />">Done</a>

</#escape>