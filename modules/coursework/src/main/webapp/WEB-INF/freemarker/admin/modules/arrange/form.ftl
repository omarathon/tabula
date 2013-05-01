<#assign department=sortModulesCommand.department />
<#escape x as x?html>

<#if saved??>
<div class="alert alert-success">
<a class="close" data-dismiss="alert">&times;</a>
<p>Changes saved.</p>
</div>
</#if>

<h1>Arrange modules for ${department.name}</h1>

<noscript>
<div class="alert">This page requires Javascript.</div>
</noscript>

<p>Drag modules by their <i class='icon-move'></i> handles to move them between departments. To select multiple departments,
drag a box from one module name to another. You can also hold the <kbd>Ctrl</kbd> key to add to a selection.</p>

<@f.form commandName="sortModulesCommand" action="/coursework/admin/department/${department.code}/sort-modules">
	
	<#macro mods department modules>
		<h1>${department.name}</h1>
		<ul class="draggable-module-codes full-width" data-deptcode="${department.code}">
		<#list modules as module>
			<li class="label label-info" title="${module.name}">
				${module.code?upper_case}
				<input type="hidden" name="mapping[${department.code}][${module_index}]" value="${module.id}" />
			</li>
		</#list>
		</ul>
	</#macro>
	
	<#list sortModulesCommand.departments as dept> 
		<@mods dept sortModulesCommand.mappingByCode[dept.code]![] />
	</#list>
	
	<#-- this form has custom submit JS handling. -->
	<input id="sort-modules-submit" class="btn btn-primary" type="submit" value="Save changes" />

</@f.form>

<script>
// FIXME move externally
// TODO refactor to make as much as possible reusable for other pages that need multi-select dragndrop.
jQuery(function($){

	var first_rows = {};

	$('.draggable-module-codes')
		.sortable({
			connectWith: '.draggable-module-codes',
			handle: '.handle',
			placeholder: 'ui-state-highlight',
			forcePlaceholderSize: true,
			
			// helper returns the HTML item that follows the mouse
			helper: function(event, element) {
			  var $element = $(element)
			  var multidrag = $element.hasClass('ui-selected');
			  var msg = $element.text();
			  if (multidrag) msg = $('.ui-selected').length + " items"
			  return $('<div>').addClass('label').addClass('multiple-items-drag-placeholder').html(msg);
			},

			// we have just started dragging a dragger - add some classes to things
			start : function(event, ui) {
				if (ui.item.hasClass('ui-selected') && $('.ui-selected').length > 1) {
					first_rows = $('.ui-selected').map(function(i, e) {
						var $tr = $(e);
						return {
							tr : $tr.clone(true),
							id : $tr.attr('id')
						};
					}).get();
					$('.ui-selected').addClass('cloned');
				}
				//ui.placeholder.html('');
			},
			
			// dropped items, put them where they want to be
			stop : function(event, ui) {
				if (first_rows.length > 1) {
					$.each(first_rows, function(i, item) {
						$(item.tr)
							.removeAttr('style')
							.removeClass('ui-selected')
							.insertBefore(ui.item);
					});
					$('.cloned').remove();
					first_rows = {};
				}
				renameAllFields();
			}

		})
		.selectable({
			filter: 'li',
			cancel: '.handle'
		})
		.find('li')
			.addClass('ui-corner-all')
			.prepend("<span class='handle'><i class='icon-white icon-move'></i> </span>")
			.end()
		.find('.label')
			.tooltip({ delay: {show:500, hide:0}, container: 'body' });

	var renameAllFields = function() {
		$('.draggable-module-codes').each(function(i, deptBox) {
			var deptCode = $(deptBox).data('deptcode');
			renameFields(deptCode, $(deptBox).find('input[type=hidden]'));
		});
	};
	
	// Rename all form input for this department to represent the ordered list
	var renameFields = function(deptCode, $fields) {
		$fields.each(function(i, field) {
			field.name = fieldNameFor(deptCode, i);
		});
	}
	// Get form field name for this item.
	var fieldNameFor = function(deptCode, index) {
		return "mapping["+deptCode+"]["+index+"]";
	}

});
</script>

<style>
/* FIXME move externally */
ul.draggable-module-codes { 
	padding-top: 5px;
	margin-top: 0;
	margin-bottom: 1em; 
	min-height: 2em; 
	background-color: #ddd; 
	overflow: auto;
	margin-left: -16px;
	margin-right: -16px;
}
ul.draggable-module-codes li,
ul.draggable-module-codes .ui-state-highlight { 
	display:inline-block; 
	float: left; 
	margin: 1px 3px; 
	width: 62px;
}
ul.draggable-module-codes .cloned { opacity: 0.5; }

.multiple-items-drag-placeholder { margin-left: 16px; }

.ui-selecting { background: #333 !important; }
.ui-selected { background: #000 !important; }
.ui-state-highlight { background: gold !important; padding: 2px 4px; }

</style>

</#escape> 