package com.paraxialtech.vapals;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalQueries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;

import com.google.common.collect.ImmutableSet;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import io.github.bonigarcia.wdm.ChromeDriverManager;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.Result;
import net.sf.expectit.matcher.Matcher;
import net.sf.expectit.matcher.Matchers;

class SamiBackgroundWebFilemanCompareTest {
    private static WebDriver driver;

    private static final String URL = "http://%s:9080/form?form=sbform&studyId=%s";
    private static final String SERVER = "avicenna.vistaexpertise.net";
    private static final String SSH_CERT = "../../.ssh/id_rsa_paraxial";    // TODO
    private static final String SSH_USER = "kjpowers";                      // TODO
    private static final List<String> STUDY_IDS = Arrays.asList( /*"PARAXIAL01",*/
                                                                 "XX0001" );
    private static final String FILEMAN_FORM = "SAMI BACKGROUND";
    private static final Map<String, String> WEB_FILEMAN_KEY_MAP = new HashMap<String, String>();
    private static final DateTimeFormatter WEB_DATE_FORMAT = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd/MMM/yyyy").toFormatter();
    private static final DateTimeFormatter FILEMAN_DATE_FORMAT = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM d[d],yyyy").toFormatter();

    static {
        WEB_FILEMAN_KEY_MAP.put("sbdob",    "DATE OF BIRTH");
        WEB_FILEMAN_KEY_MAP.put("sbage",    "AGE");
        WEB_FILEMAN_KEY_MAP.put("sbdop",    "INTAKE DATE");
        WEB_FILEMAN_KEY_MAP.put("sbocc",    "OCCUPATION");
        WEB_FILEMAN_KEY_MAP.put("sboccc",   "OCCUPATION CODE");
        WEB_FILEMAN_KEY_MAP.put("sbsex",    "SEX");
        WEB_FILEMAN_KEY_MAP.put("sbph",     "HEIGHT");
        WEB_FILEMAN_KEY_MAP.put("sbphu",    "HEIGHT UNITS");
        WEB_FILEMAN_KEY_MAP.put("sbpw",     "WEIGHT");
        WEB_FILEMAN_KEY_MAP.put("sbpwu",    "WEIGHT UNITS");
        WEB_FILEMAN_KEY_MAP.put("sbbmi",    "BMI");
        WEB_FILEMAN_KEY_MAP.put("sbet",     "ETHNICITY");
        WEB_FILEMAN_KEY_MAP.put("sbrc",     "RACE");
        WEB_FILEMAN_KEY_MAP.put("sbrcs",    "RACE (SPECIFY)");
        WEB_FILEMAN_KEY_MAP.put("sbed",     "LEVEL OF EDUCATION");
        WEB_FILEMAN_KEY_MAP.put("sbmly",    "MILITARY");
        WEB_FILEMAN_KEY_MAP.put("sbmlyo",   "BRANCH");
        WEB_FILEMAN_KEY_MAP.put("sbmeq",    "SCAN ORDERED");
        WEB_FILEMAN_KEY_MAP.put("sbopnpi",  "PRACTITIONER NPI");
        WEB_FILEMAN_KEY_MAP.put("sbdsd",    "SHARED DECISION");
        WEB_FILEMAN_KEY_MAP.put("sboppy",   "PRACTITIONER PACK YEARS");
        WEB_FILEMAN_KEY_MAP.put("sbopss",   "PRACTITIONER SMOKING STATUS");
        WEB_FILEMAN_KEY_MAP.put("sbopqy",   "PRACTITIONER QUIT YEARS");
        WEB_FILEMAN_KEY_MAP.put("sboas",    "PRACTITIONER ASYMPTOMATIC");
        WEB_FILEMAN_KEY_MAP.put("sbopci",   "CLINICAL INFORMATION");
        WEB_FILEMAN_KEY_MAP.put("sbfc",     "FAMILY HISTORY OF LUNG CANCER");
        WEB_FILEMAN_KEY_MAP.put("sbfcm",    "LUNG CANCER FATHER");
        WEB_FILEMAN_KEY_MAP.put("sbfcf",    "LUNG CANCER MOTHER");
        WEB_FILEMAN_KEY_MAP.put("sbfcs",    "LUNG CANCER SIBLING");
        WEB_FILEMAN_KEY_MAP.put("sbhco",    "ALL OTHER CANCERS");
        WEB_FILEMAN_KEY_MAP.put("sbhcdod",  "OTHER CANCERS WHEN");
        WEB_FILEMAN_KEY_MAP.put("snhcpbo",  "OTHER CANCERS SITE");
        WEB_FILEMAN_KEY_MAP.put("smbpa",    "ASTHMA");
        WEB_FILEMAN_KEY_MAP.put("sbmpat",   "ASTHMA TREATED");
        WEB_FILEMAN_KEY_MAP.put("sbmpc",    "EMPHYSEMA OR COPD");
        WEB_FILEMAN_KEY_MAP.put("sbmpht",   "HYPERTENSION");
        WEB_FILEMAN_KEY_MAP.put("sbmphtt",  "HYPERTENSION TREATED");
        WEB_FILEMAN_KEY_MAP.put("sbmphtsw", "HYPERTENSION SINCE");
        WEB_FILEMAN_KEY_MAP.put("sbmphthv", "HYPERTENSION HIGHEST VALUE");
        WEB_FILEMAN_KEY_MAP.put("sbmphc",   "HIGH CHOLESTEROL");
        WEB_FILEMAN_KEY_MAP.put("sbmpct",   "HIGH CHOLESTEROL TREATED");
        WEB_FILEMAN_KEY_MAP.put("sbmpas",   "ANGIOPLASTY OR STENT");
        WEB_FILEMAN_KEY_MAP.put("sbmpasw",  "ANGIOPLASTY WHEN");
        WEB_FILEMAN_KEY_MAP.put("sbmpast",  "ANGIOPLASTY WHERE");
        WEB_FILEMAN_KEY_MAP.put("sbmpmi",   "MI");
        WEB_FILEMAN_KEY_MAP.put("sbmpmid",  "MI WHEN");
        WEB_FILEMAN_KEY_MAP.put("sbmpmiw",  "MI WHERE");
        WEB_FILEMAN_KEY_MAP.put("sbmps",    "STROKE");
        WEB_FILEMAN_KEY_MAP.put("sbmpsd",   "STROKE WHEN");
        WEB_FILEMAN_KEY_MAP.put("sbmpsw",   "STROKE WHERE");
        WEB_FILEMAN_KEY_MAP.put("sbmppv",   "PERIPHERAL VASCULAR DISEASE");
        WEB_FILEMAN_KEY_MAP.put("sbmpd",    "DIABETES");
        WEB_FILEMAN_KEY_MAP.put("sbmpdw",   "DIABETES AGE");
        WEB_FILEMAN_KEY_MAP.put("sbmpdt",   "DIABETES TREATED");
        WEB_FILEMAN_KEY_MAP.put("sbmpld",   "LIVER DISEASE");
        WEB_FILEMAN_KEY_MAP.put("sbmplds",  "LIVER SEVERITY");
        WEB_FILEMAN_KEY_MAP.put("sbmprd",   "RENAL DISEASE");
        WEB_FILEMAN_KEY_MAP.put("sbmprds",  "RENAL SEVERITY");
        WEB_FILEMAN_KEY_MAP.put("sbwc",     "LUNG CANCER SYMPTOMS");
        WEB_FILEMAN_KEY_MAP.put("sbact",    "CHEST CT WHEN");
        WEB_FILEMAN_KEY_MAP.put("sbahcl",   "CHEST CT WHERE");
        WEB_FILEMAN_KEY_MAP.put("sbahpft",  "PULMONARY FUNCTION TEST");
        WEB_FILEMAN_KEY_MAP.put("sbfev1",   "FEV1 (L/s)");
        WEB_FILEMAN_KEY_MAP.put("sbfvc",    "FVC (L)");
        WEB_FILEMAN_KEY_MAP.put("sbffr",    "FEV1/FVC (%)");
        WEB_FILEMAN_KEY_MAP.put("sbcop",    "DIFFUSION CAPACITY");
        WEB_FILEMAN_KEY_MAP.put("sbaha",    "ASBESTOS EXPOSURE");
        WEB_FILEMAN_KEY_MAP.put("sbahaoo",  "ASBESTOS OTHER SPECIFY");
        WEB_FILEMAN_KEY_MAP.put("sbsas",    "SMOKING AGE");
        WEB_FILEMAN_KEY_MAP.put("sbshsa",   "SMOKED IN PAST MONTH");
        WEB_FILEMAN_KEY_MAP.put("sbsdlcd",  "FORMER DAYS PER WEEK");
        WEB_FILEMAN_KEY_MAP.put("sbfppd",   "FORMER PPD");
        WEB_FILEMAN_KEY_MAP.put("sbfdur",   "FORMER DURATION");
        WEB_FILEMAN_KEY_MAP.put("sbcdpw",   "CURRENT DAYS PER WEEK");
        WEB_FILEMAN_KEY_MAP.put("sbcppd",   "CURRENT PPD");
        WEB_FILEMAN_KEY_MAP.put("sbcdur",   "CURRENT DURATION");
        WEB_FILEMAN_KEY_MAP.put("sbntpy",   "TOTAL PACK YEARS");
        WEB_FILEMAN_KEY_MAP.put("sbqttq",   "TRIED TO QUIT");
        WEB_FILEMAN_KEY_MAP.put("sbqttqtb", "TRIED HOW MANY TIMES");
        WEB_FILEMAN_KEY_MAP.put("sbqly2",   "QUIT PAST 12 MONTHS");
        WEB_FILEMAN_KEY_MAP.put("sbqst",    "THINKING OF QUITTING");
        WEB_FILEMAN_KEY_MAP.put("sbcpd",    "CESSATION PACKET");
        WEB_FILEMAN_KEY_MAP.put("sbhsyh",   "SMOKING ALLOWED HOME");
        WEB_FILEMAN_KEY_MAP.put("sbmsy",    "MOTHER SMOKE UNDER 7");
        WEB_FILEMAN_KEY_MAP.put("sbmst",    "MOTHER SMOKE 7-18");
        WEB_FILEMAN_KEY_MAP.put("sbosy",    "HOME CHILDHOOD OTHER");
        WEB_FILEMAN_KEY_MAP.put("sbslws",   "SECONDHAND CURRENT");
        WEB_FILEMAN_KEY_MAP.put("sbhso",    "ECONDHAND HOME ADULT");
        WEB_FILEMAN_KEY_MAP.put("sbsfb1",   "GENERAL HEALTH");
        WEB_FILEMAN_KEY_MAP.put("sbsfb2",   "HEALTH ACTIVITY LIMITS");
        WEB_FILEMAN_KEY_MAP.put("sbsfb3",   "HEALTH DAILY WORK");
        WEB_FILEMAN_KEY_MAP.put("sbsfb4",   "BODILY PAIN");
        WEB_FILEMAN_KEY_MAP.put("sbsfb5",   "ENERGY");
        WEB_FILEMAN_KEY_MAP.put("sbsfb6",   "HEALTH SOCIAL LIMITS");
        WEB_FILEMAN_KEY_MAP.put("sbsfb7",   "HEALTH EMOTIONAL");
        WEB_FILEMAN_KEY_MAP.put("sbsfb8",   "HEALTH DAILY ACTIVITIES");
        WEB_FILEMAN_KEY_MAP.put("sbcfs",    "CONSENT SIGNED");
        WEB_FILEMAN_KEY_MAP.put("sbdoc",    "CONSENT DATE");
        WEB_FILEMAN_KEY_MAP.put("sbioc",    "CONSENT OBTAINED BY");
    }

