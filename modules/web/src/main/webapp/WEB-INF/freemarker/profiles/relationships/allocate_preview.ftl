<#escape x as x?html>

<#assign previewFormAction><@routes.profiles.relationship_allocate_preview department relationshipType /></#assign>

<#if !command.allocationType?has_content>

	<@f.form commandName="command" action="${previewFormAction}" method="POST">
		<#list command.additions?keys as entity>
			<#list command.additions[entity] as student>
			<input type="hidden" name="additions[${entity}]" value="${student}" />
			</#list>
		</#list>
		<#list command.removals?keys as entity>
			<#list command.removals[entity] as student>
			<input type="hidden" name="removals[${entity}]" value="${student}" />
			</#list>
		</#list>

		<div class="has-error">
			<@bs3form.labelled_form_group path="allocationType" labelText="Choose allocation type">
				<@bs3form.radio>
					<@f.radiobutton path="allocationType" value="${allocationTypes.Replace}" />
					Replace existing ${relationshipType.agentRole}s
					<@fmt.help_popover id="allocationType-replace" content="For any student with a ${relationshipType.agentRole} defined in the spreadsheet, remove any existing ${relationshipType.agentRole}s and add the new ${relationshipType.agentRole}" />
				</@bs3form.radio>
				<@bs3form.radio>
					<@f.radiobutton path="allocationType" value="${allocationTypes.Add}" />
					Add additional ${relationshipType.agentRole}s
					<@fmt.help_popover id="allocationType-replace" content="For any student with a ${relationshipType.agentRole} defined in the spreadsheet, add the new ${relationshipType.agentRole}. Any existing ${relationshipType.agentRole}s will remain" />
				</@bs3form.radio>
			</@bs3form.labelled_form_group>
		</div>
		<div class="submit-buttons">
			<button type="submit" class="btn btn-primary" name="continue" value="true">Continue</button>
			<a href="/profiles/" class="btn btn-default">Cancel</a>
		</div>
	</@f.form>

<#else>

	<h1>You have requested the following ${relationshipType.description?lower_case} changes</h1>

	<#macro student_table studentMap>
		<table class="sortable table table-striped">
			<thead>
				<tr>
					<th class="sortable">First name</th>
					<th class="sortable">Last name</th>
					<th class="sortable">University ID</th>
					<th class="sortable">Tutor</th>
				</tr>
			</thead>
			<tbody>
				<#-- Ignore the warning, multi-key sorting totally works -->
				<#list studentMap?keys?sort_by("lastName", "firstName") as student>
					<#list mapGet(studentMap, student) as entity>
						<tr>
							<td>${student.firstName}</td>
							<td>${student.lastName}</td>
							<td>${student.universityId}</td>
							<td>${entity}</td>
						</tr>
					</#list>
				</#list>
			</tbody>
		</table>
	</#macro>

	<details class="removals">
		<summary class="large-chevron collapsible">
			<h3 class="relationship-change-summary" style="display: inline;">
				<span class="emphasis">${command.renderRemovals?keys?size}</span>
				<@fmt.p number=command.renderRemovals?keys?size singular="student" shownumber=false />: ${relationshipType.description?lower_case} removed
			</h3>
		</summary>


		<@student_table command.renderRemovals />
	</details>

	<details class="additions">
		<summary class="large-chevron collapsible">
			<h3 class="relationship-change-summary" style="display: inline;">
				<span class="emphasis">${command.renderAdditions?keys?size}</span>
				<@fmt.p number=command.renderRemovals?keys?size singular="student" shownumber=false />: ${relationshipType.description?lower_case} added
			</h3>
		</summary>

		<@student_table command.renderAdditions />
	</details>

	<script>
		jQuery('table.sortable').sortableTable();
	</script>

	<@f.form commandName="command" action="${previewFormAction}" method="POST">
		<#include "_allocate_notifications_modal.ftl" />

		<#list command.additions?keys as entity>
			<#list command.additions[entity] as student>
			<input type="hidden" name="additions[${entity}]" value="${student}" />
			</#list>
		</#list>
		<#list command.removals?keys as entity>
			<#list command.removals[entity] as student>
			<input type="hidden" name="removals[${entity}]" value="${student}" />
			</#list>
		</#list>
		<input type="hidden" name="allocationType" value="${command.allocationType}" />

		<div class="submit-buttons">
			<input type="hidden" name="confirm" value="true">
			<button type="button" class="btn btn-primary" data-toggle="modal" data-target="#notify-modal">Confirm</button>
			<a href="/profiles/" class="btn btn-default">Cancel</a>
		</div>
	</@f.form>

</#if>

</#escape>