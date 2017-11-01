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
import net.sf.expectit.matcher.Matchers;

public class WebExpectTest {
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
            Expect expect = new ExpectBuilder()
                    .withOutput(channel.getOutputStream())
                    .withInputs(channel.getInputStream(), channel.getExtInputStream())
                    .withEchoOutput(System.out)
                    .withEchoInput(System.err)
//                    .withInputFilters(removeColors(), removeNonPrintable())
//                    .withExceptionOnFailure()
                    .withTimeout(10, TimeUnit.SECONDS)
                    .build();

//            expect.expect(contains("[RETURN]"));
            Result r = expect.expect(Matchers.contains("~$$"));
            if (r.isSuccessful()) {
                System.out.println("found a command prompt...");
            }
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
