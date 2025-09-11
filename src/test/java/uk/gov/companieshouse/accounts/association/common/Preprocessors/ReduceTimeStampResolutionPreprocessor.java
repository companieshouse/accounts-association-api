package uk.gov.companieshouse.accounts.association.common.Preprocessors;

import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;

import java.util.Objects;

public class ReduceTimeStampResolutionPreprocessor extends Preprocessor {

    @Override
    public Object preprocess(final Object object) {
        return Objects.isNull(object) ? null : reduceTimestampResolution((String) object);
    }

}
