package com.example.jobpuzzle.analysis.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AnalysisResultViewControllerTest {

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new AnalysisResultViewController()).build();

    @Test
    void rendersReadOnlyAnalysisResultPageWithCaseId() throws Exception {
        mockMvc.perform(get("/analysis-results/{analysisCaseId}", 38L))
                .andExpect(status().isOk())
                .andExpect(view().name("analysis-result"))
                .andExpect(model().attribute("analysisCaseId", 38L));
    }
}
