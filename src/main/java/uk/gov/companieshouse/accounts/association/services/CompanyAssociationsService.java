package uk.gov.companieshouse.accounts.association.services;

import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.mappers.UsersMapper;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.repositories.UsersRepository;
import uk.gov.companieshouse.api.accounts.associations.model.UserInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CompanyAssociationsService {

    private final AssociationsRepository associationsRepository;

    private final UsersRepository usersRepository;

    public CompanyAssociationsService(AssociationsRepository associationsRepository, UsersRepository usersRepository) {
        this.associationsRepository = associationsRepository;
        this.usersRepository = usersRepository;
    }

    public List<UserInfo> getUsersByCompanyNumber(final String companyNumber) {

        List<Association> associations = Optional.ofNullable(associationsRepository
                .findAllByCompanyNumber(companyNumber)).orElse(new ArrayList<>());

        //map of user ids and userinfo
        return  getUserInfoData(associations);

    }

    private List<UserInfo> getUserInfoData(List<Association> associations) {
        //Map of user id and status from associations.
        Map<String, String> usedIdStatusMap =
                associations
                        .stream()
                        .collect(
                                Collectors
                                        .toMap(Association::getUserId, Association::getStatus)
                        );

        final UsersMapper mapper = Mappers.getMapper(UsersMapper.class);

        return usersRepository
                .findAllById(
                        associations
                                .stream()
                                .map(Association::getUserId)
                                .toList()
                ).stream()
                .map(mapper::mapUserInfo)
                .peek(userInfo -> userInfo.setAutorisationStatus(usedIdStatusMap.get(userInfo.getUserId())))
                .toList();

    }
}
