<#escape x as x?html>

<h1>
<#if user.loggedIn && user.firstName??>
Hello ${user.firstName}
<#else>
Hello
</#if>
</h1>

<p>This is the in-development coursework submission application.
	It isn't quite ready for use yet, but you can keep up with news about by
	going to <a href="http://go.warwick.ac.uk/amupdates">go.warwick.ac.uk/amupdates</a>.</p>

<#if moduleWebgroups?size gt 0>
</#if>
<#list moduleWebgroups as pair>
<div>
${pair._1}
</div>
</#list>

</#escape>