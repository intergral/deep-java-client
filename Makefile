

.PHONY: pmd
pmd:
	mvn -U -B verify -Ppmd -DskipTests

,PHONY: check-formatting
check-formatting:
	mvn -U -B validate -Plint,examples,cf-it-tests

.PHONY: test
test:
	mvn -U -B clean verify

.PHONY: docs
docs:
	mvn -s .ci-settings.xml clean package javadoc:jar -DskipTests -P release-ossrh -B -U -pl agent,deep --also-make

.PHONY: package
package:
	mvn package -U -B -pl agent --also-make -DskipTests

.PHONY: cf-tests
cf-tests:
	mvn verify -U -B -P cf-it-tests -pl it-tests/cf-tests --also-make
