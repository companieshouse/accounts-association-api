artifact_name       := accounts-association-api
version             := "unversioned"

.PHONY: all
all: build

.PHONY: clean
clean:
	mvn clean
	rm -f ./$(artifact_name).jar
	rm -f ./$(artifact_name)-*.zip
	rm -rf ./build-*
	rm -f ./build.log

.PHONY: security-check
security-check:
	mvn org.owasp:dependency-check-maven:update-only
	mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=4 -DassemblyAnalyzerEnabled=false

.PHONY: build
build:
	mvn versions:set -DnewVersion=$(version) -DgenerateBackupPoms=false
	mvn package -DskipTests=true
	cp ./target/$(artifact_name)-$(version).jar ./$(artifact_name).jar

.PHONY: test
test: clean
	mvn verify

.PHONY: test-unit
test-unit: clean
	openssl version
	yum install -y openssl
	mvn test -Dincluded.tests="unit-test"

.PHONY: test-integration
test-integration: clean
	mvn test -Dincluded.tests="integration-test"

.PHONY: package
package:
ifndef version
	$(error No version given. Aborting)
endif
	$(info Packaging version: $(version))
	mvn versions:set -DnewVersion=$(version) -DgenerateBackupPoms=false
	mvn package -DskipTests=true
	$(eval tmpdir:=$(shell mktemp -d build-XXXXXXXXXX))
	cp ./start.sh $(tmpdir)
	cp ./routes.yaml $(tmpdir)
	cp ./target/$(artifact_name)-$(version).jar $(tmpdir)/$(artifact_name).jar
	cd $(tmpdir); zip -r ../$(artifact_name)-$(version).zip *
	rm -rf $(tmpdir)

.PHONY: dist
dist: clean build package

.PHONY: sonar
sonar:
	mvn sonar:sonar

.PHONY: sonar-pr-analysis
sonar-pr-analysis:
	mvn sonar:sonar -P sonar-pr-analysis