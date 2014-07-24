<#import "*/group_components.ftl" as components />
<#escape x as x?html>
	<h1>Create a set of reusable small groups</h1>

	<@f.form id="newGroups" method="POST" commandName="createDepartmentSmallGroupSetCommand" class="form-horizontal">
		<p class="progress-arrows">
			<span class="arrow-right active">Properties</span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and edit students"><button type="submit" class="btn btn-link" name="${ManageDepartmentSmallGroupsMappingParameters.createAndAddStudents}">Students</button></span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and edit groups"><button type="submit" class="btn btn-link" name="${ManageDepartmentSmallGroupsMappingParameters.createAndAddGroups}">Groups</button></span>
			<span class="arrow-right arrow-left">Allocate</span>
		</p>

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
				class="btn btn-success use-tooltip"
				name="${ManageDepartmentSmallGroupsMappingParameters.createAndAddStudents}"
				value="Save and add students"
				title="Select which students are included in these groups"
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
			<a class="btn" href="<@routes.crossmodulegroups department />">Cancel</a>
		</div>
	</@f.form>
</#escape>