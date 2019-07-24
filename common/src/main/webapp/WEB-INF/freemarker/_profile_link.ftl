<#--
 Don't forget to add a profile modal container when you use this macro if one doesn't exist on the page already
 <@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
 -->
<#macro profile_link user_id>
  <a class="profile-link" target="_blank" href="/profiles/view/${user_id}"><#--
    --><i class="icon-info-sign fal fa-info-circle"></i><span class="sr-only">View profile</span><#--
  --></a>
</#macro>