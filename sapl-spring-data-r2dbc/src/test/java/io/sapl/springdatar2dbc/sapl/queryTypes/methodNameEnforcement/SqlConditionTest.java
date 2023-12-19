package io.sapl.springdatar2dbc.sapl.queryTypes.methodNameEnforcement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SqlConditionTest {

    @Test
    void when_getConjunction_then_returnConjunction() {
        // GIVEN
        var sqlCondition = new SqlCondition(Conjunction.AND, "Test condition");

        // WHEN
        var result = sqlCondition.getConjunction();

        // THEN
        Assertions.assertEquals(result, Conjunction.AND);
    }

    @Test
    void when_getCondition_then_returnCondition() {
        // GIVEN
        var sqlCondition = new SqlCondition(Conjunction.AND, "Test condition");

        // WHEN
        var result = sqlCondition.getCondition();

        // THEN
        Assertions.assertEquals(result, "Test condition");
    }

    @Test
    void when_setCondition_then_setCondition() {
        // GIVEN
        var sqlCondition = new SqlCondition(Conjunction.AND, "Test condition");

        // WHEN
        sqlCondition.setCondition("Test condition set");

        // THEN
        Assertions.assertEquals("Test condition set", sqlCondition.getCondition());
    }

    @Test
    void when_setConjunction_then_setConjunction() {
        // GIVEN
        var sqlCondition = new SqlCondition(Conjunction.AND, "Test condition");

        // WHEN
        sqlCondition.setConjunction(Conjunction.OR);

        // THEN
        Assertions.assertEquals(Conjunction.OR, sqlCondition.getConjunction());
    }
}