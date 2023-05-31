
.PHONY: check-component
check-component:
ifndef COMPONENT
	$(error COMPONENT variable was not defined)
endif

.PHONY: prep-jar
prep-jar:
	cp ../../agent/target/agent-1.0-SNAPSHOT.jar ./$(COMPONENT)/deep.jar

.PHONY: prep-cf
prep-cf:
	cp ../../it-tests/cf-tests/src/test/cfml/testFile.cfm ./$(COMPONENT)/testFile.cfm
	cp ../../it-tests/cf-tests/src/test/resources/jvm.config ./$(COMPONENT)/jvm.config

.PHONY: docker
docker: check-component prep-jar prep-cf
	docker build -t ghcr.io/intergral/deep:$(COMPONENT) -f $(COMPONENT)/Dockerfile ./$(COMPONENT)

.PHONY: coldfusion
coldfusion:
	COMPONENT=coldfusion $(MAKE) docker
