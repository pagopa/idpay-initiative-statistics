package it.gov.pagopa.initiative.statistics.service.merchant.counters;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.initiative.statistics.dto.MerchantStatisticsDTO;
import it.gov.pagopa.initiative.statistics.dto.mapper.MerchantInitiativeCounters2MerchantInitiativeStatisticsDTOMapper;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;

@Service
@Slf4j
public class MerchantCountersServiceImpl implements MerchantCountersService {
    private final MerchantInitiativeCountersRepository countersRepository;
    private final MerchantInitiativeCounters2MerchantInitiativeStatisticsDTOMapper counter2DtoMapper;

    public MerchantCountersServiceImpl(MerchantInitiativeCountersRepository countersRepository, MerchantInitiativeCounters2MerchantInitiativeStatisticsDTOMapper counter2DtoMapper) {
        this.countersRepository = countersRepository;
        this.counter2DtoMapper = counter2DtoMapper;
    }

    @Override
    public MerchantStatisticsDTO getMerchantInitiativeStatistics(String merchantId, String initiativeId) {
        return countersRepository.findById(MerchantInitiativeCounters.buildId(merchantId, initiativeId))
                .map(counter2DtoMapper)

                .orElseThrow(() -> new ClientExceptionNoBody(HttpStatus.NOT_FOUND,
                        MessageFormat.format("No stats found for merchant with id: {0} and initiative: {1}", merchantId, initiativeId)));
    }
}
