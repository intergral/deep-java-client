# This file just contains shortcuts for dev, as there are a lot of options for different builds

.PHONY: build
build:
	mvn clean package -U -B -pl agent,deep --also-make $(MVN_ARGS)


.PHONY: install
install:
	mvn clean install -U -B -pl agent,deep --also-make $(MVN_ARGS)
