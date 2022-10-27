package it.gov.pagopa.initiative.statistics.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.initiative.statistics.dto.events.OnboardingOutcomeDTO;

import java.util.Locale;
import java.util.Random;

public class OnboardingOutcomeDTOFaker {

    private OnboardingOutcomeDTOFaker() {
    }

    private static final Random randomGenerator = new Random();

    private static Random getRandom(Integer bias) {
        return bias == null ? randomGenerator : new Random(bias);
    }

    private static int getRandomPositiveNumber(Integer bias) {
        return Math.abs(getRandom(bias).nextInt());
    }

    private static int getRandomPositiveNumber(Integer bias, int bound) {
        return Math.abs(getRandom(bias).nextInt(bound));
    }

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService(null));

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(getRandom(bias)));
    }

    /**
     * @see #mockInstance(Integer) using INITIATIVEID
     */
    public static OnboardingOutcomeDTO mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    /**
     * It will return an example of {@link OnboardingOutcomeDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static OnboardingOutcomeDTO mockInstance(Integer bias, String initiativeId) {
        return mockInstanceBuilder(bias, initiativeId).build();
    }

    public static OnboardingOutcomeDTO.OnboardingOutcomeDTOBuilder mockInstanceBuilder(Integer bias) {
        return mockInstanceBuilder(bias, "INITIATIVEID");
    }
    public static OnboardingOutcomeDTO.OnboardingOutcomeDTOBuilder mockInstanceBuilder(Integer bias, String initiativeId) {

        return OnboardingOutcomeDTO.builder()
                .userId("USERID%s".formatted(bias))
                .initiativeId(initiativeId)
                .organizationId("ORGANIZATIONID_%s".formatted(initiativeId))
                .status("ONBOARDING_OK");
    }

}
