<#--

first page of the form to setup a bunch of assignments from SITS.

-->
<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign commandName="addAssignmentsCommand"/>
<#assign command=addAssignmentsCommand />

<h1>Setup assignments</h1>

<#assign step=RequestParameters.action!'select'/>

<#if step='select'>

	<h2>Step 1 - choose which assignments to setup</h2>

	<p>Below are all of the assignments defined for this department in SITS, the central system.</p>

	<p>The first thing to do is choose which ones you want to set up to use for assessment.
	Use the checkboxes on the left hand side to choose which ones you want to setup in the coursework submission system,
	and then click Next. Some items (such as those marked "Audit Only") have already been unchecked.
	</p>

<#elseif step='options'>

	<h2>Step 2 - choose options for assignments</h2>

	<p>
		Now you need to choose how you want these assignments to behave, such as submission dates
		and resubmission behaviour. Use the checkboxes to select some assignments and then either click
		<strong>Set options</strong> or click one of the coloured labels to re-use some options.
	</p>

</#if>

<@f.form method="post" id="batch-add-form" action="/admin/department/${command.department.code}/setup-assignments" commandName=commandName cssClass="form-horizontal">

<input type="hidden" name="action" value="error" /><!-- this is changed before submit -->


<@form.labelled_row "academicYear" "Academic year">
	<#if step="select">
		<@f.select path="academicYear" id="academicYear">
			<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
		</@f.select>
	<#else>
  	<@f.hidden path="academicYear"/>
  	<span class="uneditable-value">
  	<@spring.bind path="academicYear">${status.actualValue.label}</@spring.bind>
  	</span>
  </#if>
</@form.labelled_row>

<div class="row-fluid">
<div class="span10">

<table class="table table-bordered table-striped" id="batch-add-table">
<tr>
	<th>
		<#if step="options">
			<div class="check-all checkbox">
				<label><span class="very-subtle"></span>
					<input type="checkbox" checked="checked" class="collection-check-all use-tooltip" title="Select/unselect all">
				</label>
			</div>
    </#if>
	</th>
	<th>Module</th>
	<th>Seq</th>
	<th>Assignment name</th>
	<#if step="options">
	<th></th>
	<th></th>
	</#if>
</tr>
<#list command.assignmentItems as item>
<@spring.nestedPath path="assignmentItems[${item_index}]">
<#if step="select" || item.include>

<tr class="itemContainer">
	<td>
		<@f.hidden path="upstreamAssignment" />
		<#if step="select">
			<@f.checkbox path="include" cssClass="collection-checkbox" />
		<#else>
			<@f.hidden path="include" />

			<input type="checkbox" checked="checked" class="collection-checkbox" />
		</#if>
	</td>
	<td class="selectable">
		${item.upstreamAssignment.moduleCode?upper_case}
	</td>
	<td class="selectable">
		${item.upstreamAssignment.sequence}
	</td>
	<td class="selectable">
		${item.upstreamAssignment.name}
	</td>
	<#if step="options">
 	<td class="assignment-editable-fields-cell">
 		<@f.hidden path="openDate" cssClass="open-date-field" />
 		<@f.hidden path="closeDate" cssClass="close-date-field" />
 		<span class="dates-label"></span>
 	</td>
 	<td>
 		<@f.hidden path="optionsId" cssClass="options-id-input" />
 		<span class="options-id-label"></span>
 	</td>
  </#if>
</tr>

<#else>

	<#-- even if they are unselected we still need to remember that fact -->
	<#-- FIXME these hidden inputs will be between table rows which is invalid HTML -->
	<@f.hidden path="upstreamAssignment" />
  <@f.hidden path="include" />

</#if>
</@spring.nestedPath>
</#list>
</table>

</div>

<div class="span2">

<#if step='select'>
<button class="btn btn-large btn-primary btn-block" data-action="options">Next</button>
<#elseif step='options'>
<div id="options-buttons">
<div id="selected-count">0 selected</div>
<div id="selected-deselect"><a href="#">Clear selection</a></div>
<#-- options sets -->
<a class="btn btn-warning btn-block" id="set-options-button" data-target="#set-options-modal" href="<@url page="/admin/department/${department.code}/shared-options"/>">
	Set options&hellip;
</a>
<a class="btn btn-warning btn-block" id="set-dates-button" data-target="#set-dates-modal">
	Set dates&hellip;
</a>

</div>
</#if>

</div>

</div><#-- .row-fluid -->

</@f.form>

<#if step='options'>
	<div class="modal hide fade" id="set-options-modal" tabindex="-1" role="dialog" aria-labelledby="set-options-label" aria-hidden="true">
		<div class="modal-header">
			<button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
			<h3 id="set-options-label">Set options</h3>
		</div>
		<div class="modal-body">



		</div>
		<div class="modal-footer">
			<button class="btn" data-dismiss="modal" aria-hidden="true">Close</button>
			<button class="btn btn-primary">Save options</button>
		</div>
	</div>

	<div class="modal hide fade" id="set-dates-modal" tabindex="-1" role="dialog" aria-labelledby="set-options-label" aria-hidden="true">
		<div class="modal-header">

		</div>
		<div class="modal-body">

			<@form.row>
				<@form.label>Open date</@form.label>
				<@form.field>
					<input type="text" id="modal-open-date" name="openDate" class="date-time-picker">
				</@form.field>
			</@form.row>

			<@form.row>
				<@form.label>Close date</@form.label>
				<@form.field>
					<input type="text" id="modal-close-date" name="closeDate" class="date-time-picker">
				</@form.field>
			</@form.row>

		</div>
		<div class="modal-footer">
			<button class="btn" data-dismiss="modal" aria-hidden="true">Close</button>
      <button class="btn btn-primary">Set dates</button>
		</div>
	</div>
