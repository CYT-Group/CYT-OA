package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.core.exception.BizException;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceValidationTest {
    @Test void newPoliciesAcceptExplicitZeroVersionAndRejectInvalidVersions(){
        assertEquals(0,WorkspaceValidation.version(Map.of("version",0)));
        assertEquals(3,WorkspaceValidation.version(Map.of("version","3")));
        for(Object invalid:List.of(-1,1.5,"NaN","2147483648"))
            assertThrows(BizException.class,()->WorkspaceValidation.version(Map.of("version",invalid)));
    }
    @Test void surveyAnswersMustMatchConfiguredQuestionsAndOptions(){
        var question=Map.<String,Object>of("id","q1","title","你的选择","type","multiple","required",true,"options",List.of("A","B"));
        var survey=Map.<String,Object>of("questions",List.of(question));
        assertEquals(Map.of("q1",List.of("A")),WorkspaceSurveyService.validateAnswers(survey,Map.of("q1",List.of("A"))));
        for(var invalid:List.of(Map.<String,Object>of(),Map.<String,Object>of("q1",List.of("C")),Map.<String,Object>of("q1",List.of("A","A")),Map.<String,Object>of("q1",List.of("A"),"hidden","injected")))
            assertThrows(BizException.class,()->WorkspaceSurveyService.validateAnswers(survey,invalid));
    }
    @Test void ratingRejectsFractionsAndOutOfRangeValues(){
        var survey=Map.<String,Object>of("questions",List.of(Map.of("id","score","title","评分","type","rating","required",true)));
        for(Object invalid:List.of(0,6,2.5,"NaN"))assertThrows(BizException.class,()->WorkspaceSurveyService.validateAnswers(survey,Map.of("score",invalid)));
        assertEquals(5,WorkspaceSurveyService.validateAnswers(survey,Map.of("score",5)).get("score"));
    }
    @Test void objectivesUseWeightedProgressAndRejectImpossibleTargets(){
        var body=new HashMap<String,Object>();body.put("period","2026-Q4");body.put("scope","personal");
        body.put("keyResults",List.of(Map.of("title","销售","target",100,"current",50,"weight",75,"unit","单"),Map.of("title","培训","target",10,"current",10,"weight",25,"unit","次")));
        assertEquals(62.5,WorkspaceObjectiveService.progress(WorkspaceObjectiveService.validate(body)));
        body.put("keyResults",List.of(Map.of("title","销售","target",10,"current",11,"weight",1)));
        assertThrows(BizException.class,()->WorkspaceObjectiveService.validate(body));
    }
    @Test void leaveAllocatesAcrossYearsAndExcludesWeekendAndLunch(){
        var rules=Map.<String,Object>of("startTime","09:00","endTime","18:00");
        var days=WorkspaceRequestService.leaveDays(LocalDateTime.parse("2026-12-31T09:00"),LocalDateTime.parse("2027-01-04T18:00"),rules);
        assertEquals(1.0,days.get("2026"));assertEquals(2.0,days.get("2027"));
        assertTrue(WorkspaceRequestService.leaveDays(LocalDateTime.parse("2026-11-14T09:00"),LocalDateTime.parse("2026-11-15T18:00"),rules).isEmpty());
        assertTrue(WorkspaceRequestService.leaveDays(LocalDateTime.parse("2026-11-11T12:00"),LocalDateTime.parse("2026-11-11T13:00"),rules).isEmpty());
    }
    @Test void singleExpenseHonorsCurrentCategoryLimitIncludingExactBoundary(){
        var rules=WorkspacePolicyService.defaults("expense");
        assertDoesNotThrow(()->WorkspaceRequestService.validateExpense(Map.of("expenseKind","hotel","amount",500),rules));
        var error=assertThrows(BizException.class,()->WorkspaceRequestService.validateExpense(Map.of("expenseKind","hotel","amount",500.01),rules));
        assertTrue(error.getMessage().contains("住宿单次"));assertTrue(error.getMessage().contains("500 元（CNY）"));
        var lowered=new HashMap<>(rules);lowered.put("hotel",200);
        assertThrows(BizException.class,()->WorkspaceRequestService.validateExpense(Map.of("expenseKind","hotel","amount",300),lowered));
    }
    @Test void expenseCategoryIsEnumeratedAndCurrencyAlwaysCnyWithoutLimitingTravelTotal(){
        var expense=new HashMap<String,Object>(Map.of("type","expense","reason","差旅住宿","amount",100,"expenseKind","hotel","currency","USD"));
        assertEquals("CNY",WorkspaceRequestService.validate(expense,WorkspacePolicyService.defaults("attendance")).get("currency"));
        expense.put("expenseKind","other");assertThrows(BizException.class,()->WorkspaceRequestService.validate(expense,WorkspacePolicyService.defaults("attendance")));
        var travel=Map.<String,Object>of("type","travel","reason","出差","amount",5000,"startAt","2026-11-11T09:00","endAt","2026-11-15T18:00","destination","成都");
        assertEquals(5000.0,WorkspaceRequestService.validate(travel,WorkspacePolicyService.defaults("attendance")).get("amount"));
    }
}
