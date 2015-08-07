<#import "*/group_components.ftl" as components />
<#escape x as x?html>
	<h1>Create a set of reusable small groups</h1>

	<@f.form id="newGroups" method="POST" commandName="createDepartmentSmallGroupSetCommand" class="form-horizontal">
		<#assign fakeSet = {'groups':[]} />
		<@components.reusable_set_wizard true 'properties' fakeSet />

		<fieldset>
			<@form.labelled_row "name" "Set name">
				<@f.input path="name" cssClass="text" />
				<a class="use-popover" data-html="true"
				   data-content="Give this set of groups a name to distinguish it from any other sets - eg. UG Year 1 seminars and UG Year 2 seminars">
					<i class="icon-question-sign"></i>
				</a>
			</@form.labelled_row>

			<@form.labelled_row "academicYear" "Academic year">
				<@f.select path="academicYear" id="academicYear">
					<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
				</@f.select>
			</@form.labelled_row>
		</fieldset>

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-success use-tooltip spinnable spinner-auto"
				name="${ManageDepartmentSmallGroupsMappingParameters.createAndAddGroups}"
				value="Save and add groups"
				title="Add groups to this set of reusable groups"
				data-container="body"
				/>
			<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="create"
				value="Save and exit"
				title="Save your groups and add students and groups to it later"
				data-container="body"
				/>
			<a class="btn" href="<@routes.groups.crossmodulegroups department />">Cancel</a>
		</div>
	</@f.form>
</#escape>