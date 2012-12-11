<#assign sso=JspTaglibs["/WEB-INF/tld/sso.tld"]>
<h1>Sorry</h1>

<#if (user.loggedIn)!false>
<p>Sorry<#if user??> ${user.firstName?default('')}</#if>, you don't have permission to see that.</p>
<#else>
<p>Sorry, you don't have permission to see that.
Try <a class="sso-link" href="<@sso.loginlink />">signing in</a>. 
</p>
</#if>