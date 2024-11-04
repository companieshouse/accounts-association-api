package uk.gov.companieshouse.accounts.association.tasks;

import java.util.stream.Collectors;
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

    // TODO: choose different value e.g. 15
    private static final int ITEMS_PER_PAGE = 2;

    public DataMigrationPostProcessingTask( final AssociationsService associationsService, final UsersService usersService ) {
        this.associationsService = associationsService;
        this.usersService = usersService;
    }

    // TODO: change cron = "0 0 0 * * ?", so that the cron job runs everyday at midnight
    @Scheduled( cron = "0 2 17 * * ?" )
    public void processMigratedAssociations(){
        LOG.infoContext(DATA_MIGRATION_USER_ID_UPDATE_TASK, "Starting up data migration user_id task...", null );

        final var totalAssociations = associationsService.fetchNumberOfUnprocessedMigratedAssociations();
        final var totalPages = ( totalAssociations / ITEMS_PER_PAGE ) + ( totalAssociations % 2 == 0 ? 0 : 1 );
        int currentPage = (int) totalPages - 1;

        LOG.debugContext(DATA_MIGRATION_USER_ID_UPDATE_TASK, String.format( "Identified %s associations to be processed in %s batches in reverse order... starting with batch %s", totalAssociations, totalPages, currentPage ), null );

        while ( currentPage > -1 ) {
            final var page = associationsService.fetchUnprocessedMigratedAssociations( currentPage, ITEMS_PER_PAGE );

            final var users = usersService.searchUserDetails( page.stream() );

            page.stream()
                    .filter( associationDao -> users.containsKey( associationDao.getUserEmail() ) )
                    .collect( Collectors.toMap( AssociationDao::getId, associationDao -> users.get( associationDao.getUserEmail() ).getUserId() ) )
                    .forEach( ( associationId, userId ) -> {
                        final var update = new Update()
                                .set( "user_id", userId )
                                .set( "user_email", null );
                        associationsService.updateAssociation( associationId, update );
                        LOG.debugContext(DATA_MIGRATION_USER_ID_UPDATE_TASK, String.format( "Successfully completed migration of association %s", associationId ), null );
                    } );

            LOG.debugContext(DATA_MIGRATION_USER_ID_UPDATE_TASK, String.format( "Successfully processed batch %s", currentPage ), null );

            currentPage--;
        }

        LOG.infoContext(DATA_MIGRATION_USER_ID_UPDATE_TASK, "Shutting data migration user_id task...", null );
    }




    /*
TODO: print out a count of people who haven't logged in, in over a month

This query will find a given user's latest session in the last month:

db.oauth2_authorisations.findOne(
{
	"user_details.user_id": "WITU002",
	"token_valid_until": { $gt: (Date.now() / 1000) - (30 * 24 * 60 * 60) }
},
{ sort: { "token_valid_until": -1 } }  )

Steps:
1. For each user in users...
2.    Run the above query to see if the user logged in, in the last month.
3.    If they did not log in, then add to a counter
4. Print counter

Ideally this query would go in the authentication-service. It doesn't look like there is an endpoint
there to retrieve relevant details
     */


}
