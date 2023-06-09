package it.gov.pagopa.initiative.statistics.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.initiative.statistics.dto.InitiativeStatisticsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface InitiativeApiController {

    @Operation(summary = "Return of onboard users and the budget spent (accrued bonus)", description = "", security = {
            @SecurityRequirement(name = "Bearer")}, tags = {"initiative"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InitiativeStatisticsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "The requested ID was not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "429", description = "Too many Request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "Server ERROR", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class)))})
    @GetMapping(value = "/idpay/organization/{organizationId}/initiative/{initiativeId}/statistics",
            produces = {"application/json"})
    ResponseEntity<InitiativeStatisticsDTO> initiativeStatistics(@Parameter(in = ParameterIn.PATH, description = "The Organization ID", required = true, schema = @Schema()) @PathVariable("organizationId") String organizationId,
                                                                 @Parameter(in = ParameterIn.PATH, description = "The initiative ID", required = true, schema = @Schema()) @PathVariable("initiativeId") String initiativeId);

}

