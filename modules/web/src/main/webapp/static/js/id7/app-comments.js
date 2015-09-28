/**
 * Form for giving comments/feedback about the app itself
 */
(function($){ "use strict";

var fillInAppComments = function($form) {
    BrowserDetect.init();
    $form.find('#app-comment-os').val(BrowserDetect.OS);
    $form.find('#app-comment-browser').val(BrowserDetect.browser + ' ' + BrowserDetect.version);
    $form.find('#app-comment-resolution').val(BrowserDetect.resolution);
    var $currentPage = $form.find('#app-comment-currentPage');
    if ($currentPage.val() == '')
        $currentPage.val(window.location.href);
    BrowserDetect.findIP(function(ip){
        $form.find('#app-comment-ipAddress').val(ip);
    });
};

$(function(){

$('#app-feedback-link').click(function(event){
    event.preventDefault();
    var modal = $('#app-comment-modal'), modalBody = modal.find('.modal-body');
    var formLoaded = function(contentElement) {
        var $form = jQuery(contentElement).find('form');
        $form.submit(function(event){
            event.preventDefault();
            jQuery.post('/app/tell-us', $form.serialize(), function(data){
				modalBody.html(data);
                formLoaded(contentElement);
            });
        });
    };
    var formFirstLoaded = function(contentElement) {
        formLoaded(contentElement);
        fillInAppComments($(contentElement).find('form'));
    };

	$.get($(this).attr('href'), function(data){
		modalBody.html(data);
		formFirstLoaded(modalBody);
		modal.modal('show');
	});

});

});

})(jQuery);