</#if>

<script type="text/javascript">
//<[![CDATA[
jQuery(function($){

	var optionGroupCount = 0;

	var $form = $('#batch-add-form');

	// When clicking Next, set the action parameter to the relevant value before submitting
	$form.find('button[data-action=options]').click(function(event){
		var action = $(this).data('action');
		if (action) {
			$form.find('input[name=action]').val(action);
		}
	});

	// Set up checkboxes for the big table

	$('#batch-add-table').bigList({
		setup : function() {
			var $container = this;

			$('#selected-deselect').click(function(){
				$container.find('.collection-checkbox, .collection-check-all').attr('checked',false);
				$container.find("tr.selected").removeClass('selected');
				$('#selected-count').text("0 selected");
				return false;
			});
		},

		onChange : function() {
			this.closest("tr").toggleClass("selected", this.is(":checked"));
			var x = $('#batch-add-table .collection-checkbox:checked').length;
    	$('#selected-count').text(x+" selected");
		},

		onSomeChecked : function() {
			$('#set-options-button').removeClass('disabled');
		},

		onNoneChecked : function() {
			$('#set-options-button').addClass('disabled');
			$('#selected-count').text("0 selected");
		}
	});

	// cool selection mechanism...
	var batchTableMouseDown = false;
	$('#batch-add-table td.selectable')
		.mousedown(function(){
			batchTableMouseDown = true;
			var $row = $(this).closest('tr');
			$row.toggleClass('selected');
			var checked = $row.hasClass('selected');
			$row.find('.collection-checkbox').attr('checked', checked);
			return false;
		})
		.mouseover(function(){
			if (batchTableMouseDown) {
				var $row = $(this).closest('tr');
				$row.toggleClass('selected');
				var checked = $row.hasClass('selected');
				$row.find('.collection-checkbox').attr('checked', checked);
			}
		});

	$(document).mouseup(function(){
		batchTableMouseDown = false;
		$('#batch-add-table').bigList('changed');
	});

	// make "Set options" buttons magically stay where they are
	var $opts = $('#options-buttons');
	$opts.width( $opts.width() ); //absolutify width
	$opts.affix();

	var $optsButton = $('#set-options-button');
	var $optsModal = $('#set-options-modal');
	var $optsModalBody = $optsModal.find('.modal-body');
	var optsUrl = $optsButton.attr('href');

	// eagerly pre-load the options form into the modal.
	$optsModalBody.load(optsUrl, function(){
		Courses.decorateSubmissionsForm();
	});

	$optsButton.click(function(e){
		e.preventDefault();
		$optsModal.modal();
		return false;
	});

	// sets the options ID for all the checked assignments so that they will
	// use this set of options.
	var applyGroupNameToSelected = function(groupName) {
		var $label = $('<span>').addClass('label').addClass('label-'+groupName).html(groupName);
		$(".collection-checkbox:checked").closest('tr')
			.find('.options-id-input').val(groupName).end()
			.find('.options-id-label').html($label).end();
	};

	var $datesModal = $('#set-dates-modal');
	// open dates modal
	$('#set-dates-button').click(function(){
		$datesModal.modal();
		return false;
	});
	// set dates
	$datesModal.find('.modal-footer .btn-primary').click(function(e){
		var openDate = $('#modal-open-date').val();
		var closeDate = $('#modal-close-date').val();
		var $selectedRows = $('#batch-add-table tr.selected');
		$selectedRows.find('.open-date-field').val(openDate);
		$selectedRows.find('.close-date-field').val(closeDate);
		$selectedRows.find('.dates-label').html(openDate +' - ' + closeDate);
	});

	// complicated handling for when we submit the options modal...
	// if response contains .ajax-response[data-status=success] then validation succeeded,
	// and we copy all the form fields out into the main page form to be submitted.
	$optsModal.find('.modal-footer .btn-primary').click(function(e){
		$.post(optsUrl, $optsModalBody.find('form').serialize(), function(data){
			$optsModalBody.html(data);
			Courses.decorateSubmissionsForm();
			if ($optsModalBody.find('.ajax-response').data('status') == 'success') { // passed validation
				// grab all the submittable fields and clone them to the main page form
				var fields = $optsModalBody.find('[name]').clone();

				// Generate group names alphabetically from A, continuing later with B, and then C, and so on until
				// Z. Nobody knows what happens after Z...
				var groupName = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.charAt(optionGroupCount);
				var $groupNameLabel = $('<span>').addClass('label').addClass('label-'+groupName).html(groupName);
				optionGroupCount = optionGroupCount + 1;

				var $group = $('<div>').addClass('options-button');
				var $hidden = $('<div>').addClass('options-group').data('group', groupName);
				var $button = $('<button class="btn btn-block"></button>').html('Re-use options ').append($groupNameLabel);
				$button.data('groupName', groupName);
				$group.append($button);
				$group.append($hidden);

				//re-apply options to more items.
				$button.click(function() {
					applyGroupNameToSelected($(this).data('groupName'));
					return false;
				});

				fields.each(function(i, field){
					field.name = "optionsMap["+groupName+"]." + field.name;
					$hidden.append(field);
				});

				$opts.append($group);
				$optsModal.modal('hide');

				applyGroupNameToSelected(groupName);
			}
		});
		e.preventDefault();
		return false;
	});

});
//]]>
</script>

</#escape>