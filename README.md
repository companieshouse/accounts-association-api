# Accounts Association API

### Overview
This documentation provides details about the Account User Company Service API, which facilitates company associations, delegated admin, and authorization functionalities.

For more detailed information and related Confluence pages, refer to the following:


- [LLD](https://companieshouse.atlassian.net/wiki/spaces/IDV/pages/4471619740/Version+3+-+Low+level+designs)
- [HLD](https://companieshouse.atlassian.net/wiki/spaces/IDV/pages/4471619599/High+Level+Design+V3)
- [API Specification](https://companieshouse.atlassian.net/wiki/spaces/IDV/pages/4471619599/High+Level+Design+V3)

### Prerequisites
To build the service and execute unit tests, ensure you have the following:
- Java 21
- Maven
- Git

## Endpoint Documentation

## Authentication

This API employs two types of authentication:

1. **API Key Authentication:** To authenticate requests using API keys, include the `Authorization` header with the API key.
2. **Bearer Token Authentication:** For bearer token authentication, include a JWT token in the `Authorization` header.

## Endpoints

### 1. Associations for a Company

- **Endpoint:** `/associations/companies/{company_number}`
- **Method:** GET
- **Description:** Retrieve associations for a specific company.
- **Parameters:**
   - `company_number` (path parameter, required): The unique identifier of the company.
   - `X-Request-Id` (header, required): Request ID for tracking purposes.
   - `include_removed` (query parameter, optional): A flag to include removed associations.
   - `page_index` (query parameter, optional): Page number to be returned.
   - `items_per_page` (query parameter, optional): Number of items to be returned per page.
- **Responses:**
   - `200`: Success response with a list of associations.
   - `400`: Bad request.
   - `401`: Unauthorized.
   - `403`: Forbidden.
   - `404`: Company not found.
   - `500`: Internal server error.

### 2. Associations between Company and User

- **Endpoint:** `/associations`
- **Method:** GET
- **Description:** Search associations based on specified criteria.
- **Parameters:**
   - `X-Request-Id` (header, required): Request ID for tracking purposes.
   - `ERIC-Identity` (header, required): Identity information.
   - `page_index` (query parameter, optional): Page number to be returned.
   - `items_per_page` (query parameter, optional): Number of items to be returned per page.
   - `company_number` (query parameter, optional): Filter based on company number.
   - `status` (query parameter, optional): Filter based on status.
- **Responses:**
   - `200`: Success response with a list of associations.
   - `204`: No content found.
   - `400`: Bad request.
   - `401`: Unauthorized.
   - `403`: Forbidden.
   - `500`: Internal server error.


### 3. Create Association for User in Session

- **Endpoint:** `/associations`
- **Method:** POST
- **Description:** Create an association for the user in session with the status "awaiting-approval".
- **Parameters:**
   - `X-Request-Id` (header, required): Request ID for tracking purposes.
   - `ERIC-Identity` (header, required): Identity information.
- **Request Body:**
   - `company_number` (string, required): The unique identifier of the company.
- **Responses:**
   - `201`: Association created successfully.
   - `400`: Bad request.
   - `401`: Unauthorized.
   - `403`: Forbidden.
   - `500`: Internal server error.

### 4. Create Association for Invited User

- **Endpoint:** `/associations/invitations`
- **Method:** POST
- **Description:** Invite a user to create an association.
- **Parameters:**
   - `X-Request-Id` (header, required): Request ID for tracking purposes.
   - `ERIC-Identity` (header, required): Identity information.
- **Request Body:**
   - `invitee_email_id` (string, required): Email ID of the invited user.
   - `company_number` (string, required): The unique identifier of the company.
- **Responses:**
   - `201`: Association created successfully.
   - `400`: Bad request.
   - `401`: Unauthorized.
   - `403`: Forbidden.
   - `500`: Internal server error.

### 5. Get Association Data

- **Endpoint:** `/associations/{id}`
- **Method:** GET
- **Description:** Retrieve association data for a specified association ID.
- **Parameters:**
   - `X-Request-Id` (header, required): Request ID for tracking purposes.
   - `id` (path parameter, required): The unique identifier of the association.
- **Responses:**
   - `200`: Success response with association data.
   - `204`: No content found.
   - `400`: Bad request.
   - `401`: Unauthorized.
   - `403`: Forbidden.
   - `500`: Internal server error.

### 6. Update Association Status

- **Endpoint:** `/associations/{id}`
- **Method:** PATCH
- **Description:** Update the status of a specific association.
- **Parameters:**
   - `X-Request-Id` (header, required): Request ID for tracking purposes.
   - `id` (path parameter, required): The unique identifier of the association.
- **Request Body:**
   - `status` (string, required): The new status of the association (`confirmed`, `removed`).
- **Responses:**
   - `200`: Association status updated successfully.
   - `204`: No content found.
   - `400`: Bad request.
   - `401`: Unauthorized.
   - `403`: Forbidden.
   - `500`: Internal server error.

## Data Models

### AssociationsList
- **Description:** Represents a list of associations.
- **Properties:**
   - `items`: Array of association objects.
   - `links`: Links for pagination.
   - `items_per_page`: Number of items returned per page.
   - `page_number`: Number of the current page.
   - `total_results`: Total count of associations for all pages.
   - `total_pages`: Total number of pages.

### Association
- **Description:** Represents an association between a company and a user.
- **Properties:**
   - Various properties including `etag`, `id`, `user_id`, `user_email`, `display_name`, `company_number`, `company_name`, `status`, `created_at`, `approved_at`, `removed_at`, `kind`, `approval_route`, `approval_expiry_at`, `invitations`, and `links`.

### Invitation
- **Description:** Represents an invitation to associate a user with a company.
- **Properties:**
   - `invited_by`: Email ID of the user who sent the invitation.
   - `invited_at`: Timestamp indicating when the invitation was sent.

### Errors
- **Description:** Represents error details.
- **Properties:**
   - `errors`: Array of error objects containing error information.
  
## Adding a new API Endpoint:
### Specification for API: Private Java and Controllers
In this section, we will enhance the specification file to encompass:

- API Endpoint
- Comprehensive Parameter Description (Header, Query, and Path)
- Exemplary Dataset

Upon finalization of the controller code, integration testing, such as with Postman, can commence.

### Create Service and Mongo Dao and Repository

Steps:

- Utilize the model class as a foundation for creating the DAO.
- Within the service class, establish values for any read-only fields.
- Author unit and integration tests (MongoDB)
- Integrate the controller to utilize the service.
- Following approval and compilation of this code, testers can commence testing for the functionality.

### Add Validation in the Service if required

Service should do any business or database checks. If a resource request is not found then a 404 error is returned

A business validation error will throw an exception that will be handled in the application ControllerAdvice class and a HTTP Bad request is returned with an Errors object from the CH standard Errors class.

## Getting Started with Docker
To set up and build the service using Docker, follow these steps:

1. Clone Docker CHS Development repository and follow instructions in the README.
2. Execute the following:
    - `./bin/chs-dev services enable accounts-association-api`
    - `./bin/chs-dev development enable accounts-association-api`
3. Ensure you're using Java 21
4. Start Docker using `tilt up` in the docker-chs-development directory.
5.  Open the tilt window and wait for `accounts-association-api` to become green.
6. Open your browser and navigate to http://api.chs.local/associations/healthcheck.

Note: These instructions are tailored for a local Docker environment.

For  further details, please refer to the documentation and associated resources.

