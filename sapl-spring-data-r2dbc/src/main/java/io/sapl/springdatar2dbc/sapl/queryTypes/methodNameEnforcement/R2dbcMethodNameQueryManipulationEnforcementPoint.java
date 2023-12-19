package io.sapl.springdatar2dbc.sapl.queryTypes.methodNameEnforcement;

import com.fasterxml.jackson.databind.JsonNode;
import io.sapl.springdatar2dbc.sapl.QueryManipulationExecutor;
import io.sapl.springdatar2dbc.sapl.QueryManipulationEnforcementData;
import io.sapl.springdatar2dbc.sapl.QueryManipulationEnforcementPoint;
import io.sapl.springdatar2dbc.sapl.handlers.DataManipulationHandler;
import io.sapl.springdatar2dbc.sapl.handlers.LoggingConstraintHandlerProvider;
import io.sapl.springdatar2dbc.sapl.handlers.R2dbcQueryManipulationObligationProvider;
import io.sapl.api.pdp.AuthorizationDecision;
import io.sapl.api.pdp.AuthorizationSubscription;
import io.sapl.api.pdp.Decision;
import lombok.SneakyThrows;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;

import static io.sapl.springdatar2dbc.sapl.utils.ConstraintHandlerUtils.*;

/**
 * This class is responsible for the implementation of the
 * R2dbcMethodNameQueryManipulationEnforcementPoint for all methods in the
 * repository that query can be derived from the method name.
 *
 * @param <T> is the domain type.
 */
public class R2dbcMethodNameQueryManipulationEnforcementPoint<T> implements QueryManipulationEnforcementPoint<T> {
    private final R2dbcQueryManipulationObligationProvider r2dbcQueryManipulationObligationProvider = new R2dbcQueryManipulationObligationProvider();
    private final LoggingConstraintHandlerProvider         loggingConstraintHandlerProvider         = new LoggingConstraintHandlerProvider();
    private final Logger                                   logger                                   = Logger
            .getLogger(R2dbcMethodNameQueryManipulationEnforcementPoint.class.getName());
    private final DataManipulationHandler<T>               dataManipulationHandler;
    private final QueryManipulationEnforcementData<T>      enforcementData;

    public R2dbcMethodNameQueryManipulationEnforcementPoint(QueryManipulationEnforcementData<T> enforcementData) {
        this.enforcementData         = enforcementData;
        this.dataManipulationHandler = new DataManipulationHandler<>(enforcementData.getDomainType());
    }

    /**
     * The PDP {@link io.sapl.api.pdp.PolicyDecisionPoint} is called with the
     * appropriate {@link AuthorizationSubscription} and then the {@link Decision}
     * of the PDP is forwarded.
     *
     * @return database objects that may have been filtered and/or transformed.
     */
    @Override
    public Flux<T> enforce() {
        return Mono.defer(() -> enforcementData.getPdp().decide(enforcementData.getAuthSub()).next())
                .flatMapMany(enforceDecision());
    }

    /**
     * The decision is checked for permitting and throws an
     * {@link AccessDeniedException} accordingly. Otherwise, the {@link Decision}'s
     * obligation is applied to the objects in the database.
     *
     * @return database objects that may have been filtered and/or transformed.
     */
    public Function<AuthorizationDecision, Flux<T>> enforceDecision() {
        return (decision) -> {
            var decisionIsPermit = Decision.PERMIT == decision.getDecision();
            var advice           = getAdvices(decision);

            loggingConstraintHandlerProvider.getHandler(advice).run();

            if (decisionIsPermit) {
                var obligations = getObligations(decision);
                var data        = retrieveData(obligations);

                return dataManipulationHandler.manipulate(obligations).apply(data);
            } else {
                return Flux.error(new AccessDeniedException("Access Denied by PDP"));
            }
        };
    }

    /**
     * Receives the data from the database, if desired, and forwards it. If desired,
     * the query is manipulated and then the database is called with it.
     *
     * @param obligations are the obligations from the {@link Decision}.
     * @return objects from the database that were queried with the manipulated
     *         query.
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    private Flux<T> retrieveData(JsonNode obligations) {
        if (r2dbcQueryManipulationObligationProvider.isResponsible(obligations)) {
            return enforceQueryManipulation(obligations);
        } else {

            if (enforcementData.getMethodInvocation().getMethod().getReturnType().equals(Mono.class)) {
                return Flux.from((Mono<T>) Objects.requireNonNull(enforcementData.getMethodInvocation().proceed()));
            }

            return (Flux<T>) enforcementData.getMethodInvocation().proceed();
        }
    }

    /**
     * Calling the database with the manipulated query.
     *
     * @param obligations are the obligations from the {@link Decision}.
     * @return objects from the database that were queried with the manipulated
     *         query.
     */
    private Flux<T> enforceQueryManipulation(JsonNode obligations) {
        var manipulatedCondition = createSqlQuery(obligations);

        logger.info("Manipulated condition: [" + manipulatedCondition + "]");

        return QueryManipulationExecutor
                .execute(manipulatedCondition, enforcementData.getBeanFactory(), enforcementData.getDomainType())
                .map(dataManipulationHandler.toDomainObject());
    }

    /**
     * The method fetches the matching obligation and extracts the condition from
     * it. This condition is appended to the end of the sql query. The base query is
     * converted from the method name.
     *
     * @param obligations are the obligations from the {@link Decision}.
     * @return created sql query.
     */
    private String createSqlQuery(JsonNode obligations) {
        var r2dbcQueryManipulationObligation = r2dbcQueryManipulationObligationProvider.getObligation(obligations);
        var condition                        = r2dbcQueryManipulationObligationProvider
                .getCondition(r2dbcQueryManipulationObligation);
        var sqlConditionFromDecision         = addMissingConjunction(condition.asText());
        var baseQuery                        = PartTreeToSqlQueryStringConverter.createSqlBaseQuery(enforcementData);

        return baseQuery + sqlConditionFromDecision;
    }

    /**
     * If the condition from the obligation does not have a conjunction, an "AND"
     * conjunction is automatically assumed and appended to the base query.
     *
     * @param sqlConditionFromDecision represents the condition
     * @return the condition with conjunction or not
     */
    private String addMissingConjunction(String sqlConditionFromDecision) {
        var conditionStartsWithConjunction = sqlConditionFromDecision.toLowerCase().trim().startsWith("and ")
                || sqlConditionFromDecision.toLowerCase().trim().startsWith("or ");

        if (conditionStartsWithConjunction) {
            return " " + sqlConditionFromDecision;
        } else {
            return " AND " + sqlConditionFromDecision;
        }
    }

}