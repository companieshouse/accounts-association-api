package uk.gov.companieshouse.accounts.association.utils;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;

/**
 * The type Association dao stream splitter.
 * Passed into a .collect() call on a stream of {@link AssociationDao}'s, this will produce two
 * {@link ArrayList}'s consisting of userId's and userEmails (only in the event that a userId is null)
 * respectively
 * <p>
 * Example usage: splitStreams = associations.collect(Collector.of(AssociationDaoStreamSplitter::new, AssociationDaoStreamSplitter::accept, AssociationDaoStreamSplitter::merge));
 * <p>
 * Possibly parallelization safe.
 * @see <a href="https://stackoverflow.com/a/46540204">https://stackoverflow.com/a/46540204</a>
 */
public class AssociationDaoStreamSplitter {

    /**
     * The User ids.
     */
    public final List<String> userIds = new ArrayList<>();
    /**
     * The User emails.
     */
    public final List<String> userEmails = new ArrayList<>();

    /**
     * Called per-stream item to file accordingly
     *
     * @param dao the dao
     */
    public void accept(AssociationDao dao) {
        // Assume user ids cannot contain @'s
        if (!StringUtils.isBlank(dao.getUserId())) {
            userIds.add(dao.getUserId());
        } else if (!StringUtils.isBlank(dao.getUserEmail())) {
            userEmails.add(dao.getUserEmail());
        }
    }

    /**
     * Merge association dao stream splitter.
     *
     * @param toBeMerged the instance to be merged
     * @return the combined association dao stream splitter
     */
    public AssociationDaoStreamSplitter merge(AssociationDaoStreamSplitter toBeMerged) {
        userIds.addAll(toBeMerged.userIds);
        userEmails.addAll(toBeMerged.userEmails);
        return this;
    }

}
