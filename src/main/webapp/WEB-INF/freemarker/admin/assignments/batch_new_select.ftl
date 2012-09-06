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
	<th>Seq code</th>
	<th>Assignment name</th>
	<#if step="options">
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
	<td>
		${item.upstreamAssignment.moduleCode?upper_case}
	</td>
	<td>
		${item.upstreamAssignment.sequence}
	</td>
	<td>
		${item.upstreamAssignment.name}
	</td>
	<#if step="options">
 	<td class="assignment-editable-fields-cell">
 		<@f.input path="openDate" cssClass="date-time-pickfer" placeholder="Open date" />
 		<@f.input path="closeDate" cssClass="date-time-pickfer" placeholder="Close date" />
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
<#-- options sets -->
<a class="btn btn-warning btn-block" id="set-options-button" data-target="#set-options-modal" href="<@url page="/admin/department/${department.code}/shared-options"/>">
	Set options
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
</#if>

<script type="text/javascript">
//<[![CDATA[
jQuery(function($){



	var $form = $('#batch-add-form');
	$form.find('button[data-action=options]').click(function(event){
		var action = $(this).data('action');
		if (action) {
			$form.find('input[name=action]').val(action);
		}
	});

	$('#batch-add-table').bigList({
		setup : function() {
			var $container = this;

			$('#set-options-buftton').click(function(ev){
				//ev.preventDefault();

				var $checkedBoxes = $(".collection-checkbox:checked", $container);
				if ($container.data('checked') != 'none') {
					// open options dialog with the stuff all ready to roll.
					//alert('To be implemented');
				}

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

	// make "Set options" buttons magically stay where they are
	var $opts = $('#options-buttons');
	$opts.width( $opts.width() ); //absolutify width
	$opts.affix();

	var $optsButton = $('#set-options-button');
	var $optsModal = $('#set-options-modal');
	<#--$optsModal.find('.modal-body').load($optsButton.attr('href'), function(){

	});-->
	$optsModal.find('.modal-footer .btn-primary').click(function(e){
		e.preventDefault();
		alert('better submit this form eh');
		return false;
	});

});
//]]>
</script>

</#escape>