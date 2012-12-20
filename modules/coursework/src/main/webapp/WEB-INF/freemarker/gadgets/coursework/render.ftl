<h2>
<#if user.loggedIn>
	Hello, ${user.fullName!}.
<#else>
	Hello, stranger.
</#if>
</h2>

<p>
This is a test gadget. If you can see your name above it means that the gadget has 
successfully made an authenticated OAuth request back to Tabula.
</p>

<p>
Good times!
</p>