    @BeforeAll
    public static void setUp() {
        ChromeDriverManager.getInstance().setup();
        if (System.getProperty("headless") != null || GraphicsEnvironment.isHeadless()) {
            ChromeOptions o = new ChromeOptions();
            o.addArguments("headless");
            driver = new ChromeDriver(o);
        }
        else {
            driver = new ChromeDriver();
        }
    }

    @AfterAll
    public static void tearDown() throws Exception {
        driver.quit();
    }

    private final Set<String> ignoreFields = ImmutableSet.of(); //Temporarily ignore these fields so remaining tests can run. "sbwcos"

    @TestFactory
    Iterator<DynamicTest> testWebValuesMatchFilemanValues() {
        List<DynamicTest> tests = new ArrayList<>();

        for (final String studyId : STUDY_IDS) {
            final Map<String, String> filemanValuesBefore = getFilemanValues(studyId);
            final Map<String, String> webValuesBefore = getWebValues(studyId);

            // TODO: use a study id that is known to be suitable for this test
//            final Map<String, String> filemanValuesAfter = changeFilemanValues(studyId);
//            final Map<String, String> webValuesAfter = getWebValues(studyId);

            Executable executable = new Executable() {
                @Override public void execute() throws Throwable {
                    final List<String> errors = compareWebFilemanValues(webValuesBefore, filemanValuesBefore);
                    errors.add(0, "Web/Fileman ORIGINAL values for study:'" + studyId + "' do not match (" + errors.size() + ").");
                    MatcherAssert.assertThat(String.join("\n", errors.toArray(new String[0])), errors.size(), CoreMatchers.is(0));
                }
            };
            tests.add(DynamicTest.dynamicTest("Compare web/Fileman form values for study=" + studyId, executable));

//            executable = new Executable() {
//                @Override public void execute() throws Throwable {
//                    final List<String> errors = compareWebFilemanValues(webValuesAfter, filemanValuesAfter);
//                    errors.add(0, "Web/Fileman CHANGED values for study:'" + studyId + "' do not match (" + errors.size() + ").");
//                    MatcherAssert.assertThat(String.join("\n", errors.toArray(new String[0])), errors.size(), CoreMatchers.is(0));
//                }
//            };
//            tests.add(DynamicTest.dynamicTest("Compare web/Fileman form values for study=" + studyId, executable));
        }

        return tests.iterator();
    }

