<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Edit reusable small groups: ${smallGroupSet.name}</h1>

	<@f.form id="editGroups" method="POST" commandName="command" class="form-horizontal">
		<p class="progress-arrows">
			<span class="arrow-right">Properties</span>
			<span class="arrow-right arrow-left">Students</span>
			<span class="arrow-right arrow-left active">Groups</span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and allocate students to groups"><button type="submit" class="btn btn-link" name="${ManageDepartmentSmallGroupsMappingParameters.editAndAllocate}">Allocate</button></span>
		</p>

		<#list command.groupNames as group>
			<@form.labelled_row "groupNames[${group_index}]" "${group_index + 1}.">
				<@f.input path="groupNames[${group_index}]" cssClass="text" />
			</@form.labelled_row>
		</#list>

		<#assign group_index = command.groupNames?size />

		<@form.labelled_row "groupNames[${group_index}]" "${group_index + 1}.">
			<@f.input path="groupNames[${group_index}]" cssClass="text" />

			<button type="button" class="btn" data-toggle="add" title="Add another group" disabled="disabled">
				<i class="icon-plus"></i>
			</button>
		</@form.labelled_row>

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-success use-tooltip"
				name="${ManageDepartmentSmallGroupsMappingParameters.editAndAllocate}"
				value="Save and allocate students to groups"
				title="Allocate students to this set of reusable groups"
				data-container="body"
				/>
			<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="create"
				value="Save and exit"
				title="Save your groups and allocate students later"
				data-container="body"
				/>
			<a class="btn" href="<@routes.crossmodulegroups smallGroupSet.department />">Cancel</a>
		</div>
	</@f.form>

	<script type="text/javascript">
		jQuery(function($) {
			$('button[data-toggle="add"]').each(function() {
				var $button = $(this);
				var $group = $button.closest('.control-group');
				var $input = $group.find('input[type="text"]');

				$input.on('paste', function() {
					setTimeout(function() {
						$input.trigger('change');
					}, 50);
				});

				$input.on('change keyup', function() {
					if ($input.val().length > 0) {
						$button.removeAttr('disabled');
					} else {
						$button.attr('disabled', 'disabled');
					}
				});
			});

			$('button[data-toggle="add"]').on('click', function() {
				var $button = $(this);
				var $group = $button.closest('.control-group');

				var $clone = $group.clone();
				$clone.find('button[data-toggle="add"]').remove();

				var index = parseInt(/\[(\d+)\]/.exec($clone.find('input[type="text"]').attr('name'))[1]);
				console.log(index);
				$clone.find('label').text((index + 1) + ".");

				$clone.insertBefore($group);

				var nextIndex = index + 1;
				var $input = $group.find('input[type="text"]');
				$input.attr('name', 'groupNames[' + nextIndex + ']');
				$input.attr('id', 'groupNames' + nextIndex);
				$group.find('label').attr('for', $input.attr('id')).text((nextIndex + 1) + '.');

				$input.val('').focus();
				$button.attr('disabled', 'disabled');
			});
		});
	</script>
</#escape>