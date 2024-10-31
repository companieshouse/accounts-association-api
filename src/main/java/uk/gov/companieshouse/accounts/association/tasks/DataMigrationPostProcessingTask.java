package uk.gov.companieshouse.accounts.association.tasks;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class DataMigrationPostProcessingTask {

    private final AssociationsService associationsService;

    private final UsersService usersService;

    private static final Logger LOG = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );

    private static final String DATA_MIGRATION_TASK = "data-migration-task";

    // TODO: choose different value e.g. 15
    private static final int ITEMS_PER_PAGE = 2;

    public DataMigrationPostProcessingTask( final AssociationsService associationsService, final UsersService usersService ) {
        this.associationsService = associationsService;
        this.usersService = usersService;
    }

    // TODO: change cron = "0 0 0 * * ?", so that the cron job runs everyday at midnight
    @Scheduled( cron = "0 2 17 * * ?" )
    public void processMigratedAssociations(){
        LOG.infoContext( DATA_MIGRATION_TASK, "Starting up data migration task...", null );


        /* Decided to use pagination because loading in all of the associations at the same time might consume too much memory.

           Updating the associations on a given page will distort the pagination for the subsequent pages, but has no effect on the
           previous pages. So, decided to start from the last page and work back to the first page, to avoid the distortion effect.

           Suggestion: Reactive programming would be well suited to this task.
        */
        final var totalAssociations = associationsService.fetchNumberOfUnprocessedMigratedAssociations();
        final var totalPages = ( totalAssociations / ITEMS_PER_PAGE ) + ( totalAssociations % 2 == 0 ? 0 : 1 );
        int currentPage = (int) totalPages - 1;

        LOG.debugContext( DATA_MIGRATION_TASK, String.format( "Identified %s associations to be processed in %s batches in reverse order... starting with batch %s", totalAssociations, totalPages, currentPage ), null );

        while ( currentPage > -1 ) {
            final var page = associationsService.fetchUnprocessedMigratedAssociations( currentPage, ITEMS_PER_PAGE );

            /* These two blocks extract the user_email's from across all of the associations on the page.
               A single call to the accounts-user-api is made to fetch the user_id's for these user_email's,
               and they are stored in a map (userEmailToIdMapper).

               Note, that this means that ITEMS_PER_PAGE not only determines batch size, but also the number of
               users we request from the accounts-user-api in a single call.
            */
            final var userEmails = page
                    .map( AssociationDao::getUserEmail )
                    .filter( Objects::nonNull )
                    .toList();

            final var userEmailToIdMapper = new HashMap<String, String>();
            Optional.ofNullable( usersService.searchUserDetails( userEmails ) )
                    .orElse( new UsersList() )
                    .forEach( user -> userEmailToIdMapper.put( user.getEmail(), user.getUserId() ) );

            /* This block filters out the associations on the page, so that we skip any associations relating to
               users that were not found by the accounts-user-api (i.e. users without a CHS account).
               The remaining associations are processed.
            */
            page.filter( associationDao -> userEmailToIdMapper.containsKey( associationDao.getUserEmail() ) )
                    .forEach( associationDao -> {
                        final var update = new Update()
                                .set( "user_id", userEmailToIdMapper.get( associationDao.getUserEmail() ) )
                                .set( "user_email", null );
                        associationsService.updateAssociation( associationDao.getId(), update );

                        LOG.debugContext( DATA_MIGRATION_TASK, String.format( "Successfully completed migration of association %s", associationDao.getId() ), null );
                    } );

            LOG.debugContext( DATA_MIGRATION_TASK, String.format( "Successfully processed batch %s", currentPage ), null );

            currentPage--;
        }

        LOG.infoContext( DATA_MIGRATION_TASK, "Shutting data migration task...", null );

    }

}
