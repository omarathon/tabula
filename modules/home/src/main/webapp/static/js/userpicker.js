// Lazily loaded user picker object.
(function ($) { "use strict";

var trim = $.trim;
var _userPicker = null;
function getUserPicker() {
    var targetWidth = 500, targetHeight=400;
    if (_userPicker == null) {

        // A popup box enhanced with a few methods for user picker
        _userPicker = new WPopupBox();
        
        _userPicker.isUniId = false;

        _userPicker.showPicker = function (element, targetInput) {
            this.setContent("Loading&hellip;");
            this.targetInput = targetInput;
            this.setSize(targetWidth,targetHeight);
            $.get('/api/userpicker/form', function (data) {
                _userPicker.setContent(data);
                _userPicker.setSize(targetWidth,targetHeight);
                _userPicker.show();
                _userPicker.positionRight(element);
                _userPicker.decorateForm();
            });
        };

        /* Give behaviour to user lookup form */
        _userPicker.decorateForm = function () {

            var $contents = $(this.contentElement),
                $firstname = $contents.find('.userpicker-firstname'),
                $lastname  = $contents.find('.userpicker-lastname'),
                $results = $contents.find('.userpicker-results'),
                $xhr,
                onResultsLoaded;

            $firstname.focus();
            $contents.find('input').delayedObserver(function () {
                // WSOS will search with at least 2 chars, but let's
                // enforce at least 3 to avoid overly broad searches.
                if (trim($firstname.val()).length > 2 ||
                        trim($lastname.val()).length > 2) {
                    $results.html('Loading&hellip;');
                    if ($xhr) $xhr.abort();
                    $xhr = jQuery.get('/api/userpicker/query',  {
                        firstName: $firstname.val(),
                        lastName: $lastname.val(),
                        isUniId: _userPicker.isUniId
                    }, onResultsLoaded);
                }
            }, 0.5);

            // wire up each user Id to be clickable
            onResultsLoaded = function(data) {
                $results.html(data);
                $results.find('td.user-id').click(function(){
                    var userId = this.innerHTML;
                    _userPicker.targetInput.value = userId;
                    _userPicker.hide();
                });
            }

        };

    }
    return _userPicker;
}

function initUserPicker(picker, isUniId){
    var $picker = $(picker),
        $button = $('<a href="#" class="btn">Search for user</a>'),
        userPicker;
    $picker.after("&nbsp;");
    $picker.after($button);
    $button.click(function(event){
        // lazy load on click
        if (userPicker === undefined){
            userPicker = getUserPicker();
        }
        userPicker.isUniId = isUniId;
        event.preventDefault();
        userPicker.showPicker(picker, picker);
    });
}

// decorate all inputs with class user-code-picker.
$(function(){
    $('input.user-code-picker').each(function (i, picker) {
        initUserPicker(picker, false);
    });
});

}(jQuery));