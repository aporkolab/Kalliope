package hu.porkolab.kalliope.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AnalyzeController.class, CanonController.class})
class AnalyzeControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("az elemzés soronként adja a skandálást, a mértéket és a rímbetűt")
    void analyzeReturnsFullAnalysis() throws Exception {
        mvc.perform(post("/api/analyze").contentType(MediaType.APPLICATION_JSON).content("""
                                {"text":"Romlásnak indult hajdan erős magyar!\\nNem látod, Árpád vére miként fajul?\\nNem látod a bosszús egeknek\\nOstorait nyomorult hazádon?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stanzas[0].lines[0].scansion").isNotEmpty())
                .andExpect(jsonPath("$.stanzas[0].lines[0].syllables[0].reason").isNotEmpty())
                .andExpect(jsonPath("$.stanzas[0].forms[0].form.name").value("alkaioszi strófa"))
                .andExpect(jsonPath("$.summary.lineCount").value(4));
    }

    @Test
    @DisplayName("üres szöveg 400-at ad, ProblemDetail alakban")
    void emptyTextIsRejected() throws Exception {
        mvc.perform(post("/api/analyze").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("ismeretlen beállítás 400-at ad, nem 500-at")
    void unknownSettingIsBadRequest() throws Exception {
        mvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"vers\",\"settings\":{\"nincs_ilyen\":true}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Ismeretlen beállítás")));
    }

    @Test
    @DisplayName("a kánon egyben adja a mértékeket, a beállításokat és a szótárakat")
    void canonIsSelfContained() throws Exception {
        mvc.perform(get("/api/canon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meters.length()").value(org.hamcrest.Matchers.greaterThan(100)))
                .andExpect(jsonPath("$.stanzas.length()").value(18))
                .andExpect(jsonPath("$.settings[0].label").isNotEmpty())
                .andExpect(jsonPath("$.reasons[0].explanation").isNotEmpty());
    }

    @Test
    @DisplayName("a kánon kereshető")
    void canonIsSearchable() throws Exception {
        mvc.perform(get("/api/canon").param("q", "alkaioszi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meters.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("ismeretlen mértékazonosító 400")
    void unknownMeterIsBadRequest() throws Exception {
        mvc.perform(get("/api/canon/nincs-ilyen")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a példatár elérhető")
    void examplesAreServed() throws Exception {
        mvc.perform(get("/api/examples"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].expected").isNotEmpty());
    }
}
