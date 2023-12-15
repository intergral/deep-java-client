# This file just contains shortcuts for dev, as there are a lot of options for different builds

# Run the PMD check - while skipping tests
# Doesn't run PMD on it tests or examples
.PHONY: pmd
pmd:
	mvn -U -B verify -Ppmd -DskipTests $(MVN_ARGS)

# Run lint on all code
,PHONY: lint
lint:
	mvn -U -B validate -Plint,examples,cf-it-tests $(MVN_ARGS)

# Package and verify all code expect cf-tests and examples
.PHONY: test
test:
	mvn -U -B clean verify $(MVN_ARGS)

# Generate documentation from source
.PHONY: docs
docs:
	mvn -s .ci-settings.xml clean package javadoc:jar -DskipTests -P release-ossrh -B -U -pl agent,deep --also-make $(MVN_ARGS)

# Generate the agent and rebuild all the dependant modules
.PHONY: package-agent
package:
	mvn package -U -B -pl agent --also-make -DskipTests $(MVN_ARGS)

# Run the CF it tests
.PHONY: cf-tests
cf-tests: package
	mvn verify -U -B -P cf-it-tests -pl it-tests/cf-tests --also-make $(MVN_ARGS)

# Build the agent and deep, also rebuild all dependant modules
.PHONY: build
build:
	mvn clean package -U -B -pl agent,deep --also-make $(MVN_ARGS)

# Install the agent and deep into the local repo (~/.m2)
.PHONY: install
install:
	mvn clean install -U -B -pl agent,deep --also-make $(MVN_ARGS)

# Run the coverage checks
.PHONY: coverage
coverage:
	mvn clean verify -U -B -P coverage -pl '!it-tests/java-tests,!it-tests'

# Compile all modules
.PHONY: compile
compile:
	mvn clean compile -U -B -P cf-it-tests,coverage,docs,examples,lint,pmd

# Combine a few common checks in a single task
.PHONY: precommit
precommit:
	$(MAKE) lint
	$(MAKE) pmd
	$(MAKE) coverage
	$(MAKE) docs
	$(MAKE) compile