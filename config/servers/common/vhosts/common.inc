## mod_deflate configuration
<IfModule mod_deflate.c>

AddOutputFilterByType DEFLATE text/html text/javascript text/css application/x-javascript

BrowserMatch ^Mozilla/4 gzip-only-text/html
BrowserMatch ^Mozilla/4\.0[678] no-gzip

BrowserMatch ^Lynx no-gzip

BrowserMatch \bMSI[E] !no-gzip !gzip-only-text/html

# Avoid ANY possible trouble for IE6 by turning off compression
BrowserMatch \bMSI[E]\s6 no-gzip

# Tell proxies to store separate cached copies per user-agent, 
# so we don't send gzipped stuff to someone 
Header append Vary User-Agent env=!dont-vary

</IfModule>

<IfModule mod_headers.c>
Header unset X-Powered-By
Header set Server Warwick
</IfModule> 