#!/bin/bash

read -p "Enter the fully qualified domain name of your local machine (e.g. localdev.warwick.ac.uk): " domain

if ! [[ $domain =~ .*\.warwick\.ac.\uk$ ]]
then
   echo "Domain must end with .warwick.ac.uk for SSO to work."
   exit 1
fi

read -p "Enter your email address (for Let's Encrypt to issue a certificate): " email

rsa_key_size=4096
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
data_path="${script_dir}/data/certbot"
nginx_data_path="${script_dir}/data/nginx"
staging=0 # Set to 1 if you're testing your setup to avoid hitting request limits

if [ -d "$data_path" ]; then
  read -p "Existing data found for $domain. Continue and replace existing certificate? (y/N) " decision
  if [ "$decision" != "Y" ] && [ "$decision" != "y" ]; then
    exit
  fi
fi

if [ ! -e "$data_path/conf/options-ssl-nginx.conf" ] || [ ! -e "$data_path/conf/ssl-dhparams.pem" ]; then
  echo "### Downloading recommended TLS parameters ..."
  mkdir -p "$data_path/conf"
  curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot-nginx/certbot_nginx/options-ssl-nginx.conf > "$data_path/conf/options-ssl-nginx.conf"
  curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot/ssl-dhparams.pem > "$data_path/conf/ssl-dhparams.pem"
  echo
fi

echo "### Creating dummy certificate for $domain ..."
path="/etc/letsencrypt/live/$domain"
mkdir -p "$data_path/conf/live/$domain"
sudo docker-compose run --rm --entrypoint "\
  openssl req -x509 -nodes -newkey rsa:1024 -days 1\
    -keyout '$path/privkey.pem' \
    -out '$path/fullchain.pem' \
    -subj '/CN=$domain'" certbot
echo

echo "### Writing nginx config ..."
mkdir -p $nginx_data_path
cat <<EOF > $nginx_data_path/app.conf
server {
    listen 80;
    server_name $domain;
    server_tokens off;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://\$host\$request_uri;
    }
}

server {
    listen 443 ssl http2;
    server_name $domain;
    server_tokens off;

    ssl_certificate /etc/letsencrypt/live/$domain/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/$domain/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    client_max_body_size 10G;

    location / {
				proxy_pass http://127.0.0.1:8080;
				proxy_set_header Host \$host;
				proxy_set_header X-Real-IP \$remote_addr;
				proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
				proxy_set_header X-Forwarded-Proto \$scheme;
				proxy_set_header X-Requested-Uri "\${scheme}://\${host}\${request_uri}";
				proxy_http_version 1.1;
				proxy_connect_timeout 300;
				proxy_send_timeout 300;
				proxy_read_timeout 300;
				send_timeout 300;
    }
}
EOF

echo "### Starting nginx ..."
sudo docker-compose up --force-recreate -d nginx
echo

echo "### Deleting dummy certificate for $domain ..."
sudo docker-compose run --rm --entrypoint "\
  rm -Rf /etc/letsencrypt/live/$domain && \
  rm -Rf /etc/letsencrypt/archive/$domain && \
  rm -Rf /etc/letsencrypt/renewal/$domain.conf" certbot
echo


echo "### Requesting Let's Encrypt certificate for $domain ..."
domain_args="-d $domain"

# Select appropriate email arg
case "$email" in
  "") email_arg="--register-unsafely-without-email" ;;
  *) email_arg="--email $email" ;;
esac

# Enable staging mode if needed
if [ $staging != "0" ]; then staging_arg="--staging"; fi

sudo docker-compose run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
    $staging_arg \
    $email_arg \
    $domain_args \
    --rsa-key-size $rsa_key_size \
    --agree-tos \
    --non-interactive \
    --force-renewal" certbot
echo

echo "### Reloading nginx ..."
sudo docker-compose exec nginx nginx -s reload