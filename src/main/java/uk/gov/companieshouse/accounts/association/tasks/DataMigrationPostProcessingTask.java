package uk.gov.companieshouse.accounts.association.tasks;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class DataMigrationPostProcessingTask {

    private final AssociationsService associationsService;

    private final UsersService usersService;

    private static final Logger LOG = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );

    private static final String DATA_MIGRATION_USER_ID_UPDATE_TASK = "data-migration-user-id-update-task";

    @Value( "${scheduler.data-migration.user-id-update-task.items-per-page}" )
    private int ITEMS_PER_PAGE;

    public DataMigrationPostProcessingTask( final AssociationsService associationsService, final UsersService usersService ) {
        this.associationsService = associationsService;
        this.usersService = usersService;
    }

    @Scheduled( cron = "0 0 0 * * ?" )
    public void processMigratedAssociations(){
        LOG.infoContext(DATA_MIGRATION_USER_ID_UPDATE_TASK, "Starting up data migration user_id task...", null );

        final var totalAssociations = associationsService.fetchNumberOfUnprocessedMigratedAssociations();
        final var totalPages = ( totalAssociations / ITEMS_PER_PAGE ) + ( totalAssociations % 2 == 0 ? 0 : 1 );
        int currentPage = (int) totalPages - 1;

        LOG.debugContext(DATA_MIGRATION_USER_ID_UPDATE_TASK, String.format( "Identified %s associations to be processed in %s batches in reverse order... starting with batch %s", totalAssociations, totalPages, currentPage ), null );

        int totalSearched = 0;
        int totalFound = 0;
        int totalNotFound = 0;
        final var totalUpdated = new AtomicInteger();
        final var totalFailed = new AtomicInteger();
        while ( currentPage > -1 ) {
            final var page = associationsService.fetchUnprocessedMigratedAssociations( currentPage, ITEMS_PER_PAGE );

            final var users = usersService.searchUserDetails( page.stream() );
            totalSearched += page.getNumberOfElements();
            totalFound += users.size();
            totalNotFound += page.getNumberOfElements() - users.size();

            page.stream()
                    .filter( associationDao -> users.containsKey( associationDao.getUserEmail() ) )
                    .collect( Collectors.toMap( AssociationDao::getId, associationDao -> users.get( associationDao.getUserEmail() ).getUserId() ) )
                    .forEach( ( associationId, userId ) -> {
                        final var update = new Update()
                                .set( "user_id", userId )
                                .set( "user_email", null );

                        try {
                            associationsService.updateAssociation( associationId, update );
                            totalUpdated.getAndIncrement();
                        } catch ( Exception e ){
                            totalFailed.getAndIncrement();
                        }

                        LOG.infoContext( DATA_MIGRATION_USER_ID_UPDATE_TASK, String.format( "Successfully completed migration of association %s", associationId ), null );
                    } );

            LOG.infoContext( DATA_MIGRATION_USER_ID_UPDATE_TASK, String.format( "Successfully processed batch %s", currentPage ), null );


            final var dataMigrationSummary =
            String.format(
                    """
                    Data Migration Summary:
                    So far, %d searches have been carried out; %d users were found and %d users were not found.
                    So far, %d associations were successfully updated, but updates have failed for %s associations.
                    """, totalSearched, totalFound, totalNotFound, totalUpdated.get(), totalFailed.get() );
            LOG.infoContext( DATA_MIGRATION_USER_ID_UPDATE_TASK, dataMigrationSummary, null );

            if ( totalFailed.get() > 100 ){
                LOG.error( String.format( "%s: Excessive number of update failures. Terminating task...", DATA_MIGRATION_USER_ID_UPDATE_TASK ) );
                break;
            }

            currentPage--;
        }

        LOG.infoContext(DATA_MIGRATION_USER_ID_UPDATE_TASK, "Shutting data migration user_id task...", null );
    }

}
