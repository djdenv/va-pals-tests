package com.paraxialtech.vapals;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.Result;
import net.sf.expectit.filter.Filters;
import net.sf.expectit.matcher.Matcher;
import net.sf.expectit.matcher.Matchers;

public class WebExpectTest {
    private static final String URL = "http://avicenna.vistaexpertise.net:9080/form?form=sbform&studyId=%s";
    private static final String STUDY_ID = "XX0001"; //"PARAXIAL01"';
    private static final String baseUrl = "http://avicenna.vistaexpertise.net:9080/form?form=sbform&studyid=PARAXIAL01";
//    private static WebDriver driver;

//    @BeforeClass
//    public static void setUp() {
//        ChromeDriverManager.getInstance().setup();
//        if (System.getProperty("headless") != null || GraphicsEnvironment.isHeadless()) {
//            ChromeOptions o = new ChromeOptions();
//            o.addArguments("headless");
//            driver = new ChromeDriver(o);
//        }
//        else {
//            driver = new ChromeDriver();
//        }
//    }
//
//    @AfterClass
//    public static void tearDown() throws Exception {
//        driver.quit();
//    }

//    @Before
//    public void before() throws Exception {
//        //get the initial page and determine fields we'll be testing.
//        driver.navigate().to(baseUrl);
//    }

    public static void main(final String[] argv) {
        WebExpectTest test = new WebExpectTest();
//        System.out.println("thsi is running now");
        test.testExpectLogin();
    }

    public void testExpectLogin() {
        Session session = null;
        Channel channel = null;

        try {
            // 1) Login
            JSch jSch = new JSch();
            jSch.addIdentity("../../.ssh/id_rsa_paraxial");
            session = jSch.getSession("kjpowers", "avicenna.vistaexpertise.net");
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("shell");
            channel.connect();
            System.out.println("connected...");

            // 2) Set up the Expect object
            Expect expect = new ExpectBuilder()
                    .withOutput(channel.getOutputStream())
                    .withInputs(channel.getInputStream(), channel.getExtInputStream())
                    .withEchoOutput(System.out)
                    .withEchoInput(System.err)
                    .withInputFilters(Filters.removeColors(), Filters.removeNonPrintable())
                    .withExceptionOnFailure()
                    .withTimeout(10, TimeUnit.SECONDS)
                    .build();

            // 3) Get to FileMan
            expect.expect(Matchers.contains("~$f"));
            expect.sendLine("osehra");

            expect.expect(Matchers.contains("~$"));
            expect.sendLine("mumps -dir");

            expect.expect(Matchers.contains(">"));
            expect.sendLine("SET DUZ=1");

            expect.expect(Matchers.contains(">"));
            expect.sendLine("DO Q^DI");

            // 4) Step through the SAMI BACKGROUND form
            expect.expect(Matchers.contains("Select OPTION:"));
            expect.sendLine("1");

            expect.expect(Matchers.contains("Input to what File:"));
            expect.sendLine("SAMI BACKGROUND");

            expect.expect(Matchers.contains("EDIT WHICH FIELD:"));
            expect.sendLine("ALL");

            expect.expect(Matchers.contains("Select SAMI BACKGROUND STUDY ID:"));
            expect.sendLine(STUDY_ID);

            Matcher<Result> matcherDone = Matchers.contains("Select OPTION:");
            Matcher<Result> matcherItem = Matchers.regexp("[\r\n]([^:]+):(?: (.+)//)?");
            while (true) {
                Result result = expect.expect(Matchers.anyOf(matcherDone, matcherItem));
                if (matcherDone.matches(result.group(), false).isSuccessful()) {
                    break;
                }

                System.out.print(">>" + result.groupCount() + ">>");
                for (int i = 1 ; i <= result.groupCount() ; i++) {
                    System.out.print(result.group(i) + ",");
                }
                System.out.println();

                expect.sendLine();
            }
//          expect.expect(Matchers.contains("Select OPTION:"));
            expect.sendLine("^");

            // 5) Quit
            expect.expect(Matchers.contains(">"));
            expect.sendLine("HALT");

            expect.expect(Matchers.contains("~$"));
            expect.sendLine("exit");

            expect.expect(Matchers.contains("~$"));
            expect.sendLine("exit");

            expect.expect(Matchers.eof());
//            expect.sendLine();
//            String ipAddress = expect.expect(regexp("Trying (.*)\\.\\.\\.")).group(1);
//            System.out.println("Captured IP: " + ipAddress);
//            expect.expect(contains("login:"));
//            expect.sendLine("new");
//            expect.expect(contains("(Y/N)"));
//            expect.send("N");
//            expect.expect(regexp(": $"));
            expect.close();
        } catch (JSchException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
}
