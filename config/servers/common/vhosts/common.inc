## mod_deflate configuration
<IfModule mod_deflate.c>

AddOutputFilterByType DEFLATE text/html text/javascript text/css application/x-javascript application/javascript

# Netscape 4.x has some problems...
BrowserMatch ^Mozilla/4 gzip-only-text/html
# Netscape 4.06-4.08 have some more problems
BrowserMatch ^Mozilla/4\.0[678] no-gzip

# Lynx works but it stops with notices telling you what it's
# gzipping. Thanks a lot!!!
# BrowserMatch ^Lynx no-gzip

# MSIE masquerades as Netscape, but it is fine
# NOTE: Due to a bug in mod_setenvif up to Apache 2.0.48
# the "[E]" is required. I think we have 2.2, but no harm done.
BrowserMatch \bMSI[E] !no-gzip !gzip-only-text/html

# Default for IE6 is no compression
BrowserMatch \bMSI[E]\s6 no-gzip
# But turn it back on if you have the service pack, gzip is fine now [SBTWO-3784]
BrowserMatch \bMSI[E]\s6.+SV1 !no-gzip

SetEnvIf Request_URI "^/$" no-gzip
SetEnvIf Request_URI "^/[^s][^t][^a][^t]" no-gzip

# URL patterns to not send Vary headers. Don't vary on URLs that can never be the type you want
SetEnvIf Request_URI "^/$" dont-vary
SetEnvIf Request_URI "^/[^s][^t][^a][^t]" dont-vary
SetEnvIf Request_URI "^/static/(.+\.(jpg|jpeg|jpe|png|gif|bmp|zip|gz|mp4|m4v|flv|wav|ogg|ogv|webm|mp3|m4a|aac|ico|swf))(\.[0-9]+)?$" dont-vary

# Tell proxies to store separate cached copies per user-agent, 
# so we don't send gzipped stuff to someone 
Header append Vary User-Agent env=!dont-vary

</IfModule>

<IfModule mod_headers.c>
Header unset X-Powered-By
Header set Server Warwick
</IfModule> 