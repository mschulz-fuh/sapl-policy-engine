/*
 * Copyright © 2017-2022 Dominic Heutelbeck (dominic@heutelbeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sapl.interpreter.pip;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.validation.constraints.NotNull;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.sapl.api.interpreter.Val;
import io.sapl.api.pip.Attribute;
import io.sapl.api.pip.EnvironmentAttribute;
import io.sapl.api.pip.PolicyInformationPoint;
import io.sapl.api.validation.Bool;
import io.sapl.api.validation.Text;
import io.sapl.grammar.sapl.impl.util.ParserUtil;
import io.sapl.interpreter.InitializationException;
import io.sapl.interpreter.context.AuthorizationContext;
import io.sapl.interpreter.functions.AnnotationFunctionContext;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

public class AnnotationAttributeContextTests {

	@Test
	public void when_classHasNoAnnotation_fail() {
		assertThrows(InitializationException.class, () -> new AnnotationAttributeContext(
				"I am an instance of a class without @PolicyInformationPoint annotation"));
	}

	@Test
	public void when_classHasNoAttributesDeclared_fail() {
		@PolicyInformationPoint
		class PIP {

		}
		var pip = new PIP();
		assertThrows(InitializationException.class, () -> new AnnotationAttributeContext(pip));
	}

	@Test
	public void when_pipWithSameNameExists_fail() {
		@PolicyInformationPoint(name = "somePip")
		class PIP {

			@Attribute
			public Flux<Val> x() {
				return null;
			}

		}

		var pip = new PIP();
		assertThrows(InitializationException.class, () -> new AnnotationAttributeContext(pip, pip));
	}

	@Test
	public void when_firstParameterOfAttributeIllegal_fail() {
		@PolicyInformationPoint
		class PIP {

			@Attribute
			public Flux<Val> x(String x) {
				return null;
			}

		}

		var pip = new PIP();
		assertThrows(InitializationException.class, () -> new AnnotationAttributeContext(pip));
	}

	@Test
	public void when_returnTypeIllegal_fail() {
		@PolicyInformationPoint
		class PIP {

			@Attribute
			public void x() {
			}

		}

		var pip = new PIP();
		assertThrows(InitializationException.class, () -> new AnnotationAttributeContext(pip));
	}

	@Test
	public void when_returnTypeIllegalFluxType_fail() {
		@PolicyInformationPoint
		class PIP {

			@Attribute
			public Flux<String> x() {
				return null;
			}

		}

		var pip = new PIP();
		assertThrows(InitializationException.class, () -> new AnnotationAttributeContext(pip));
	}

	@Test
	public void when_returnTypeIllegalGenericVal_fail() {
		@PolicyInformationPoint
		class PIP {

			@Attribute
			public List<Val> x() {
				return null;
			}

		}

		var pip = new PIP();
		assertThrows(InitializationException.class, () -> new AnnotationAttributeContext(pip));
	}

	@Test
	public void when_firstAndOnlyParameterVal_loadSuccessful() {
		@PolicyInformationPoint
		class PIP {

			@Attribute
			public Flux<Val> x(Val leftHand) {
				return null;
			}

		}

		var pip = new PIP();
		var ctx = new AnnotationAttributeContext();
		assertDoesNotThrow(() -> ctx.loadPolicyInformationPoint(pip));
		assertThat(ctx.getAvailableLibraries().contains("PIP"), is(true));
		assertThat(ctx.providedFunctionsOfLibrary("PIP").contains("x"), is(true));
		assertThat(ctx.isProvidedFunction("PIP.x"), is(true));
		assertThat(new ArrayList<>(ctx.getDocumentation()).get(0).getName(), is("PIP"));
	}

	@Test
	public void when_noPip_providedIsEmpty() {
		var ctx = new AnnotationAttributeContext();
		assertThat(ctx.providedFunctionsOfLibrary("PIP").isEmpty(), is(true));
		assertThat(ctx.getDocumentation().isEmpty(), is(true));
	}

	@Test
	public void when_firstAndOnlyParameterVariablesMap_loadSuccessful() {
		@PolicyInformationPoint
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> x(Map<String, JsonNode> variables) {
				return null;
			}

		}

		var pip = new PIP();
		var ctx = new AnnotationAttributeContext();
		assertDoesNotThrow(() -> ctx.loadPolicyInformationPoint(pip));
	}

	@Test
	public void when_firstAndOnlyParameterMapWithBadKeyType_fail() {
		@PolicyInformationPoint
		class PIP {

			@Attribute
			public Flux<Val> x(Map<Long, JsonNode> variables) {
				return null;
			}

		}

		var pip = new PIP();
		assertThrows(InitializationException.class, () -> new AnnotationAttributeContext(pip));
	}

	@Test
	public void when_firstAndOnlyParameterMapWithBadValueType_fail() {
		@PolicyInformationPoint
		class PIP {

			@Attribute
			public Flux<Val> x(Map<String, String> variables) {
				return null;
			}

		}

		var pip = new PIP();
		assertThrows(InitializationException.class, () -> new AnnotationAttributeContext(pip));
	}

	@Test
	public void when_firstAndOnlyParameterOfEnvAttributeVal_loadSuccessful() {
		@PolicyInformationPoint
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> x(Val firstParameter) {
				return null;
			}

		}

		var pip = new PIP();
		var ctx = new AnnotationAttributeContext();
		assertDoesNotThrow(() -> ctx.loadPolicyInformationPoint(pip));
	}

	@Test
	public void when_firstAndOnlyParameterNotAVal_fail() {
		@PolicyInformationPoint
		class PIP {

			@Attribute
			public Flux<Val> x(Object firstParameter) {
				return null;
			}

		}

		var pip = new PIP();
		assertThrows(InitializationException.class, () -> new AnnotationAttributeContext(pip));
	}

	@Test
	public void when_someParamBAdType_fail() {
		@PolicyInformationPoint
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> x(Val firstParameter, String x) {
				return null;
			}

		}

		var pip = new PIP();
		assertThrows(InitializationException.class, () -> new AnnotationAttributeContext(pip));
	}

	@Test
	public void when_firstAndOnlyParameterIsVarArgsOfVal_loadSuccessful() {
		@PolicyInformationPoint
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> x(Val... varArgsParams) {
				return null;
			}

		}

		var pip = new PIP();
		var ctx = new AnnotationAttributeContext();
		assertDoesNotThrow(() -> ctx.loadPolicyInformationPoint(pip));
	}

	@Test
	public void when_firstAndOnlyParameterIsArrayOfVal_loadSuccessful() {
		@PolicyInformationPoint
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> x(Val[] varArgsParams) {
				return null;
			}

		}

		var pip = new PIP();
		var ctx = new AnnotationAttributeContext();
		assertDoesNotThrow(() -> ctx.loadPolicyInformationPoint(pip));
	}

	@Test
	public void when_arrayFollowedBYSomething_failImport() {
		@PolicyInformationPoint
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> x(Val[] varArgsParams, String iAmTooMuchToHandle) {
				return null;
			}

		}

		var pip = new PIP();
		var ctx = new AnnotationAttributeContext();
		assertThrows(InitializationException.class, () -> ctx.loadPolicyInformationPoint(pip));
	}

	@Test
	public void when_firstAndOnlyParameterIsVarArgsString_fail() {
		@PolicyInformationPoint
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> x(String... varArgsParams) {
				return null;
			}

		}

		var pip = new PIP();
		var ctx = new AnnotationAttributeContext();
		assertThrows(InitializationException.class, () -> ctx.loadPolicyInformationPoint(pip));
	}

	@Test
	public void when_differentNames_noCollision() throws InitializationException {
		@PolicyInformationPoint
		class PIP {

			@Attribute
			public Flux<Val> x(Val leftHand) {
				return null;
			}

			@Attribute
			public Flux<Val> y(Val leftHand) {
				return null;
			}

		}

		var pip = new PIP();
		var ctx = new AnnotationAttributeContext();
		assertDoesNotThrow(() -> ctx.loadPolicyInformationPoint(pip));
	}

	@Test
	public void when_envAndNonEnv_noCollision() throws InitializationException {
		@PolicyInformationPoint
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> x() {
				return null;
			}

			@Attribute
			public Flux<Val> x(Val leftHand) {
				return null;
			}

		}

		var pip = new PIP();
		var ctx = new AnnotationAttributeContext();
		assertDoesNotThrow(() -> ctx.loadPolicyInformationPoint(pip));
	}

	@Test
	public void when_varargAndNonVarArg_noCollision() throws InitializationException {
		@PolicyInformationPoint
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> x(Val a) {
				return null;
			}

			@EnvironmentAttribute
			public Flux<Val> x(Val... a) {
				return null;
			}

		}

		var pip = new PIP();
		var ctx = new AnnotationAttributeContext();
		assertDoesNotThrow(() -> ctx.loadPolicyInformationPoint(pip));
	}

	@Test
	public void when_twoVarArg_collision() throws InitializationException {
		@PolicyInformationPoint
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> x(Val[] a) {
				return null;
			}

			@EnvironmentAttribute(name = "x")
			public Flux<Val> y(Val... a) {
				return null;
			}

		}

		var pip = new PIP();
		var ctx = new AnnotationAttributeContext();
		assertThrows(InitializationException.class, () -> ctx.loadPolicyInformationPoint(pip));
	}

	@Test
	public void when_sameNumberOFParams_collision() throws InitializationException {
		@PolicyInformationPoint
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> x(Val a) {
				return null;
			}

			@EnvironmentAttribute(name = "x")
			public Flux<Val> y(Val b) {
				return null;
			}

		}

		var pip = new PIP();
		var ctx = new AnnotationAttributeContext();
		assertThrows(InitializationException.class, () -> ctx.loadPolicyInformationPoint(pip));
	}

	@Test
	public void when_evaluateUnknownAttribute_fails() throws InitializationException, IOException {
		var attributeCtx = new AnnotationAttributeContext();
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("<test.envAttribute(\"param1\",\"param2\")>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNextMatches(valErrorText("Unknown attribute test.envAttribute")).verifyComplete();
	}

	@Test
	public void when_varArgsWithVariablesEnvironmentAttribute_evaluates() throws InitializationException, IOException {
		@PolicyInformationPoint(name = "test")
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> envAttribute(Map<String, JsonNode> variables, @Text Val... varArgsParams) {
				return Flux.just(varArgsParams[1]);
			}

		}

		var pip          = new PIP();
		var attributeCtx = new AnnotationAttributeContext(pip);
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("<test.envAttribute(\"param1\",\"param2\")>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNext(Val.of("param2")).verifyComplete();
	}

	@Test
	public void when_varArgsWithVariablesAttribute_evaluates() throws InitializationException, IOException {
		@PolicyInformationPoint(name = "test")
		class PIP {

			@Attribute
			public Flux<Val> attribute(Val leftHand, Map<String, JsonNode> variables, @Text Val... varArgsParams) {
				return Flux.just(varArgsParams[1]);
			}

		}

		var pip          = new PIP();
		var attributeCtx = new AnnotationAttributeContext(pip);
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("\"\".<test.attribute(\"param1\",\"param2\")>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNext(Val.of("param2")).verifyComplete();
	}

	@Test
	public void when_varArgsWithVariablesAndTwoAttributes_evaluates() throws InitializationException, IOException {
		@PolicyInformationPoint(name = "test")
		class PIP {

			@Attribute
			public Flux<Val> attribute(Val leftHand, Map<String, JsonNode> variables, @Text Val param1,
					@Text Val param2) {
				return Flux.just(param2);
			}

		}

		var pip          = new PIP();
		var attributeCtx = new AnnotationAttributeContext(pip);
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("\"\".<test.attribute(\"param1\",\"param2\")>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNext(Val.of("param2")).verifyComplete();
	}

	@Test
	public void when_varArgsNoVariablesEnvironmentAttribute_evaluates() throws InitializationException, IOException {
		@PolicyInformationPoint(name = "test")
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> envAttribute(@Text Val... varArgsParams) {
				return Flux.just(varArgsParams[1]);
			}

		}

		var pip          = new PIP();
		var attributeCtx = new AnnotationAttributeContext(pip);
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("<test.envAttribute(\"param1\",\"param2\")>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNext(Val.of("param2")).verifyComplete();
	}

	@Test
	public void when_varsAndParamEnvironmentAttribute_evaluates() throws InitializationException, IOException {
		@PolicyInformationPoint(name = "test")
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> envAttribute(Map<String, JsonNode> variables, @Text Val param1) {
				return Flux.just(param1);
			}

		}

		var pip          = new PIP();
		var attributeCtx = new AnnotationAttributeContext(pip);
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("<test.envAttribute(\"param1\")>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNext(Val.of("param1")).verifyComplete();
	}

	@Test
	public void when_varArgsAndTwoParamEnvironmentAttribute_evaluatesExactParameterMatch()
			throws InitializationException, IOException {
		@PolicyInformationPoint(name = "test")
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> envAttribute(Map<String, JsonNode> variables, @Text Val param1, @Text Val param2) {
				return Flux.just(param1);
			}

			@EnvironmentAttribute
			public Flux<Val> envAttribute(Map<String, JsonNode> variables, @Text Val... varArgsParams) {
				return Flux.just(varArgsParams[1]);
			}

		}

		var pip          = new PIP();
		var attributeCtx = new AnnotationAttributeContext(pip);
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("<test.envAttribute(\"param1\",\"param2\")>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNext(Val.of("param1")).verifyComplete();
	}

	@Test
	public void when_varArgsEnvironmentAttribute_calledWithNoParams_evalsVarArgsWithEmptyParamArray()
			throws InitializationException, IOException {
		@PolicyInformationPoint(name = "test")
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> envAttribute(Map<String, JsonNode> variables, @Text Val... varArgsParams) {
				return Flux.just(Val.of(varArgsParams.length));
			}

		}

		var pip          = new PIP();
		var attributeCtx = new AnnotationAttributeContext(pip);
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("<test.envAttribute>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNext(Val.of(0)).verifyComplete();
	}

	@Test
	public void when_noArgsEnvironmentAttribute_called_evals() throws InitializationException, IOException {
		@PolicyInformationPoint(name = "test")
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> envAttribute() {
				return Flux.just(Val.of("OK"));
			}

		}

		var pip          = new PIP();
		var attributeCtx = new AnnotationAttributeContext(pip);
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("<test.envAttribute>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNext(Val.of("OK")).verifyComplete();
	}

	@Test
	public void when_noArgsEnvironmentAttribute_calledAndFails_evalsToError()
			throws InitializationException, IOException {
		@PolicyInformationPoint(name = "test")
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> envAttribute() {
				throw new IllegalStateException("INTENDED ERROR FROM TEST");
			}

		}

		var pip          = new PIP();
		var attributeCtx = new AnnotationAttributeContext(pip);
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("<test.envAttribute>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNextMatches(valErrorText("INTENDED ERROR FROM TEST")).verifyComplete();
	}

	@Test
	public void when_noArgsAttribute_calledAndFails_evalsToError() throws InitializationException, IOException {
		@PolicyInformationPoint(name = "test")
		class PIP {

			@Attribute
			public Flux<Val> attribute(Val leftHand) {
				throw new IllegalStateException("INTENDED ERROR FROM TEST");
			}

		}

		var pip          = new PIP();
		var attributeCtx = new AnnotationAttributeContext(pip);
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("\"\".<test.attribute>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNextMatches(valErrorText("INTENDED ERROR FROM TEST")).verifyComplete();
	}

	private Predicate<Val> valErrorText(String errorMessage) {
		return val -> val.isError() && errorMessage.equals(val.getMessage());
	}
	
	@Test
	public void when_noArgsAttribute_called_evals() throws InitializationException, IOException {
		@PolicyInformationPoint(name = "test")
		class PIP {

			@Attribute
			public Flux<Val> attribute(Val leftHand) {
				return Flux.just(leftHand);
			}

		}

		var pip          = new PIP();
		var attributeCtx = new AnnotationAttributeContext(pip);
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("\"\".<test.attribute>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNext(Val.of("")).verifyComplete();
	}

	@Test
	public void when_unkownAttribute_called_evalsToError() throws InitializationException, IOException {
		var attributeCtx = new AnnotationAttributeContext();
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("\"\".<test.envAttribute>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNextMatches(valErrorText("Unknown attribute test.envAttribute")).verifyComplete();
	}

	@Test
	public void when_twoParamEnvironmentAttribute_calledWithOneParam_evaluatesToError()
			throws InitializationException, IOException {
		@PolicyInformationPoint(name = "test")
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> envAttribute(Map<String, JsonNode> variables, @Text Val param1, @Text Val param2) {
				return Flux.just(param1);
			}

		}

		var pip          = new PIP();
		var attributeCtx = new AnnotationAttributeContext(pip);
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("<test.envAttribute(\"param1\")>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNextMatches(valErrorText("Unknown attribute test.envAttribute")).verifyComplete();
	}

	@Test
	public void when_varArgsWithVariablesEnvironmentAttributeAndBadParamType_evaluatesToError()
			throws InitializationException, IOException {
		@PolicyInformationPoint(name = "test")
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> envAttribute(Map<String, JsonNode> variables, @Bool Val... varArgsParams) {
				return Flux.just(varArgsParams[1]);
			}

		}

		var pip          = new PIP();
		var attributeCtx = new AnnotationAttributeContext(pip);
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("<test.envAttribute(\"param1\",\"param2\")>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNextMatches(valErrorText("Illegal parameter type. Got: STRING Expected: @io.sapl.api.validation.Bool() ")).verifyComplete();
	}

	@Test
	public void generatesCodeTemplates() throws InitializationException, IOException {

		@PolicyInformationPoint(name = "test")
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> a(Map<String, JsonNode> variables, @Bool @Text Val... varArgsParams) {
				return Flux.just(varArgsParams[1]);
			}

			@EnvironmentAttribute
			public Flux<Val> a(@Bool @Text Val a1, @Bool Val a2) {
				return Flux.just(a1);
			}

			@EnvironmentAttribute
			public Flux<Val> a2(Val a1, Val a2) {
				return Flux.just(a1);
			}

			@EnvironmentAttribute
			public Flux<Val> a2() {
				return Flux.empty();
			}

			@Attribute
			public Flux<Val> x(Val leftHand, Map<String, JsonNode> variables, @Bool @Text Val... varArgsParams) {
				return Flux.just(varArgsParams[1]);
			}

			@Attribute
			public Flux<Val> x(Val leftHand, @NotNull @Bool @Text Val a1, @Bool Val a2) {
				return Flux.just(a1);
			}

			@Attribute
			public Flux<Val> x2(Val leftHand, Val a1, Val a2) {
				return Flux.just(a1);
			}

		}

		var pip = new PIP();
		var sut = new AnnotationAttributeContext(pip);

		var expectedEnvirionmentTemplates = new String[] { "test.a(a1, a2)>", "test.a(varArgsParams...)>",
				"test.a2(a1, a2)>", "test.a2>" };
		var actualEnvironmentTemplates    = sut.getEnvironmentAttributeCodeTemplates();
		actualEnvironmentTemplates = sut.getEnvironmentAttributeCodeTemplates();
		assertThat(actualEnvironmentTemplates, containsInAnyOrder(expectedEnvirionmentTemplates));

		var expectedNonEnvirionmentTemplates = new String[] { "test.x2(a1, a2)>", "test.x(varArgsParams...)>",
				"test.x(a1, a2)>" };
		var actualNonEnvironmentTemplates    = sut.getAttributeCodeTemplates();
		actualNonEnvironmentTemplates = sut.getAttributeCodeTemplates();
		assertThat(actualNonEnvironmentTemplates, containsInAnyOrder(expectedNonEnvirionmentTemplates));

		assertThat(sut.getAvailableLibraries(), containsInAnyOrder("test"));
		assertThat(sut.getAllFullyQualifiedFunctions().size(), is(7));
		assertThat(sut.getAllFullyQualifiedFunctions(),
				containsInAnyOrder("test.x2", "test.a", "test.a", "test.a2", "test.a2", "test.x", "test.x"));
	}

	@Test
	public void when_environmentAttributeButOnlyNonEnvAttributePresent_fail()
			throws InitializationException, IOException {
		@PolicyInformationPoint(name = "test")
		class PIP {

			@Attribute
			public Flux<Val> attribute(Val leftHand, Map<String, JsonNode> variables, @Text Val... varArgsParams) {
				return Flux.just(varArgsParams[1]);
			}

		}

		var pip          = new PIP();
		var attributeCtx = new AnnotationAttributeContext(pip);
		var variables    = Map.of("key1", (JsonNode) Val.JSON.textNode("valueOfKey"));
		var expression   = ParserUtil.expression("<test.attribute(\"param1\",\"param2\")>");
		StepVerifier.create(expression.evaluate().contextWrite(this.constructContext(attributeCtx, variables)))
				.expectNextMatches(valErrorText("Unknown attribute test.attribute")).verifyComplete();
	}

	private Function<Context, Context> constructContext(AttributeContext attributeCtx,
			Map<String, JsonNode> variables) {
		return ctx -> {
			ctx = AuthorizationContext.setAttributeContext(ctx, attributeCtx);
			ctx = AuthorizationContext.setFunctionContext(ctx, new AnnotationFunctionContext());
			ctx = AuthorizationContext.setVariables(ctx, variables);
			return ctx;
		};
	}

	@Test
	public void addsDocumentedAttributeCodeTemplates() throws InitializationException {

		final String pipName        = "test";
		final String pipDescription = "description";

		@PolicyInformationPoint(name = pipName, description = pipDescription)
		class PIP {

			@EnvironmentAttribute
			public Flux<Val> empty() {
				return Flux.empty();
			}
		}

		var pip = new PIP();
		var sut = new AnnotationAttributeContext(pip);

		var actualTemplates = sut.getDocumentedAttributeCodeTemplates();
		assertThat(actualTemplates, hasEntry(pipName, pipDescription));
	}

}