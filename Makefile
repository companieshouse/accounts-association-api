artifact_name       := accounts-association-api
version             := "unversioned"
dependency_check_base_suppressions:=common_suppressions_spring_6.xml
dependency_check_suppressions_repo_branch:=main
dependency_check_minimum_cvss := 4
dependency_check_assembly_analyzer_enabled := false
dependency_check_suppressions_repo_url:=git@github.com:companieshouse/dependency-check-suppressions.git
suppressions_file := target/suppressions.xml

.PHONY: all
all: build

.PHONY: clean
clean:
	mvn clean
	rm -f ./$(artifact_name).jar
	rm -f ./$(artifact_name)-*.zip
	rm -rf ./build-*
	rm -f ./build.log

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
	mvn test -DexcludedGroups="integration-test"

.PHONY: test-integration
test-integration: clean
	mvn test -Dgroups="integration-test"

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
	mvn sonar:sonar -Dsonar.dependencyCheck.htmlReportPath=./target/dependency-check-report.html

.PHONY: sonar-pr-analysis
sonar-pr-analysis:
		mvn sonar:sonar -P sonar-pr-analysis -Dsonar.dependencyCheck.htmlReportPath=./target/dependency-check-report.html

.PHONY: security-check
security-check:
	@ if [ -d "$(DEPENDENCY_CHECK_SUPPRESSIONS_HOME)" ]; then \
        suppressions_home="$${DEPENDENCY_CHECK_SUPPRESSIONS_HOME}"; \
    fi; \
    if [ ! -d "$${suppressions_home}" ]; then \
        suppressions_home_target_dir="./target/dependency-check-suppressions"; \
        if [ -d "$${suppressions_home_target_dir}" ]; then \
            suppressions_home="$${suppressions_home_target_dir}"; \
        else \
            mkdir -p "./target"; \
            git clone $(dependency_check_suppressions_repo_url) "$${suppressions_home_target_dir}" && \
                suppressions_home="$${suppressions_home_target_dir}"; \
            if [ -d "$${suppressions_home_target_dir}" ] && [ -n "$(dependency_check_suppressions_repo_branch)" ]; then \
                cd "$${suppressions_home}"; \
                git checkout $(dependency_check_suppressions_repo_branch); \
                cd -; \
            fi; \
        fi; \
    fi; \
    mvn org.owasp:dependency-check-maven:update-only ;\
   	mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=4 -DassemblyAnalyzerEnabled=false ;\