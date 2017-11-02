package com.paraxialtech.vapals;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
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
import java.util.stream.Collectors;

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

    private List<WebElement> findElements(final WebDriver driver, final String selector) {
        return driver.findElements(By.cssSelector(selector)).stream()
                .filter(WebElement::isEnabled)
                .filter(webElement -> !ignoreFields.contains(webElement.getAttribute("name")))
                .collect(Collectors.toList());
    }

    @TestFactory
    Iterator<DynamicTest> testWebValuesMatchFilemanValues() {
        List<DynamicTest> tests = new ArrayList<>();

        for (final String studyId : STUDY_IDS) {
            Executable executable = new Executable() {
                @Override public void execute() throws Throwable {
                    final Map<String, String> webValues = getWebValues(studyId);
                    final Map<String, String> filemanValues = getFilemanValues(studyId);

                    final StringBuilder sbMessage = new StringBuilder();
                    for (final Entry<String, String> entry : getKeyMap().entrySet()) {
                        if (webValues.containsKey(entry.getKey())) {
                            if (!filemanValues.containsKey(entry.getValue()) ||
                                !webValues.get(entry.getKey()).equals(filemanValues.get(entry.getValue()))) {
                                sbMessage
                                    .append("web value '")
                                    .append(webValues.get(entry.getKey()))
                                    .append("' (")
                                    .append(entry.getKey())
                                    .append(") does not equal fileman value '")
                                    .append(filemanValues.get(entry.getValue()))
                                    .append("' (")
                                    .append(entry.getValue())
                                    .append(")\n")
                                    ;
                            }
                        }
                    }

                    MatcherAssert.assertThat("Web/Fileman values do not match.\n" + sbMessage.toString(), sbMessage.length(), CoreMatchers.is(0));
                }
            };
            tests.add(DynamicTest.dynamicTest("Compare web/Fileman form values for study=" + studyId, executable));
        }

        return tests.iterator();
    }

    private Map<String, String> getWebValues(final String studyId) {
        Map<String, String> webValues = new HashMap<>();
        driver.get(String.format(URL, SERVER, studyId));

        for (final WebElement webElement : driver.findElements(By.cssSelector("input,select,textarea"))) {
            String name = webElement.getAttribute("name");
            if (name == null || name.trim().length() == 0) {
                continue;
            }
            webValues.put(name, webElement.getAttribute("value"));
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

                filemanValues.put(result.group(1), result.groupCount() == 1 ? "" : result.group(2));
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

    private final Map<String, String> getKeyMap() {
        final Map<String, String> webFilemanKeyMap = new HashMap<>();
        webFilemanKeyMap.put("sbdob", "DATE OF BIRTH");
        webFilemanKeyMap.put("sbage", "AGE");
        webFilemanKeyMap.put("sbocc", "OCCUPATION");
//        webFilemanKeyMap.put("", "");
//        webFilemanKeyMap.put("", "");
//        webFilemanKeyMap.put("", "");
//        webFilemanKeyMap.put("", "");
//        webFilemanKeyMap.put("", "");
//        webFilemanKeyMap.put("", "");
//        webFilemanKeyMap.put("", "");
//        webFilemanKeyMap.put("", "");
//        webFilemanKeyMap.put("", "");
//        webFilemanKeyMap.put("", "");
//        webFilemanKeyMap.put("", "");
//        webFilemanKeyMap.put("", "");

        return webFilemanKeyMap;
    }
}
