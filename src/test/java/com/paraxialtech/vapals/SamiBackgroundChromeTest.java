package com.paraxialtech.vapals;

import io.github.bonigarcia.wdm.ChromeDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.awt.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class SamiBackgroundChromeTest {
    private static final String baseUrl = "http://avicenna.vistaexpertise.net:9080/form?form=sbform&studyid=PARAXIAL01";
    private static WebDriver driver;

    @BeforeClass
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

    @AfterClass
    public static void tearDown() throws Exception {
        driver.quit();
    }

    @Before
    public void before() throws Exception {
        //get the initial page and determine fields we'll be testing.
        driver.navigate().to(baseUrl);
    }

    @Test
    public void testVisitDate() {
        final String fieldName = "sbdop";

        submitField(fieldName, "foobar");
        assertFieldValueIsNotValid(fieldName);

        submitField(fieldName, "25/Oct/2017");
        assertFieldValueEquals(fieldName, "25/Oct/2017");
    }

    @Test
    public void testDateOfBirth() {
        final String fieldName = "sbdob";

        submitField(fieldName, "foobar");
        assertFieldValueIsNotValid(fieldName);

        submitField(fieldName, "25/Oct/2017");
        assertFieldValueEquals(fieldName, "25/Oct/2017");
    }

    @Test
    public void testAge() {

        final String fieldName = "sbage";

        submitField(fieldName, "foobar");
        assertFieldValueIsNotValid(fieldName);

        submitField(fieldName, "45");
        assertFieldValueEquals(fieldName, "45");
    }


    private void submitField(final String fieldName, final String fieldValue) {
        WebElement element = driver.findElement(By.name(fieldName));
        assertNotNull("element " + fieldName + " not found", element);

        element.clear();
        element.sendKeys(fieldValue);
        element.submit();

    }


    private void assertFieldValueEquals(final String fieldName, final String fieldValue) {
        driver.navigate().to(baseUrl);

        //assertions
        Elements elements = Jsoup.parse(driver.getPageSource()).getElementsByAttributeValue("name", fieldName);
        assertThat(elements.size(), is(1));
        assertThat("Incorrect value for field " + fieldName, elements.get(0).val(), is(fieldValue));
    }

    private void assertFieldValueIsNotValid(final String fieldName) {
        //assertions
        Elements elements = Jsoup.parse(driver.getPageSource()).getElementsByAttributeValue("name", fieldName);
        assertThat(elements.size(), is(1));
        elements.get(0).parent().getElementsContainingText("Input invalid");
        assertThat("Expected element indicating " + fieldName + " field was invalid", elements.size(), is(1));
    }
}
