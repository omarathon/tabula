<#--

This is included inline in batch_new_select.ftl.

It would probably be better as an external js file. Probably!

-->

jQuery(function($){


	// editable name field
	$('.editable-name').editable({
		toggle: '<a class="name-edit-link"><i class="icon-pencil"></i></a>',
		validate: function(value) {
		  if ($.trim(value) == '') {
		    return "A name is required.";
		  }
		}
	}).on('update', function(){
		// set the hidden field to the new value.
		var newVal = $(this).data('editable').value;
		$(this).siblings('input[type=hidden]').val( newVal );
	});
	
	

	var optionGroupCount = $('#options-buttons .options-button').length;

	var $form = $('#batch-add-form');
	
	// reload page when academic field dropdown changes, as it changes the contents of the list.
	$('#academicYearSelect').change(function(){
		$form.find('input[name=action]').val('refresh-select');
		$form.submit();
	}); 

	// When clicking Next, set the action parameter to the relevant value before submitting
	$form.find('button[data-action]').click(function(event){
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
			$('#set-options-button, #set-dates-button').removeClass('disabled');
		},

		onNoneChecked : function() {
			$('#set-options-button, #set-dates-button').addClass('disabled');
			$('#selected-count').text("0 selected");
		}
	});

	// cool selection mechanism...
	var batchTableMouseDown = false;
	$('#batch-add-table')
		.on('mousedown', 'td.selectable', function(){
			batchTableMouseDown = true;
			var $row = $(this).closest('tr');
			$row.toggleClass('selected');
			var checked = $row.hasClass('selected');
			$row.find('.collection-checkbox').attr('checked', checked);
			return false;
		})
		.on('mouseenter', 'td.selectable', function(){
			console.log('mouseenter');
			if (batchTableMouseDown) {
				var $row = $(this).closest('tr');
				$row.toggleClass('selected');
				var checked = $row.hasClass('selected');
				$row.find('.collection-checkbox').attr('checked', checked);
			}
		})
		.on('mousedown', 'a.name-edit-link', function(e){
			// prevent td.selected toggling when clicking the edit link. 
			e.stopPropogation(); 
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
		if (!$(this).hasClass('disabled')) {
			$optsModal.modal();
		}
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
		if (!$(this).hasClass('disabled')) {
			$datesModal.modal();
		}
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
		$datesModal.modal('hide');
	});
	
	// Wire all "Re-use options" buttons, present and future
	$('#options-buttons').on('click', '.options-button > button', function() {
		applyGroupNameToSelected($(this).data('group'));
		return false;
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
				$button.data('group', groupName);
				$group.append($button);
				$group.append($hidden);

				// button behaviour already wired by an on() call.

				// rename all the fields to sit under an optionsMap entry.
				fields.each(function(i, field){
					var prefix = "optionsMap["+groupName+"].";
					// HFC-306 if it starts with _, keep that at the start after renaming
					if (field.name.indexOf('_') == 0) {
						field.name = field.name.substring(1);
						prefix = "_" + prefix;
					}
					field.name =  prefix + field.name;
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