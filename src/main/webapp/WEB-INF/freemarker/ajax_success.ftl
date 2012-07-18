<#-- 

What is this?

It is something a controller can return as a response to an AJAX request, when the
page is mainly working with HTML.

The script using AJAX can look for the matching element, and do the appropriate thing
(usually close a popup) if it is found.

    if ($element.find('.ajax-response').data('status') == 'success') {
      // close popup or whatever
    }

-->
<div class="ajax-response" data-status="success">
<#-- Some text just in case close doesn't work. -->
Successful. You can close this popup now.
</div>