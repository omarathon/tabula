<#import "*/group_components.ftl" as components />
<#escape x as x?html>
	<h1>Edit reusable small groups: ${smallGroupSet.name}</h1>

	<@f.form id="editGroups" method="POST" commandName="editDepartmentSmallGroupSetCommand" class="form-horizontal">
		<@components.reusable_set_wizard false 'properties' smallGroupSet />

		<fieldset>
			<@form.labelled_row "name" "Set name">
				<@f.input path="name" cssClass="text" />
				<a class="use-popover" data-html="true"
				   data-content="Give this set of groups a name to distinguish it from any other sets - eg. UG Year 1 seminars and UG Year 2 seminars">
					<i class="icon-question-sign"></i>
				</a>
			</@form.labelled_row>

			<@form.labelled_row "academicYear" "Academic year">
				<@spring.bind path="academicYear">
					<span class="uneditable-value">${status.actualValue.label} <span class="hint">(can't be changed)</span></span>
				</@spring.bind>
			</@form.labelled_row>
		</fieldset>

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-success use-tooltip"
				name="${ManageDepartmentSmallGroupsMappingParameters.editAndAddGroups}"
				value="Save and edit groups"
				title="Edit groups for set of reusable groups"
				data-container="body"
				/>
			<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="edit"
				value="Save and exit"
				title="Save your groups and add students and groups to it later"
				data-container="body"
				/>
			<a class="btn" href="<@routes.crossmodulegroups smallGroupSet.department />">Cancel</a>
		</div>
	</@f.form>
</#escape>