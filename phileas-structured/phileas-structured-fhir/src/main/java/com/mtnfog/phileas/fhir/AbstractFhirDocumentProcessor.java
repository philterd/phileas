package com.mtnfog.phileas.fhir;

import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.utils.Encryption;
import org.hl7.fhir.r4.model.StringType;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractFhirDocumentProcessor {

    protected List<StringType> encryptList(List<StringType> list, Crypto crypto) throws Exception {

        final List<StringType> encryptedList = new LinkedList<>();

        for(StringType st : list) {

            final String encryptedValue = Encryption.encrypt(st.getValueAsString(), crypto);

            final StringType clone = st.copy();
            clone.setValueAsString(encryptedValue);

        }

        return encryptedList;

    }

}
