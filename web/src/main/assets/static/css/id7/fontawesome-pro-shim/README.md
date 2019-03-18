These files are only used if fontawesome-pro isn't available, for example if
you're building open source Tabula in development. Normally you'd expect to have
FontAwesome Pro available by running the following:

	npm config set "@fortawesome:registry" https://npm.fontawesome.com/ && \
	npm config set "//npm.fontawesome.com/:_authToken" [token]

Where `[token]` is from our FA Pro subscription.