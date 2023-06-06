package it.gov.pagopa.initiative.statistics.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.initiative.statistics.dto.events.RewardNotificationDTO;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class RewardNotificationDTOFaker {

    private RewardNotificationDTOFaker() {
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
     * @see #mockInstance(Integer, boolean) using INITIATIVEID
     */
    public static RewardNotificationDTO mockInstance(Integer bias, boolean merchant) {
        return mockInstanceBuilder(bias, merchant).build();
    }

    /**
     * It will return an example of {@link RewardNotificationDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static RewardNotificationDTO mockInstance(Integer bias, String initiativeId, boolean merchant) {
        return mockInstanceBuilder(bias, initiativeId, merchant).build();
    }

    public static RewardNotificationDTO.RewardNotificationDTOBuilder mockInstanceBuilder(Integer bias, boolean merchant) {
        return mockInstanceBuilder(bias, "INITIATIVEID", merchant);
    }
    public static RewardNotificationDTO.RewardNotificationDTOBuilder mockInstanceBuilder(Integer bias, String initiativeId, boolean merchant) {
        LocalDate now = LocalDate.now();
        String notificationDateFormatted = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String beneficiaryPrefix = merchant ? "MERCHANTID" : "USERID";

        String notificationId = "%s%d_%s_%s".formatted(beneficiaryPrefix, bias, initiativeId, notificationDateFormatted);
        String externalId = "%s_%s".formatted(UUID.nameUUIDFromBytes(notificationId.getBytes(StandardCharsets.UTF_8)), notificationDateFormatted);

        return RewardNotificationDTO.builder()
                .id(notificationId)
                .externalId(externalId)
                .rewardNotificationId(notificationId)
                .initiativeId(initiativeId)
                .beneficiaryId("%s%d".formatted(beneficiaryPrefix, bias))
                .organizationId("ORGANIZATIONID%d".formatted(bias))
                .iban("IBAN%d".formatted(bias))
                .status("STATUS")
                .rewardStatus("REWARDSTATUS")
                .refundType("ORDINARY")
                .rewardCents(1000L)
                .effectiveRewardCents(1000L)
                .startDate(now)
                .endDate(now.plusDays(1))
                .feedbackDate(now.atStartOfDay())
                .rejectionCode(null)
                .rejectionReason(null)
                .feedbackProgressive(1L)
                .executionDate(now)
                .transferDate(now)
                .userNotificationDate(now)
                .cro("CRO%d".formatted(bias*731));
    }

}
