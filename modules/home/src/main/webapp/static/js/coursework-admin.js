/**
 * Scripts used only by the coursework admin section. 
 */
(function ($) { "use strict";

var exports = {};

var slideMoreOptions = function($checkbox, $slidingDiv, showWhenChecked) {
	if (showWhenChecked) {
	    $checkbox.change(function(){
	        if ($checkbox.is(':checked'))
	        	$slidingDiv.stop().slideDown('fast');
	        else
	        	$slidingDiv.stop().slideUp('fast');
	    }).trigger('change');
	} else {
	    $checkbox.change(function(){
	        if ($checkbox.is(':checked'))
	        	$slidingDiv.stop().slideUp('fast');
	        else
	        	$slidingDiv.stop().slideDown('fast');
	    }).trigger('change');
	}
};

// export the stuff we do to the submissions form so we can re-run it on demand.
var decorateSubmissionsForm = function() {
    slideMoreOptions($('input#collectSubmissions'), $('#submission-options'), true);
};
exports.decorateSubmissionsForm = decorateSubmissionsForm;

$(function(){

    decorateSubmissionsForm();
    
    // hide stuff that makes no sense when open-ended
    slideMoreOptions($('input#openEnded'), $('.has-close-date'), false);
    slideMoreOptions($('input#modal-open-ended'), $('.has-close-date'), false);

    // check that the extension UI elements are present
    if($('input#allowExtensionRequests').length > 0){
        slideMoreOptions($('input#allowExtensionRequests'), $('#request-extension-fields'), true);
    }
    
    // Zebra striping on lists of modules/assignments
    $('.module-info').each(function(i, module) { 
        $(module).find('.assignment-info').filter(':visible:even').addClass('alt-row');
    });
    
    $('.module-info.empty').css('opacity',0.66)
        .find('.module-info-contents').hide().end()
        .click(function(){
            $(this).css('opacity',1)
                .find('.module-info-contents').show().end();
        })
        .hide();
        
    $('.dept-show').click(function(event){
    	event.preventDefault();   	
    	var hideButton = $(this).find("a");
    	
        $('.module-info.empty').toggle('fast', function() {
        	if($('.module-info.empty').is(":visible")) {
        		hideButton.html('<i class="icon-eye-close"></i> Hide');
        		hideButton.attr("data-original-title", hideButton.attr("data-title-hide"));
        		
        	} else { 
        		hideButton.html('<i class="icon-eye-open"></i> Show');
        		hideButton.attr("data-original-title", hideButton.attr("data-title-show"));
        	}
        });

    });
        
    
    // code for tabs
    $('.nav.nav-tabs a').click(function (e) {
        e.preventDefault();
        $(this).tab('show');
    });
    
    // code for the marks web forms
    $('#marks-web-form').tableForm({
        addButtonClass: 'add-additional-marks',
        headerClass: 'mark-header',
        rowClass: 'mark-row',
        tableClass: 'marksUploadTable',
        listVariable: 'marks',
        onAdd: function(){
            $('input.universityId', this).each(function(i, picker){
                initUserPicker(picker, true);
            });
        }
    });
    
    $('.show-archived-assignments').click(function(e){
        e.preventDefault();
        $(e.target).hide().closest('.module-info').find('.assignment-info.archived').show();
    });

	$('.hide-awaiting-submission').on('click', function(){
        $('.awaiting-submission, .hide-label, .show-label').toggle();
    });
	// hide this button if it would do nothing
	if ($('.awaiting-submission').length == 0) {
		$('.hide-awaiting-submission').hide();
	}
    
    // enable shift-click on multiple checkboxes in tables
    $('table').find('input[type="checkbox"]').shiftSelectable();
    
    $('.submission-feedback-list, .submission-list, .feedback-list, .marker-feedback-list, #coursework-progress-table').bigList({
        setup : function() {
            var $container = this;
            // #delete-selected-button won't work for >1 set of checkboxes on a page.
            $('#download-selected-button, #delete-selected-button').click(function(event){
                event.preventDefault();
    
                var $checkedBoxes = $(".collection-checkbox:checked", $container);
                if ($container.data('checked') != 'none') {
                    var $form = $('<form></form>').attr({method:'POST',action:this.href}).hide();
                    $form.append($checkedBoxes.clone());
                    $(document.body).append($form);
                    $form.submit();
                }
                return false;
            });
            
            $('#mark-plagiarised-selected-button').click(function(event){
                event.preventDefault();
    
                var $checkedBoxes = $(".collection-checkbox:checked", $container);
                
                if ($container.data('checked') != 'none') {
        
                    var $form = $('<form></form>').attr({method:'POST',action:this.href}).hide();
                    $form.append($checkedBoxes.clone());
                    
                    if ($container.data("all-plagiarised") === true) {
                        $form.append("<input type='hidden' name='markPlagiarised' value='false'>");
                    }
                    
                    $(document.body).append($form);
                    $form.submit();
                }
                return false;
            });

            $('.form-post').click(function(event){
                event.preventDefault();
                
                var $form = $('<form></form>').attr({method:'POST',action:this.href}).hide();
                var doFormSubmit = false;                
                
                if ($container.data('checked') != 'none') {
                    var $checkedBoxes = $(".collection-checkbox:checked", $container);
                    $form.append($checkedBoxes.clone());
                    
                    doFormSubmit = true;
                }
                                
                if ($(this).hasClass('include-filter') && ($('.filter-form').length > 0)) {
                		var $inputs = $(':input', '.filter-form');
                		$form.append($inputs.clone());
                
                		doFormSubmit = true;
                }
                               
                if (doFormSubmit) {
                	$(document.body).append($form);
                  $form.submit();
                } else {
                	return false;
                }
            });
            
        },
    
        // rather than just toggling the class check the state of the checkbox to avoid silly errors
        onChange : function() {
            this.closest(".itemContainer").toggleClass("selected", this.is(":checked"));
            var $checkedBoxes = $(".collection-checkbox:checked");
    
            var allPlagiarised = false;
            
            if ($checkedBoxes.length > 0) {
                allPlagiarised = true;
                $checkedBoxes.each(function(index){
                    var $checkBox = $(this);
                    if ($checkBox.closest('tr').data('plagiarised') != true) {
                        allPlagiarised = false;
                    }
                });                 
            }
            $('.submission-feedback-list,.submission-list,#coursework-progress-table').data("all-plagiarised", allPlagiarised);
            if (allPlagiarised) {
                $('#mark-plagiarised-selected-button').html('<i class="icon-exclamation-sign"></i> Unmark plagiarised');
            }
            else {
                $('#mark-plagiarised-selected-button').html('<i class="icon-exclamation-sign"></i> Mark plagiarised');               
            }
        },
    
        onSomeChecked : function() {
            $('.must-have-selected').removeClass('disabled');
        },
    
        onNoneChecked : function() {
            $('.must-have-selected').addClass('disabled');
        }
    
    });
    
    
    
});

// take anything we've attached to "exports" and add it to the global "Courses"
// we use extend() to add to any existing variable rather than clobber it
window.Courses = jQuery.extend(window.Courses, exports);


// shift selectable checkboxes
$.fn.shiftSelectable = function() {
    var lastChecked,
        $boxes = this;
 
    $boxes.click(function(evt) {
        if(!lastChecked) {
            lastChecked = this;
            return;
        }
 
        if(evt.shiftKey) {
            var start = $boxes.index(this),
                end = $boxes.index(lastChecked);
            $boxes.slice(Math.min(start, end), Math.max(start, end) + 1)
                .attr('checked', lastChecked.checked)
                .trigger('change');
        }
 
        lastChecked = this;
    });
};

// assign markers javascript
$(function(){

	var draggableOptions = {
		containment: "#assign-markers",
		scroll: false,
		revert: true,
		helper: 'clone',
		zIndex: 350,
		start: function() { $(this).toggle(); },
		stop: function() { $(this).toggle(); }
	};

	var addStudent = function(student, marker){
		var markerId = marker.data("marker-id");
		var studentId = student.data("student-id");
		var studentDisplay = student.data("student-display");
		var studentIndex = jQuery('.student-container input', marker).length;
		var formElement = jQuery('<input type="hidden" />');
		formElement.attr('name', 'markerMapping['+markerId+']['+studentIndex+']');
		formElement.attr('value', studentId);
		jQuery('.student-container', marker).append(formElement);
		var studentElement = jQuery('<li>'+studentDisplay+' </li>');
		var button = jQuery('<a href="#" class="remove-student btn btn-mini"><i class="icon-remove"></i> Remove</a>');
		button.attr("data-marker-id", markerId);
		button.attr("data-student-id", studentId);
		button.attr("data-student-display", studentDisplay);
		studentElement.append(button);
		jQuery('.student-list', marker).append(studentElement);
	}

	var removeStudent = function(studentDisplay, studentId, tab){
		var studentElement = jQuery('<div class="student ui-draggable"><i class="icon-user"></i> '+studentDisplay+'</div>');
		studentElement.attr('data-student-id', studentId);
		studentElement.attr('data-student-display', studentDisplay);
		var listElement = jQuery('<li class="hide"></li>') ;
		listElement.append(studentElement);
		$('.students .member-list', tab).append(listElement);
		studentElement.draggable(draggableOptions);
		return listElement;
	};

	$("#assign-markers .student").draggable(draggableOptions);

	$("#assign-markers .marker").droppable({
		hoverClass: "drop-hover",
		drop: function( event, ui ) {
			var marker = $(this);
			var countBadge = marker.find(".count");
			countBadge.html(parseInt(countBadge.html()) + 1);
			var student = ui.draggable;
			addStudent(student, marker);
			student.hide("scale", {percent: 0}, 250);
			student.closest('li').remove();
		}
	});

	$("#assign-markers .first-markers, #assign-markers .second-markers").on("click", ".remove-student", function(e){
		var studentId = $(this).data("student-id");
		var studentDisplay = $(this).data("student-display");
		var markerId = $(this).data("marker-id");
		var marker = jQuery('#container-'+markerId).closest('.marker');

		jQuery('li:contains('+studentDisplay+')', marker).remove();
		jQuery('input[value="'+studentId+'"]', marker).remove();

		var countBadge = marker.find(".count");
		countBadge.html(parseInt(countBadge.html()) - 1);
		var tab =marker.closest(".tab-pane");
		removeStudent(studentDisplay, studentId, tab).show('scale', {percent: 100}, 250);

		e.preventDefault();
		return false;
	});

	$("#first-markers, #second-markers").on("click", ".random", function(e){
		var tab = $(this).closest(".tab-pane");
		// shuffle the students
		var students = $(".student", tab).sort(function(){
			return Math.random() > 0.5 ? 1 : -1;
		});

		var markers = $(".marker", tab);
		var studentPerMarker =  Math.floor(students.length / markers.length);
        var remainder = students.slice(students.length - (students.length % markers.length));
		markers.each(function(index){
			var marker = $(this);
			var from = (index*studentPerMarker);
			var to = ((index+1)*studentPerMarker);
			var studentsForMarker = students.slice(from, to);
			// if there are remainder student left add one to this marker
			if(remainder.length > 0)
				studentsForMarker = studentsForMarker.add(remainder.splice(0,1));
			var countBadge = $(".count", marker);
			countBadge.html(parseInt(countBadge.html()) + studentsForMarker.length);
			studentsForMarker.each(function(){
				addStudent($(this), marker);
				$(this).closest('li').remove();
			});
		});

		e.preventDefault();
		return false;
	});

	$("#first-markers, #second-markers").on("click", ".remove-all", function(e){
		var tab = $(this).closest(".tab-pane");
		$('.student-container input',tab).each(function(){
			var studentId = $(this).val();
			var studentDisplay = $(this).data("student-display");
			$(this).remove();
			removeStudent(studentDisplay, studentId, tab).show();
		});
		$('.student-container .student-list',tab).html("");
		$('.count',tab).html("0");

		e.preventDefault();
		return false;
	});
});

// code for markingWorkflow add/edit
$(function(){
	var markingMethod = $('#markingMethod');
	var secondMarkers = $('.second-markers-container');
	if($('option:selected', markingMethod).hasClass('uses-second-markers'))
		secondMarkers.show();
	$('#markingMethod').on('change', function(){
		var option = $('option:selected', $(this));
		if(option.hasClass('uses-second-markers'))
			secondMarkers.show();
		else
			secondMarkers.hide();
	});
});

$(function(){
	$('a.disabled').on('click', function(e){
		e.preventDefault();
	});
});





var frameLoad = function(frame){
    if(jQuery(frame).contents().find("form").length == 0){
        jQuery("#feedback-report-modal").modal('hide');
        document.location.reload(true);
    }
}

$(function(){




// Ajax specific modal start

// modals use ajax to retrieve their contents
    $('#feedback-report-button, #extension-list').on('click', 'a[data-toggle=modal]', function(e){
        e.preventDefault();
        var $this = $(this);
        var target = $this.attr('data-target');
        var url = $this.attr('href');
        $(target).load(url);
    });



// any date fields returned by ajax will have datetime pickers bound to them as required
    $('#feedback-report-modal, #extension-list').on('focus', 'input.date-time-picker', function(e){
        e.preventDefault();
        var isPickerHidden = (typeof $('.datetimepicker').filter(':visible')[0] === "undefined") ? true : false;

        if(isPickerHidden) {
            $(this).datetimepicker('remove').datetimepicker({
                format: "dd-M-yyyy hh:ii:ss",
                weekStart: 1,
                minView: 'day',
                autoclose: true
            }).on('show', function(ev){
                var d = new Date(ev.date.valueOf()),
                      minutes = d.getUTCMinutes(),
                        seconds = d.getUTCSeconds(),
                        millis = d.getUTCMilliseconds();

                if (minutes > 0 || seconds > 0 || millis > 0) {
                    d.setUTCMinutes(0);
                    d.setUTCSeconds(0);
                    d.setUTCMilliseconds(0);

                    var DPGlobal = $.fn.datetimepicker.DPGlobal;
                    $(this).val(DPGlobal.formatDate(d, DPGlobal.parseFormat("dd-M-yyyy hh:ii:ss", "standard"), "en", "standard"));

                    $(this).datetimepicker('update');
                }
            });
        }
    });

    //$('#feedback-report-modal, #extension-list')
    var submitAjaxModal = function(selector, successCallback) {
         $(selector).on('submit', 'form', function(e){
                e.preventDefault();
                var $form = $(this);
                $.post($form.attr('action'), $form.serialize(), function(data){
                    if(data.status == "error"){
                        // delete any old errors
                        $("span.error").remove();
                        $('.error').removeClass('error');
                        var error;
                        for(error in data.result){
                            addError(error, data.result[error]);
                        }
                    } else {
                        successCallback(data)
                        // hide the model
                        jQuery("#extension-model").modal('hide');
                    }
                });
            });

    }

    var addError = function(field, message){
        var $field = $("input[name='"+field+"']");
        $field.closest(".control-group").addClass("error");
        // insert error message
        $field.after('<span class="error help-inline">'+message+'</span>');
    };


// Ajax specific modal end


    // feedback report

    submitAjaxModal("#feedback-report-modal", function(data) {
    	window.location = data.result;
    })

   // extensions admin

   submitAjaxModal("#extension-list", function(data) {
        var action = data.action;
        $.each(data.result, function() {
            modifyRow(this, action);
        });
    });

    var highlightId = "${highlightId}";
    if (highlightId != "") {
        var container = $("#extension-list");
        var highlightRow = $("#row"+highlightId);
        if(highlightRow.length > 0){
            container.animate({
                scrollTop: highlightRow.offset().top - container.offset().top + container.scrollTop()
            }, 1000);
        }
    }

    // set reject and approved flags
    $("#extension-model").on('click', '#approveButton', function(){
        $(".approveField").val("1");
        $(".rejectField").val("0");
    });

    $("#extension-model").on('click', '#rejectButton', function(){
        $(".approveField").val("0");
        $(".rejectField").val("1");
    });

    var modifyRow = function(results, action){
        var $row =  $("#extension-list").find("#row"+results.id);
        if(action === "add"){
            updateRowUI(results, $row);
            $(".new-extension", $row).hide();
            $(".modify-extension", $row).show();
            $(".revoke-extension", $row).show();
        } else if (action === "edit"){
            updateRowUI(results, $row);
        } else if (action === "delete"){
            $("td.expiryDate", $row).html("");
            $("td.status", $row).html("");
            $(".new-extension", $row).show();
            $(".modify-extension", $row).hide();
            $(".revoke-extension", $row).hide();
        }
    };

    var updateRowUI = function(json, $row){
    	var prop;
        for(prop in json){
            $row.find('.'+prop).html(json[prop]);
        }
        $('.status').each(function(){
            if ($(this).html() === "Approved")
                $(this).html('<span>Granted</span>');
            else if ($(this).html() === "Rejected")
                $(this).html('<span">Rejected</span>');
        });
    };


	// makes dropdown menus dropup rather than down if they're so
	// close to the end of the screen that they will drop off it
	var bodyHeight = $('body').height();
	$('.module-info:not(.empty) .btn-group').each( function(index) {
		if(($(this).find('.dropdown-menu').height() +  $(this).offset().top) > bodyHeight) {
			$(this).addClass("dropup");
		}
	});

});

$(function(){
	// Dragging modules between departments ("modules/arrange/form")
	$('#sortModulesCommand').dragAndDrop({});	
	// Dragging modules between departments end
});

}(jQuery));


