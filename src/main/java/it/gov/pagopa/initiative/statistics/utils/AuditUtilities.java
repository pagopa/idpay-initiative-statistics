package it.gov.pagopa.initiative.statistics.utils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
@AllArgsConstructor
@Slf4j(topic = "AUDIT")
public class AuditUtilities {
    public static final String SRCIP;

    static {
        String srcIp;
        try {
            srcIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("Cannot determine the ip of the current host", e);
            srcIp="UNKNOWN";
        }

        SRCIP = srcIp;
    }

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=NotificationManager dstip=%s", SRCIP);
    private static final String CEF_BASE_PATTERN = CEF + " msg={}";
    private static final String CEF_PATTERN = CEF_BASE_PATTERN + " suser={} cs1Label=initiativeId cs1={}";

    private void logAuditString(String pattern, String... parameters) {
        log.info(pattern, (Object[]) parameters);
    }

    public void logDeletedMerchantCounter(String userId, String initiativeId){
        logAuditString(
                CEF_PATTERN,
                "Merchant counter deleted", userId,initiativeId
        );
    }

}