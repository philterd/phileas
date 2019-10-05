package com.mtnfog.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;

import java.util.Collection;
import java.util.LinkedList;

public class CountyAnonymizationService extends AbstractAnonymizationService {

    private static final Collection<String> COUNTIES = new LinkedList<String>() {{

        add("Beaver");
        add("Ohio");
        add("Tallahatchie");
        add("Braxton");
        add("Orange");
        add("Lemhi");
        add("Wagoner");
        add("Osage");
        add("Rensselaer");
        add("Meeker");
        add("Stark");
        add("McCone");
        add("Clarion");
        add("Spotsylvania");
        add("Accomack");
        add("Dauphin");
        add("Jim Hogg");
        add("Prince Edward");
        add("Greenville");
        add("Tillman");
        add("Ravalli");
        add("Santa Rosa");
        add("Wyandot");
        add("Box Butte");
        add("Milwaukee");
        add("Trinity");
        add("Kleberg");
        add("Ritchie");
        add("Rockland");
        add("Miami-Dade");
        add("Keya Paha");
        add("McCulloch");
        add("Meade");
        add("Collin");
        add("Utah");
        add("Breathitt");
        add("Allen Parish");
        add("Refugio");
        add("Jim Wells");
        add("Torrance");
        add("Lunenburg");
        add("Otsego");
        add("Bryan");
        add("Nueces");
        add("Decatur");
        add("Sibley");
        add("Candler");
        add("Del Norte");
        add("Aleutians East");
        add("Humboldt");
        add("Cheboygan");
        add("Tom Green");
        add("Hodgeman");
        add("Benzie");
        add("Kidder");
        add("Burleigh");
        add("Berrien");
        add("St. Lucie");
        add("Harnett");
        add("Sublette");
        add("Traverse");
        add("Caldwell Parish");
        add("Walworth");
        add("Kalamazoo");
        add("Hamilton");
        add("Yellow Medicine");
        add("Mora");
        add("Sherman");
        add("Bethel");
        add("Charles City");
        add("Daniels");
        add("Washington");
        add("Dearborn");
        add("Solano");
        add("Conejos");
        add("Elk");
        add("Harris");
        add("Fremont");
        add("Addison");
        add("LaGrange");
        add("Sarasota");
        add("Schuyler");
        add("Bacon");
        add("Brookings");
        add("Androscoggin");
        add("Forrest");
        add("Smith");
        add("Milam");
        add("McClain");
        add("Labette");
        add("Powhatan");
        add("Musselshell");
        add("Coosa");
        add("Kootenai");
        add("Parker");
        add("Mitchell");
        add("Niobrara");
        add("Miller");
        add("Bingham");
        add("Borden");

    }};

    public CountyAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
    }

    @Override
    public String anonymize(String token) {

        return COUNTIES.stream()
                .skip((int) (COUNTIES.size() * Math.random()))
                .findFirst().orElse("Harris");

    }

}
