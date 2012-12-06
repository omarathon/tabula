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
	    });
	    $slidingDiv.toggle($checkbox.is(':checked'));
	} else {
	    $checkbox.change(function(){
	        if ($checkbox.is(':checked'))
	        	$slidingDiv.stop().slideUp('fast');
	        else
	        	$slidingDiv.stop().slideDown('fast');
	    });
	    $slidingDiv.toggle(! $checkbox.is(':checked'));
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
        .find('h2').prepend($('<small>Click to expand</small>')).end()
        .click(function(){
            $(this).css('opacity',1)
                .find('h2 small').remove().end()
                .find('.module-info-contents').show().end();
        })
        .hide()
        .first().before(
            $('<p>').html('Modules with no assignments are hidden. ').append(
                $('<a>').addClass('btn btn-success').attr('href','#').html("Show all modules").click(function(event){
                    event.preventDefault();
                    $(this.parentNode).remove();
                    $('.module-info.empty').show();
                })
            )
        );
    
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
    
    $('.submission-feedback-list, .submission-list, .feedback-list').bigList({
    
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

            $('#release-submissions-button').click(function(event){
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
            $('.submission-feedback-list,.submission-list').data("all-plagiarised", allPlagiarised);
            if (allPlagiarised) {
                $('#mark-plagiarised-selected-button').text("Unmark selected plagiarised");
            }
            else {
                $('#mark-plagiarised-selected-button').text("Mark selected plagiarised");               
            }
        },
    
        onSomeChecked : function() {
            $('#modify-selected').removeClass('disabled');
        },
    
        onNoneChecked : function() {
            $('#modify-selected').addClass('disabled');
        }
    
    });
    
});

// take anything we've attached to "exports" and add it to the global "Courses"
// we use extend() to add to any existing variable rather than clobber it
window.Courses = jQuery.extend(window.Courses, exports);


}(jQuery));