package uk.gov.companieshouse.accounts.association.repositories;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;

@Repository
public class InvitationsRepositoryImpl implements InvitationsRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<AssociationDao> fetchActiveInvitations( String userId ) {

        final var filterForUserIdAndStatusAndExpiry = Aggregation.match(
                new Criteria( "user_id" ).is( userId )
                        .and( "status" ).is( "awaiting-approval" )
                        .and("approval_expiry_at").gt( LocalDateTime.now() )
        );

        final var unwindInvitations = Aggregation.unwind( "invitations" );

        final var sortByInvitedAt = Aggregation.sort( Sort.Direction.DESC, "invitations.invited_at" );

        final var groupByIdAndComputeMostRecentInvitation = Aggregation.group("_id" )
                .first("invitations" ).as( "most_recent_invitation" );

        final var mostRecentInvitationToInvitations = Aggregation.addFields()
                .addField( "invitations" ).withValue( List.of("$most_recent_invitation") )
                .build();

        final var aggregation = Aggregation.newAggregation(
                filterForUserIdAndStatusAndExpiry,
                unwindInvitations,
                sortByInvitedAt,
                groupByIdAndComputeMostRecentInvitation,
                mostRecentInvitationToInvitations,
                sortByInvitedAt
        );

        return mongoTemplate.aggregate( aggregation, "user_company_associations", AssociationDao.class )
                .getMappedResults();
    }

}
