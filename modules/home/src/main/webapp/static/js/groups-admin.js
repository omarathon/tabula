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
});

}(jQuery));