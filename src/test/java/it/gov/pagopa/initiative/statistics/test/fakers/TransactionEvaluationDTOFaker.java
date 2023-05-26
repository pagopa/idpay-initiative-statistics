package it.gov.pagopa.initiative.statistics.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.initiative.statistics.dto.events.Reward;
import it.gov.pagopa.initiative.statistics.dto.events.TransactionEvaluationDTO;
import it.gov.pagopa.common.utils.TestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class TransactionEvaluationDTOFaker {

    private TransactionEvaluationDTOFaker() {
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
    public static TransactionEvaluationDTO mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    /**
     * It will return an example of {@link TransactionEvaluationDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static TransactionEvaluationDTO mockInstance(Integer bias, String initiativeId) {
        return mockInstanceBuilder(bias, initiativeId).build();
    }

    public static TransactionEvaluationDTO.TransactionEvaluationDTOBuilder mockInstanceBuilder(Integer bias) {
        return mockInstanceBuilder(bias, "INITIATIVEID");
    }
    public static TransactionEvaluationDTO.TransactionEvaluationDTOBuilder mockInstanceBuilder(Integer bias, String initiativeId) {
        BigDecimal amount = TestUtils.bigDecimalValue(getRandomPositiveNumber(bias, 200));

        return TransactionEvaluationDTO.builder()
                .id("TRXID%s".formatted(bias))
                .userId("USERID%s".formatted(bias))
                .operationTypeTranscoded("CHARGE")
                .status("REWARDED")
                .rewards(Map.of(
                        initiativeId, new Reward(initiativeId, "ORGANIZATIONID_%s".formatted(initiativeId), amount.multiply(BigDecimal.valueOf(0.1)).setScale(2, RoundingMode.HALF_DOWN))
                ));
    }

}
