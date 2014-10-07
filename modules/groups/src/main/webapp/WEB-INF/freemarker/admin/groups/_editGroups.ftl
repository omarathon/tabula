<#escape x as x?html>
	<style type="text/css">
		.existing-group.deleted label {
			text-decoration: line-through;
			color: @grayLight;
		}

		.existing-group.deleted .help-inline {
			text-decoration: none;
		}

		.existing-group.deleted input {
			cursor: not-allowed;
			background-color: #eee !important;
			text-decoration: line-through;
		}

		.help-inline.hidden { display: none !important; }
	</style>

	<#if smallGroupSet.linked>
		<@form.row>
			<p>There are <@fmt.p smallGroupSet.groups?size "group" /> in ${smallGroupSet.name} (from ${smallGroupSet.linkedDepartmentSmallGroupSet.name}).</p>

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

		<#macro fields index is_new=false no_remove_button=false>
			<#local cssClass><#compress>
				<#if is_new>
					new-group
				<#else>
					existing-group
					<@spring.bind path="delete">
						<#if status.actualValue>deleted</#if>
					</@spring.bind>
				</#if>
			</#compress></#local>

			<@form.row path="name" cssClass=cssClass>
				<@form.label for="${index}-name">
					${index}.
				</@form.label>

				<@form.field>
					<@f.input path="name" id="${index}-name" cssClass="text" />
					&nbsp;
					<span class="max-group-size-options">
						<@f.input id="${index}-maxGroupSize" path="maxGroupSize" type="number" min="0" cssClass="text input-mini" />
					</span>
					<#if !no_remove_button>
						&nbsp;
						<#if is_new>
							<button type="button" class="btn btn-danger" data-toggle="remove"><i class="icon-remove"></i></button>
						<#else>
							<@f.hidden path="delete" />
							<button type="button" class="btn btn-danger" data-toggle="mark-deleted"><i class="icon-remove"></i></button>
							<button type="button" class="btn btn-info" data-toggle="undo-deleted"><i class="icon-undo"></i></button>
							<span class="help-inline hidden"><small>
								Click "Save and add students" or "Save and exit" to delete this group and any events associated with it
							</small></span>
						</#if>
					</#if>

					<@form.errors "name" />
					<@form.errors "maxGroupSize" />
					<#if !is_new>
						<@form.errors "delete" />
					</#if>
				</@form.field>
			</@form.row>
		</#macro>

		<#list smallGroupSet.groups as group>
			<@spring.nestedPath path="existingGroups[${group.id}]">
				<@fields index=(group_index + 1) />
			</@spring.nestedPath>
		</#list>
		<#list command.newGroups as newGroup>
			<@spring.nestedPath path="newGroups[${newGroup_index}]">
				<@fields index=(smallGroupSet.groups?size + newGroup_index + 1) is_new=true />
			</@spring.nestedPath>
		</#list>

		<@spring.nestedPath path="newGroups[${command.newGroups?size}]">
			<@fields index=(smallGroupSet.groups?size + command.newGroups?size + 1) is_new=true no_remove_button=true />
		</@spring.nestedPath>

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

					var offset = $('.control-group.existing-group').length;

					var $clone = $group.clone();

					var index = parseInt(/\[(\d+)\]/.exec($clone.find('input[type="text"]').attr('name'))[1]);
					$clone.find('label').text((index + offset + 1) + ".");

					$clone.insertBefore($group);
					$clone.find('.controls')
						.append(' &nbsp; ')
						.append('<button type="button" class="btn btn-danger" data-toggle="remove"><i class="icon-remove"></i></button>');

					var nextIndex = index + 1;
					var $name = $group.find('input[type="text"]');
					$name.attr('name', 'newGroups[' + nextIndex + '].name');
					$name.attr('id', (nextIndex + offset + 1) + '-name');

					var $max = $group.find('input[type="number"]');
					$max.attr('name', 'newGroups[' + nextIndex + '].maxGroupSize');
					$max.attr('id', (nextIndex + offset + 1) + '-maxGroupSize');

					$group.find('label').attr('for', $name.attr('id')).text((nextIndex + offset + 1) + '.');

					$name.val('').focus();
					$button.attr('disabled', 'disabled');
				});

				$(document.body).on('click', 'button[data-toggle="remove"]', function() {
					var $button = $(this);
					var $group = $button.closest('.control-group');

					$group.remove();

					// Re-order all of the groups
					var groups = $('.control-group.new-group');
					var offset = $('.control-group.existing-group').length;
					groups.each(function(index) {
						var $group = $(this);

						var $name = $group.find('input[type="text"]');
						$name.attr('name', 'newGroups[' + index + '].name');
						$name.attr('id', (index + offset + 1) + '-name');

						var $max = $group.find('input[type="number"]');
						$max.attr('name', 'newGroups[' + index + '].maxGroupSize');
						$max.attr('id', (index + offset + 1) + '-maxGroupSize');

						$group.find('label').attr('for', $name.attr('id')).text((index + offset + 1) + '.');
					});
				});

				// Set up radios to enable/disable max students per group field
				$("input:radio[name='defaultMaxGroupSizeEnabled']").radioControlled();

				// Stop 'Enter' from submitting the form
				$('input[name$=".name"]').closest('form')
						.off('keyup.inputSubmitProtection keypress.inputSubmitProtection')
						.on('keyup.inputSubmitProtection keypress.inputSubmitProtection', function(e){
							var code = e.keyCode || e.which;
							if (code  == 13) {
								e.preventDefault();
								return false;
							}
						});

				$('.existing-group').each(function() {
					var $this = $(this);

					var $deleteButton = $this.find('.btn-danger');
					var $undoButton = $this.find('.btn-info');
					var $helpBlock = $this.find('.help-inline');
					var $hiddenField = $this.find('input[name$="delete"]');

					var handleButtonDisplay = function() {
						if ($this.is('.deleted')) {
							$deleteButton.hide();
							$undoButton.show();
							$helpBlock.removeClass('hidden');
						} else {
							$deleteButton.show();
							$undoButton.hide();
							$helpBlock.addClass('hidden');
						}
					};
					handleButtonDisplay();

					$deleteButton.on('click', function() {
						$this.addClass('deleted');
						$hiddenField.val('true');
						handleButtonDisplay();
					});

					$undoButton.on('click', function() {
						$this.removeClass('deleted');
						$hiddenField.val('false');
						handleButtonDisplay();
					});
				});
			});
		</script>
	</#if>
</#escape>