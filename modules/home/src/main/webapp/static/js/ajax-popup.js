/**
 * Enhances links with "ajax-popup" class by turning them magically
 * into AJAX-driven popup dialogs, without needing much special magic
 * on the link itself.
 *
 * "data-popup-target" attribute can be a CSS selector for a parent
 * element for the popup to point at.
 */
jQuery(function($){ "use strict";

var ajaxPopup = new WPopupBox();

$('a.ajax-popup').click(function(e){
    var link = e.target,
        $link = $(link),
        popup = ajaxPopup,
        width = 300,
        height = 300,
        pointAt = link;

    if ($link.data('popup-target') != null) {
        pointAt = $link.closest($link.data('popup-target'));
    }

    var decorate = function($root) {
        if ($root.find('html').length > 0) {
            // dragged in a whole document from somewhere, whoops. Replace it
            // rather than break the page layout
            popup.setContent("<p>Sorry, there was a problem loading this popup.</p>");
        }
        $root.find('.cancel-link').click(function(e){
            e.preventDefault();
            popup.hide();
        });
        $root.find('input[type=submit]').click(function(e){
            e.preventDefault();
            var $form = $(this).closest('form');
            
            // added ZLJ Aug 10th to make a unique URL which IE8 will load
            var $randomNumber = Math.floor(Math.random() * 10000000);
            
            // this line doesn't work in IE8 (IE8 bug) - need to make the URL
            // unique using generated random number
            //jQuery.post($form.attr('action'), $form.serialize(), function(data){
            jQuery.post($form.attr('action') + "?rand=" + $randomNumber, $form.serialize(), function(data){
                popup.setContent(data);
                decorate($(popup.contentElement));
            });
        });
        // If page returns hint that we're done, close the popup.
        if ($root.find('.ajax-response').data('status') == 'success') {
            //popup.hide();
            // For now, reload the page. Might extend it to allow selective reloading
            // of just the thing that's changed.
            window.location.reload();
        }
    };

    e.preventDefault();
    popup.setContent("Loading&hellip;");
    popup.setSize(width, height);
    $.get(link.href, function (data) {
        popup.setContent(data);
        var $contentElement = $(popup.contentElement);
        decorate($contentElement);
        popup.setSize(width, height);
        popup.show();
        popup.increaseHeightToFit();
        popup.positionLeft(pointAt);
    }, 'html');
});

});
