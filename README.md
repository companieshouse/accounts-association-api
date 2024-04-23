# accounts-association-api
The Accounts association api is used to manage associations between users and companies.

## 1.0) Environment

This section describes how the environment must be setup, in order to run the accounts-association-api locally.

### 1.1) docker-chs-development

The accounts-association-api uses the `email-producer-java` library to send out notifications. This library depends on Kafka and other projects that use Kafka e.g. `chs-email-sender`.

We discovered that there is a race condition in Tilt which can cause Kafka related projects to load in the incorrect order. To rectify this, edit `docker-chs-development/services/modules/platform/api-ch-gov-uk.docker-compose.yaml` and `docker-chs-development/services/modules/platform/account-ch-gov-uk.docker-compose.yaml`, by adding `- kafka` to the depends_on section.

### 1.2) Tilt

The following modules must be enabled: `platform` and `streaming`.

The following services must be enabled: `accounts-user-api`, `accounts-association-api`, and `company-profile-api`.
