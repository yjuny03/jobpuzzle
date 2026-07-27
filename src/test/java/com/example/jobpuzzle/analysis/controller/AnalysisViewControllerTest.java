package com.example.jobpuzzle.analysis.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AnalysisViewControllerTest {

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new AnalysisViewController()).build();

    @Test
    void rendersAnalysisResultPageWithCaseId() throws Exception {
        mockMvc.perform(get("/api/analysis/{analysisCaseId}", 12L))
                .andExpect(status().isOk())
                .andExpect(view().name("analysis"))
                .andExpect(model().attribute("analysisCaseId", 12L));
    }
}
