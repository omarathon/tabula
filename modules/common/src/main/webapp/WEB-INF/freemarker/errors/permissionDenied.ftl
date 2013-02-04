<#assign sso=JspTaglibs["/WEB-INF/tld/sso.tld"]>
<h1>Sorry</h1>

<#if (user.loggedIn)!false>
<p>Sorry<#if user??> ${user.firstName?default('')}</#if>, you don't have permission to see that.</p>

<#if user.sysadmin>
	<div class="alert alert-info sysadmin-only-content" style="margin-top: 2em;">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		
		<h4>Permissions helper</h4>
		
		<p>This is only shown to Tabula system administrators. Click the &times; button to see the page as a non-administrator sees it.</p>
	
		<@f.form method="post" action="${url('/sysadmin/permissions-helper', '/')}">
			<input type="hidden" name="user" value="${originalException.user.userId}" />
			
			<#if originalException.scope??>
				<input type="hidden" name="scopeType" value="${originalException.scope.class.name}" />
				<input type="hidden" name="scope" value="${originalException.scope}" />
			</#if>
				
			<input type="hidden" name="permission" value="${originalException.permission.name}" />
		
			<button class="btn btn-large" type="submit">Why can't I see this?</button>
		</@f.form>
	</div>
</#if>
<#else>
<p>Sorry, you don't have permission to see that.
Try <a class="sso-link" href="<@sso.loginlink />">signing in</a>. 
</p>
</#if>