    private List<String> compareWebFilemanValues(final Map<String, String> webValues, final Map<String, String> filemanValues) {
        final List<String> lstMismatches = new ArrayList<>();

        for (final Entry<String, String> entry : WEB_FILEMAN_KEY_MAP.entrySet()) {
            if (webValues.containsKey(entry.getKey())) {
                if (!filemanValues.containsKey(entry.getValue()) ||
                    !webValues.get(entry.getKey()).equals(filemanValues.get(entry.getValue()))) {
                    lstMismatches.add(
                        new StringBuilder()
                            .append("web value '")
                            .append(webValues.get(entry.getKey()))
                            .append("' (")
                            .append(entry.getKey())
                            .append(") does not equal fileman value '")
                            .append(filemanValues.get(entry.getValue()))
                            .append("' (")
                            .append(entry.getValue())
                            .append(")")
                            .toString());
                }
            }
        }

        return lstMismatches;
    }

    private Map<String, String> getWebValues(final String studyId) {
        Map<String, String> webValues = new HashMap<>();
        driver.get(String.format(URL, SERVER, studyId));

        for (final WebElement webElement : driver.findElements(By.cssSelector("input,select,textarea"))) {
            String name = webElement.getAttribute("name");
            // Skip elements with no name
            if (name == null || name.trim().length() == 0) {
                continue;
            }
            // Skip radio buttons that are not checked
            if (webElement.getAttribute("type").equals("radio") &&
                !"checked".equals(webElement.getAttribute("checked"))) {
                continue;
            }
            // Get the text of the select option rather than its value
            if ("select".equals(webElement.getTagName())) {
                webValues.put(name, tryParse(new Select(webElement).getFirstSelectedOption().getText(), true));
            }
            else {
                webValues.put(name, tryParse(webElement.getAttribute("value"), true));
            }
        }

        return webValues;
    }

