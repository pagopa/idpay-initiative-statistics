package it.gov.pagopa.initiative.statistics.service.commands.ops;

import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import it.gov.pagopa.initiative.statistics.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeleteInitiativeServiceImpl implements DeleteInitiativeService{
    private final InitiativeStatRepository initiativeStatRepository;
    private final MerchantInitiativeCountersRepository merchantInitiativeCountersRepository;
    private final AuditUtilities auditUtilities;

    public DeleteInitiativeServiceImpl(InitiativeStatRepository initiativeStatRepository, MerchantInitiativeCountersRepository merchantInitiativeCountersRepository, AuditUtilities auditUtilities) {
        this.initiativeStatRepository = initiativeStatRepository;
        this.merchantInitiativeCountersRepository = merchantInitiativeCountersRepository;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public void execute(String initiativeId) {
        initiativeStatRepository.deleteById(initiativeId);
        log.info("Deleted rule on initiative {}", initiativeId);
        merchantInitiativeCountersRepository.deleteByInitiativeId(initiativeId)
                .forEach(merchantCounter -> {
                    log.info("Delete counter for merchant {} on initiative {}", merchantCounter.getMerchantId(), initiativeId);
                    auditUtilities.logDeletedMerchantCounter(merchantCounter.getMerchantId(), initiativeId);
                });
    }
}
