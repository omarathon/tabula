<#--
 Don't forget to add a profile modal container when you use this macro if one doesn't exist on the page already
 <div id="profile-modal" class="modal fade profile-subset"></div>
 -->
<#macro profile_link user_id>
  <a class="ajax-modal profile-link" href="/profiles/view/subset/${user_id}" data-target="#profile-modal"><#--
    --><i class="icon-info-sign fal fa-info-circle"></i><span class="sr-only">View profile</span><#--
  --></a>
</#macro>