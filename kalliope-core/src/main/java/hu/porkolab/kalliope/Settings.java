package hu.porkolab.kalliope;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Az elemzés kapcsolói. Az első hat az eredeti adatbázis {@code !} sorainak
 * felel meg; az utolsó kettő ennek a változatnak a dokumentált kiegészítése.
 *
 * <p>A tisztán megjelenítésbeli beállítások ({@code a_jobb_oldali_szoveg_formazott_legyen},
 * {@code a_fuggoleges_toszogalos_mutyur_helye}, {@code a_beallitasokat_tartalmazo_felulet_elrejtve})
 * az eredeti Lazarus-felület ablakkezeléséhez tartoztak; itt nincs értelmük, és
 * nem is teszünk úgy, mintha lenne.
 */
public record Settings(
        boolean sConjunctionAnceps,
        boolean letterSyllables,
        boolean explainUnstressed,
        boolean multipleMatches,
        boolean assonanceAsRhyme,
        boolean showIctus,
        boolean shortWordsAnceps,
        boolean allowSynizesis,
        boolean wordFinalConsonantAnceps,
        boolean wordInitialStressLengthens) {

    public static final String S_CONJUNCTION_ANCEPS = "az_s_kotoszo_kozombos";
    public static final String LETTER_SYLLABLES = "az_abece_betuinek_kulon_szotag";
    public static final String EXPLAIN_UNSTRESSED = "emberi_nyelvu_mit_tudok";
    public static final String MULTIPLE_MATCHES = "egynel_tobb_telitalalat_keresese";
    public static final String ASSONANCE_AS_RHYME = "az_asszonanc_rimkent_valo_kezelese";
    public static final String SHOW_ICTUS = "latszik_az_utemhangsuly_a_gorogon";
    public static final String SHORT_WORDS_ANCEPS = "a_rovid_kotoszok_kozombosek";
    public static final String ALLOW_SYNIZESIS = "a_gorog_diftongusok_osszevonhatok";
    public static final String WORD_FINAL_CONSONANT_ANCEPS = "a_szovegi_massalhangzo_kozosse_tesz";
    public static final String WORD_INITIAL_STRESS = "a_szokezdo_hangsuly_nyujthat";

    /** A beágyazott adatbázis {@code !} sorai szerinti alapállapot. */
    public static Settings fromDatabase(Map<String, String> raw) {
        return new Settings(
                flag(raw, S_CONJUNCTION_ANCEPS, true),
                flag(raw, LETTER_SYLLABLES, false),
                flag(raw, EXPLAIN_UNSTRESSED, true),
                flag(raw, MULTIPLE_MATCHES, true),
                flag(raw, ASSONANCE_AS_RHYME, false),
                flag(raw, SHOW_ICTUS, false),
                flag(raw, SHORT_WORDS_ANCEPS, true),
                flag(raw, ALLOW_SYNIZESIS, true),
                flag(raw, WORD_FINAL_CONSONANT_ANCEPS, true),
                flag(raw, WORD_INITIAL_STRESS, false));
    }

    private static boolean flag(Map<String, String> raw, String key, boolean fallback) {
        String v = raw.get(key);
        if (v == null) {
            return fallback;
        }
        return "1".equals(v.trim());
    }

    /** Kapcsolónkénti felülírás — ezt hívja a REST-réteg. Ismeretlen kulcs hibát dob. */
    public Settings with(Map<String, Boolean> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return this;
        }
        Map<String, Boolean> current = asMap();
        for (Map.Entry<String, Boolean> e : overrides.entrySet()) {
            if (!current.containsKey(e.getKey())) {
                throw new IllegalArgumentException("Ismeretlen beállítás: " + e.getKey());
            }
            if (e.getValue() != null) {
                current.put(e.getKey(), e.getValue());
            }
        }
        return new Settings(
                current.get(S_CONJUNCTION_ANCEPS),
                current.get(LETTER_SYLLABLES),
                current.get(EXPLAIN_UNSTRESSED),
                current.get(MULTIPLE_MATCHES),
                current.get(ASSONANCE_AS_RHYME),
                current.get(SHOW_ICTUS),
                current.get(SHORT_WORDS_ANCEPS),
                current.get(ALLOW_SYNIZESIS),
                current.get(WORD_FINAL_CONSONANT_ANCEPS),
                current.get(WORD_INITIAL_STRESS));
    }

    public Map<String, Boolean> asMap() {
        Map<String, Boolean> m = new LinkedHashMap<>();
        m.put(S_CONJUNCTION_ANCEPS, sConjunctionAnceps);
        m.put(LETTER_SYLLABLES, letterSyllables);
        m.put(EXPLAIN_UNSTRESSED, explainUnstressed);
        m.put(MULTIPLE_MATCHES, multipleMatches);
        m.put(ASSONANCE_AS_RHYME, assonanceAsRhyme);
        m.put(SHOW_ICTUS, showIctus);
        m.put(SHORT_WORDS_ANCEPS, shortWordsAnceps);
        m.put(ALLOW_SYNIZESIS, allowSynizesis);
        m.put(WORD_FINAL_CONSONANT_ANCEPS, wordFinalConsonantAnceps);
        m.put(WORD_INITIAL_STRESS, wordInitialStressLengthens);
        return m;
    }

    /** Emberi nyelvű leírás a felülethez. */
    public static String describe(String key) {
        return switch (key) {
            case S_CONJUNCTION_ANCEPS -> "Az „s” kötőszó közömbös (mássalhangzója elhagyható)";
            case LETTER_SYLLABLES -> "A magában álló betűk külön szótagot alkotnak (b → bé)";
            case EXPLAIN_UNSTRESSED -> "Jelölje a hangsúlytalan szavakat";
            case MULTIPLE_MATCHES -> "Egynél több teljes találat keresése";
            case ASSONANCE_AS_RHYME -> "Az asszonánc rímnek számít";
            case SHOW_ICTUS -> "Látszik az ütemhangsúly (iktus) a metrumsoron";
            case SHORT_WORDS_ANCEPS -> "A rövid, nyílt szótagú kötőszók és névmások közösek (ha, de, te, mi…)";
            case ALLOW_SYNIZESIS -> "A görög-latin nevek eu/au kapcsolata egy szótagnak is vehető";
            case WORD_FINAL_CONSONANT_ANCEPS ->
                "Magánhangzó előtt a szóvégi mássalhangzó zárhatja a szótagot (latinos hagyomány) — közös";
            case WORD_INITIAL_STRESS ->
                "Költői licencia: a szókezdő hangsúly megnyújthatja a rövid szótagot (Íliász: „Haragot”)";
            default -> key;
        };
    }
}
