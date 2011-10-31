<#escape x as x?html>

<#if user.loggedIn>
<p>
Hello ${user.firstName!'unnamed person'}.
</p>
<#else>
<p>Hello Dave.</p>
</#if>

<#if moduleWebgroups?size gt 0>
</#if>
<#list moduleWebgroups as pair>
<div>
${pair._1}
</div>
</#list>

</#escape>