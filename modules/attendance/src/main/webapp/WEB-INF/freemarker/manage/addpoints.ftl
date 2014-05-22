<#escape x as x?html>

<h1>Add points</h1>

<div class="add-points-to-schemes">
	<form method="POST">
		<input name="returnTo" value="<@routes.manageAddPoints command.department command.academicYear.startYear?c />" type="hidden" />
		<p>Choose scheme/s to add points</p>
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
							<input name="schemes" value="${scheme.id}" type="checkbox" <#if mapGet(schemeMap, scheme)>checked</#if>/>
						</td>
						<td>
							${scheme.displayName}
						</td>
					</tr>
				</#list>
			</tbody>
		</table>
	</form>
</div>

<a class="btn" href="<@routes.manageHomeForYear command.department command.academicYear.startYear?c />">Done</a>

</#escape>