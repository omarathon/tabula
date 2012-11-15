<#assign sso=JspTaglibs["/WEB-INF/tld/sso.tld"]>
<h1>Sorry</h1>

<#if user.loggedIn>
<p>Sorry ${user.firstName}, you don't have permission to see that.</p>
<#else>
<p>Sorry, you don't have permission to see that.
Try <a class="sso-link" href="<@sso.loginlink />">signing in</a>. 
</p>
</#if>