    private Map<String, String> getFilemanValues(final String studyId) {
        Map<String, String> filemanValues = new HashMap<>();

        Session session = null;
        Channel channel = null;

        try {
            // 1) Login
            JSch jSch = new JSch();
            jSch.addIdentity(SSH_CERT);
            session = jSch.getSession(SSH_USER, SERVER);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("shell");
            channel.connect();

            // 2) Set up the Expect object
            Expect expect = new ExpectBuilder()
                    .withOutput(channel.getOutputStream())
                    .withInputs(channel.getInputStream(), channel.getExtInputStream())
//                    .withEchoOutput(System.out)
//                    .withEchoInput(System.err)
//                    .withInputFilters(removeColors(), removeNonPrintable())
                    .withExceptionOnFailure()
                    .withTimeout(10, TimeUnit.SECONDS)
                    .build();

            // 3) Get to FileMan
            expect.expect(Matchers.contains("~$"));
            expect.sendLine("osehra");

            expect.expect(Matchers.contains("~$"));
            expect.sendLine("mumps -dir");

            expect.expect(Matchers.contains(">"));
            expect.sendLine("SET DUZ=1");

            expect.expect(Matchers.contains(">"));
            expect.sendLine("DO Q^DI");

            // 4) Enter/edit file entries
            expect.expect(Matchers.contains("Select OPTION:"));
            expect.sendLine("1");

            expect.expect(Matchers.contains("Input to what File:"));
            expect.sendLine(FILEMAN_FORM);

            expect.expect(Matchers.contains("EDIT WHICH FIELD:"));
            expect.sendLine("ALL");

            expect.expect(Matchers.contains("Select SAMI BACKGROUND STUDY ID:"));
            expect.sendLine(studyId);

            // 5) Loop through each entry
            Matcher<Result> matcherDone = Matchers.contains("Select OPTION:");
            Matcher<Result> matcherItem = Matchers.regexp("[\r\n]+([^:]+):(?: (.+)//)?");
            while (true) {
                Result result = expect.expect(Matchers.anyOf(matcherDone, matcherItem));
                if (matcherDone.matches(result.group(), false).isSuccessful()) {
                    break;
                }

                filemanValues.put(result.group(1), result.groupCount() == 1 ? "" : tryParse(result.group(2), false));
                expect.sendLine();
            }
//            expect.expect(Matchers.contains("Select OPTION:"));
            expect.sendLine("^");

            // 6) Log out
            expect.expect(Matchers.contains(">"));
            expect.sendLine("HALT");

            expect.expect(Matchers.contains("~$"));
            expect.sendLine("exit");

            expect.expect(Matchers.contains("~$"));
            expect.sendLine("exit");

            expect.expect(Matchers.eof());

            expect.close();
        } catch (JSchException e) {
            Assertions.fail(String.format("Failed to connect to server='%s' with cert='%s' and user='%s'", SERVER, SSH_CERT, SSH_USER));
        } catch (IOException e) {
            Assertions.fail(String.format("Error communicating with server."));
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }

        return filemanValues;
    }

