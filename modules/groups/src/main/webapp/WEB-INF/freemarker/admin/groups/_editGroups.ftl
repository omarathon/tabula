<#escape x as x?html>
	<#if smallGroupSet.linked>
		<@form.row>
			<p>There are <@fmt.p smallGroupSet.groups?size "group" /> in ${smallGroupSet.name} (from <i class="icon-link"></i> ${smallGroupSet.linkedDepartmentSmallGroupSet.name}).</p>

			<#if smallGroupSet.groups?size gt 0>
				<ul>
					<#list smallGroupSet.groups as group>
						<li>${group.name}</li>
					</#list>
				</ul>
			</#if>
		</@form.row>
	<#else>
		<@form.labelled_row "defaultMaxGroupSizeEnabled", "Group size">
			<@form.label checkbox=true>
				<@f.radiobutton path="defaultMaxGroupSizeEnabled" id="unlimitedDefaultGroupSize" value="false" />
				Unlimited
			</@form.label>

			<@form.label checkbox=true>
				<@f.radiobutton path="defaultMaxGroupSizeEnabled" id="limitedDefaultGroupSize" value="true" selector=".max-group-size-options" />
				Maximum
				<span class="max-group-size-options">
					<@f.input path="defaultMaxGroupSize" type="number" min="0" cssClass="input-mini" />
				</span>
				students per group
				<a class="use-popover" data-html="true"
				   data-content="This is the default maximum size for any new groups you create.  You can adjust the maximum size of individual groups">
					<i class="icon-question-sign"></i>
				</a>
			</@form.label>

			<@f.errors path="defaultMaxGroupSize" cssClass="error" />
		</@form.labelled_row>

		<@form.row>
			<@form.field>
				<#-- Well this sucks -->
				<span class="uneditable-input hidden">&nbsp;</span>
				&nbsp;
				Max
			</@form.field>
		</@form.row>

		<#list command.groupNames as group>
			<@form.labelled_row "groupNames[${group_index}]" "${group_index + 1}.">
				<@f.input path="groupNames[${group_index}]" cssClass="text" />
				&nbsp;
				<span class="max-group-size-options">
					<@f.input path="maxGroupSizes[${group_index}]" type="number" min="0" cssClass="text input-mini" />
				</span>
				&nbsp;
				<button type="button" class="btn btn-danger" data-toggle="remove"><i class="icon-remove"></i></button>
			</@form.labelled_row>
		</#list>

		<#assign group_index = command.groupNames?size />

		<@form.labelled_row "groupNames[${group_index}]" "${group_index + 1}.">
			<@f.input path="groupNames[${group_index}]" cssClass="text" />
			&nbsp;
			<span class="max-group-size-options">
				<@f.input path="maxGroupSizes[${group_index}]" type="number" min="0" value="15" cssClass="text input-mini" />
			</span>
		</@form.labelled_row>

		<@form.row>
			<@form.field>
				<button type="button" class="btn" data-toggle="add" title="Add another group" disabled="disabled">
					<i class="icon-plus"></i> Add group
				</button>
			</@form.field>
		</@form.row>

		<script type="text/javascript">
			jQuery(function($) {
				var setMaxSizeOptions = function() {
					if ($("#defaultMaxGroupSizeEnabled:checked").val()){
						$('#defaultMaxGroupSize').removeAttr('disabled');
						$(".groupSizeUnlimited").hide();
						$(".groupSizeLimited").show();
					} else {
						$('#defaultMaxGroupSize').attr('disabled', 'disabled');
						$(".groupSizeUnlimited").show();
						$(".groupSizeLimited").hide();
					}
				}

				setMaxSizeOptions();

				$('#defaultMaxGroupSizeEnabled').change(setMaxSizeOptions);

				$('button[data-toggle="add"]').each(function() {
					var $button = $(this);
					var $group = $button.closest('.control-group').prev('.control-group');
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
					var $group = $button.closest('.control-group').prev('.control-group');

					var $clone = $group.clone();

					var index = parseInt(/\[(\d+)\]/.exec($clone.find('input[type="text"]').attr('name'))[1]);
					$clone.find('label').text((index + 1) + ".");

					$clone.insertBefore($group);
					$clone.find('.controls')
						.append(' &nbsp; ')
						.append('<button type="button" class="btn btn-danger" data-toggle="remove"><i class="icon-remove"></i></button>');

					var nextIndex = index + 1;
					var $name = $group.find('input[type="text"]');
					$name.attr('name', 'groupNames[' + nextIndex + ']');
					$name.attr('id', 'groupNames' + nextIndex);

					var $max = $group.find('input[type="number"]');
					$max.attr('name', 'maxGroupSizes[' + nextIndex + ']');
					$max.attr('id', 'maxGroupSizes' + nextIndex);

					$group.find('label').attr('for', $name.attr('id')).text((nextIndex + 1) + '.');

					$name.val('').focus();
					$button.attr('disabled', 'disabled');
				});

				$(document.body).on('click', 'button[data-toggle="remove"]', function() {
					var $button = $(this);
					var $group = $button.closest('.control-group');

					$group.remove();

					// Re-order all of the groups
					var groups = $('input[name^="groupNames"]').closest('.control-group');
					groups.each(function(index) {
						var $group = $(this);

						var $name = $group.find('input[type="text"]');
						$name.attr('name', 'groupNames[' + index + ']');
						$name.attr('id', 'groupNames' + index);

						var $max = $group.find('input[type="number"]');
						$max.attr('name', 'maxGroupSizes[' + index + ']');
						$max.attr('id', 'maxGroupSizes' + index);

						$group.find('label').attr('for', $name.attr('id')).text((index + 1) + '.');
					});
				});

				// Set up radios to enable/disable max students per group field
				$("input:radio[name='defaultMaxGroupSizeEnabled']").radioControlled();
			});
		</script>
	</#if>
</#escape>