

.PHONY: pmd
pmd:
	mvn -U -B verify -Ppmd -DskipTests $(MVN_ARGS)

,PHONY: lint
lint:
	mvn -U -B validate -Plint,examples,cf-it-tests $(MVN_ARGS)

.PHONY: test
test:
	mvn -U -B clean verify $(MVN_ARGS)

.PHONY: docs
docs:
	mvn -s .ci-settings.xml clean package javadoc:jar -DskipTests -P release-ossrh -B -U -pl agent,deep --also-make $(MVN_ARGS)

.PHONY: package-agent
package:
	mvn package -U -B -pl agent --also-make -DskipTests $(MVN_ARGS)

.PHONY: cf-tests
cf-tests: package
	mvn verify -U -B -P cf-it-tests -pl it-tests/cf-tests --also-make $(MVN_ARGS)
# This file just contains shortcuts for dev, as there are a lot of options for different builds

.PHONY: build
build:
	mvn clean package -U -B -pl agent,deep --also-make $(MVN_ARGS)


.PHONY: install
install:
	mvn clean install -U -B -pl agent,deep --also-make $(MVN_ARGS)

.PHONY: coverage
coverage:
	mvn clean verify -U -B -P coverage -pl '!it-tests/java-tests,!it-tests'

.PHONY: precommit
precommit:
	$(MAKE) lint
	$(MAKE) pmd
	$(MAKE) coverage
	$(MAKE) docs