/**
 * Scripts used only by the coursework admin section. 
 */
(function ($) { "use strict";

$(function(){
    
    // Zebra striping on lists of modules/groups
    $('.module-info').each(function(i, module) { 
        $(module).find('.group-info').filter(':visible:even').addClass('alt-row');
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
    	
        $('.striped-section.empty').toggle('fast', function() {
        	if($('.module-info.empty').is(":visible")) {
        		hideButton.html('<i class="icon-eye-close"></i> Hide');
        		hideButton.attr("data-original-title", hideButton.attr("data-title-hide"));
        		
        	} else { 
        		hideButton.html('<i class="icon-eye-open"></i> Show');
        		hideButton.attr("data-original-title", hideButton.attr("data-title-show"));
        	}
        });

    });
    
    $('.show-archived-small-groups').click(function(e){
        e.preventDefault();
        $(e.target).hide().closest('.striped-section').find('.item-info.archived').show();
    });

    // enable/disable the "sign up" buttons on the student groups homepage
    $('#student-groups-view .sign-up-button').addClass('disabled use-tooltip').prop('disabled',true).prop('title','Please select a group');
    $('#student-groups-view input.group-selection-radio').change(function(){
		$('.sign-up-button').removeClass('disabled use-tooltip').prop('disabled',false).prop('title','');
	});
});


// modals use ajax to retrieve their contents
$(function() {
    $('.btn-group').on('click', 'a[data-toggle=modal]', function(e){
        e.preventDefault();
        var $this = $(this);
        var target = $this.attr('data-target');
        var url = $this.attr('href');
        $(target).load(url);
    });

    $("#modal-container ").on("click","input[type='submit']", function(e){
        e.preventDefault();
        var $this = $(this);
        var $form = $this.closest("form")

        var randomNumber = Math.floor(Math.random() * 10000000);

        jQuery.post($form.attr('action') + "?rand=" + randomNumber, $form.serialize(), function(data){
            $("#modal-container ").modal('hide');
            window.location.reload();
        });
    });
});
// Drag and drop allocation
$(function() {
	$('#allocateStudentsToGroupsCommand')
		.dragAndDrop({
			itemName: 'student',
			textSelector: '.name h6',
			useHandle: false,
			selectables: '.students .drag-target',
			scroll: true,
			removeTooltip: 'Remove this student from this group'
		})
		.each(function(i, container) {
			var $container = $(container);
			$container.find('a.random').on('click', function(e) {			
				$container.dragAndDrop('randomise');
				
				e.preventDefault();
				e.stopPropagation();
				return false;
			});
		});
	
	if ($('#allocateStudentsToGroupsCommand .student-list').length) {
		// Manage button disabled-ness when there are items in/out the source and target lists
		var manageButtons = function() {
			var stillToAllocate = $('#allocateStudentsToGroupsCommand .student-list .drag-list li').length;
			
			if (stillToAllocate > 0) {
				$('#allocateStudentsToGroupsCommand a.random').removeClass('disabled');
			} else {
				$('#allocateStudentsToGroupsCommand a.random').addClass('disabled');
			}
			
			var alreadyAllocated = $('#allocateStudentsToGroupsCommand .groups .drag-list li').length;
			
			if (alreadyAllocated > 0) {
				$('#allocateStudentsToGroupsCommand a.return-items').removeClass('disabled');
			} else {
				$('#allocateStudentsToGroupsCommand a.return-items').addClass('disabled');
			}
		};
		
		$('#allocateStudentsToGroupsCommand .student-list').on('changed.tabula', manageButtons);
		manageButtons();
	}
});

}(jQuery));