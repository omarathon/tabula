/**
 * Form for giving comments/feedback about the app itself
 */
(function($){ "use strict";


var _feedbackPopup;
var getFeedbackPopup = function() {
    if (!_feedbackPopup) {
        _feedbackPopup = new WPopupBox();
        _feedbackPopup.setSize(500,300);
    }
    return _feedbackPopup;
}

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

var decorateAppCommentsForm = function($form) {
    $form.addClass('narrowed-form');
};

$(function(){

// Fills in non-AJAX app comment form
$('#app-comment-form').each(function() {
    var $form = $(this);
    fillInAppComments($form);
    decorateAppCommentsForm($form);
});

$('#app-feedback-link').click(function(event){
    event.preventDefault();
    var popup = getFeedbackPopup();
    var target = event.target;
    var formLoaded = function(contentElement) {
        var $form = jQuery(contentElement).find('form');
        decorateAppCommentsForm($form);
        $form.submit(function(event){
            event.preventDefault();
            jQuery.post('/app/tell-us', $form.serialize(), function(data){
                popup.setContent(data);
                popup.positionRight(target);
                formLoaded(contentElement);
            });
        });
    };
    var formFirstLoaded = function(contentElement) {
        formLoaded(contentElement);
        fillInAppComments($(contentElement).find('form'));
    };

    if (popup.isShowing()) {
        popup.hide();
    } else {
        popup.showUrl($(this).find('a[href]').attr('href'), {
            method:'GET', target: target, position:'right',
            onComplete: formFirstLoaded
        });
    }

});

});

})(jQuery);
