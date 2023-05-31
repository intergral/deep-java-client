
.PHONY: check-component
check-component:
ifndef COMPONENT
	$(error COMPONENT variable was not defined)
endif

.PHONY: prep-jar
prep-jar:
	cp ../../agent/target/agent-1.0-SNAPSHOT.jar ./$(COMPONENT)/deep.jar

.PHONY: docker
docker: check-component prep-jar
	docker build -t ghcr.io/intergral/deep:$(COMPONENT) -f $(COMPONENT)/Dockerfile ./$(COMPONENT)

.PHONY: coldfusion
coldfusion:
	COMPONENT=coldfusion $(MAKE) docker