    private Map<String, String> changeFilemanValues(final String studyId) {
        Map<String, String> filemanValues = new HashMap<>();

        Session session = null;
        Channel channel = null;

        try {
            // 1) Login
            JSch jSch = new JSch();
            jSch.addIdentity(SSH_CERT);
            session = jSch.getSession(SSH_USER, SERVER);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("shell");
            channel.connect();

            // 2) Set up the Expect object
            Expect expect = new ExpectBuilder()
                    .withOutput(channel.getOutputStream())
                    .withInputs(channel.getInputStream(), channel.getExtInputStream())
//                    .withEchoOutput(System.out)
//                    .withEchoInput(System.err)
//                    .withInputFilters(removeColors(), removeNonPrintable())
                    .withExceptionOnFailure()
                    .withTimeout(10, TimeUnit.SECONDS)
                    .build();

            // 3) Get to FileMan
            expect.expect(Matchers.contains("~$"));
            expect.sendLine("osehra");

            expect.expect(Matchers.contains("~$"));
            expect.sendLine("mumps -dir");

            expect.expect(Matchers.contains(">"));
            expect.sendLine("SET DUZ=1");

            expect.expect(Matchers.contains(">"));
            expect.sendLine("DO Q^DI");

            // 4) Enter/edit file entries
            expect.expect(Matchers.contains("Select OPTION:"));
            expect.sendLine("1");

            expect.expect(Matchers.contains("Input to what File:"));
            expect.sendLine(FILEMAN_FORM);

            expect.expect(Matchers.contains("EDIT WHICH FIELD:"));
            expect.sendLine("ALL");

            expect.expect(Matchers.contains("Select SAMI BACKGROUND STUDY ID:"));
            expect.sendLine(studyId);

            // 5) Loop through each entry, reverse those with values
            Matcher<Result> matcherDone = Matchers.contains("Select OPTION:");
            Matcher<Result> matcherItem = Matchers.regexp("[\r\n]+([^:]+):(?: (.+)//)?");
            while (true) {
                Result result = expect.expect(Matchers.anyOf(matcherDone, matcherItem));
                if (matcherDone.matches(result.group(), false).isSuccessful()) {
                    break;
                }

                final String value = result.groupCount() == 1 ? "" : new StringBuilder(tryParse(result.group(2), false)).reverse().toString();
                filemanValues.put(result.group(1), value);
                expect.sendLine(value);
            }
//            expect.expect(Matchers.contains("Select OPTION:"));
            expect.sendLine("^");

            // 6) Log out
            expect.expect(Matchers.contains(">"));
            expect.sendLine("HALT");

            expect.expect(Matchers.contains("~$"));
            expect.sendLine("exit");

            expect.expect(Matchers.contains("~$"));
            expect.sendLine("exit");

            expect.expect(Matchers.eof());

            expect.close();
        } catch (JSchException e) {
            Assertions.fail(String.format("Failed to connect to server='%s' with cert='%s' and user='%s'", SERVER, SSH_CERT, SSH_USER));
        } catch (IOException e) {
            Assertions.fail(String.format("Error communicating with server."));
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }

        return filemanValues;
    }

    private final String tryParse(String val, final boolean isWeb) {
        // 1) If it's null / empty
        if (val == null ||
            val.trim().isEmpty()) {
            return "";
        }

        // 2) If it's a date
        val = tryParseDate(val, isWeb ? WEB_DATE_FORMAT : FILEMAN_DATE_FORMAT);

        // 3) Done
        return val;
    }

    private final String tryParseDate(final String val, final DateTimeFormatter fmt) {
        if (val == null ) {
            return null;
        }
        try {
            return fmt.parse(val, TemporalQueries.localDate()).format(DateTimeFormatter.ISO_DATE);
        }
        catch (DateTimeParseException e) {
            return val;
        }
    }
}
