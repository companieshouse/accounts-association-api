package uk.gov.companieshouse.accounts.association.repositories;

import org.junit.After;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.companieshouse.accounts.association.models.Associations;

@DataMongoTest()
@ExtendWith(SpringExtension.class)
class AssociationsRepositoryTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    private AssociationsRepository associationsRepository;

//    @Before
//    void setup(){
//        final var associationOne = new Associations();
//        associationOne.setCompanyNumber( "111111" );
//        associationOne.setUserId( "111" );
//
//        final var associationTwo = new Associations();
//        associationTwo.setCompanyNumber( "222222" );
//        associationTwo.setUserId( "111" );
//
//        final var associationThree = new Associations();
//        associationThree.setCompanyNumber( "111111" );
//        associationThree.setUserId( "222" );
//
//        associationsRepository.insert( associationOne );
//        associationsRepository.insert( associationTwo );
//        associationsRepository.insert( associationThree );
//    }


    @Test
    void findAllByCompanyNumber() {

        final var associationOne = new Associations();
        associationOne.setCompanyNumber( "111111" );
        associationOne.setUserId( "111" );

        final var associationTwo = new Associations();
        associationTwo.setCompanyNumber( "222222" );
        associationTwo.setUserId( "111" );

        final var associationThree = new Associations();
        associationThree.setCompanyNumber( "111111" );
        associationThree.setUserId( "222" );

        associationsRepository.save(associationOne);
        associationsRepository.insert( associationTwo );
        associationsRepository.insert( associationThree );

        associationsRepository.findAllByCompanyNumber("111111");
    }

    @Test
    void findAllByUserId() {
    }

    @After
    void after(){
        mongoTemplate.dropCollection(Associations.class);
    }
}