package it.gov.pagopa.initiative.statistics.service.commands.ops;

import it.gov.pagopa.initiative.statistics.dto.events.CommandOperationDTO;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import it.gov.pagopa.initiative.statistics.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@SuppressWarnings("BusyWait")
public class DeleteInitiativeServiceImpl implements DeleteInitiativeService{
    private final InitiativeStatRepository initiativeStatRepository;
    private final MerchantInitiativeCountersRepository merchantInitiativeCountersRepository;
    private final AuditUtilities auditUtilities;
    private static final String PAGINATION_KEY = "pagination";
    private static final String DELAY_KEY = "delay";

    public DeleteInitiativeServiceImpl(InitiativeStatRepository initiativeStatRepository, MerchantInitiativeCountersRepository merchantInitiativeCountersRepository, AuditUtilities auditUtilities) {
        this.initiativeStatRepository = initiativeStatRepository;
        this.merchantInitiativeCountersRepository = merchantInitiativeCountersRepository;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public void execute(CommandOperationDTO payload) {
        initiativeStatRepository.deleteById(payload.getEntityId());
        log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: initiative_statistics", payload.getEntityId());
        auditUtilities.logDeletedInitiativeStatistics(payload.getEntityId());

        List<MerchantInitiativeCounters> deletedMerchantInitiativeCounters = new ArrayList<>();
        List<MerchantInitiativeCounters> fetchedMerchantInitiativeCounters;

        do {
            fetchedMerchantInitiativeCounters = merchantInitiativeCountersRepository.deletePaged(payload.getEntityId(),
                    Integer.parseInt(payload.getAdditionalParams().get(PAGINATION_KEY)));
            deletedMerchantInitiativeCounters.addAll(fetchedMerchantInitiativeCounters);
            try{
                Thread.sleep(Long.parseLong(payload.getAdditionalParams().get(DELAY_KEY)));
            } catch (InterruptedException e){
                log.error("An error has occurred while waiting {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        } while (fetchedMerchantInitiativeCounters.size() == (Integer.parseInt(payload.getAdditionalParams().get(PAGINATION_KEY))));

        deletedMerchantInitiativeCounters.forEach(merchantCounter -> auditUtilities.logDeletedMerchantCounter(merchantCounter.getMerchantId(), payload.getEntityId()));
        log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: merchant_initiative_counters", payload.getEntityId());
    }
}
