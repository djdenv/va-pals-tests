package com.paraxialtech.vapals;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SamiBackgroundTest {
    private final WebDriver driver = new HtmlUnitDriver();
    private final Set<String> ignoreFields = ImmutableSet.of("sbwcos"); //Temporarily ignore these fields so remaining tests can run.

    @SuppressWarnings("deprecation")
    private String randomString() {
        return RandomStringUtils.randomAscii(5);
    }

    private List<WebElement> findElements(final WebDriver driver, final String selector) {
        return driver.findElements(By.cssSelector(selector)).stream()
                .filter(WebElement::isEnabled)
                .filter(webElement -> !ignoreFields.contains(webElement.getAttribute("name")))
                .collect(Collectors.toList());
    }

    @Test
    public void backgroundTest() throws Exception {
        final Map<String, String> textValues = new HashMap<>();
        final Map<String, String> radioValues = new HashMap<>();
        final Map<String, String> checkboxValues = new HashMap<>();
        final Map<String, String> selectValues = new HashMap<>();
        final Map<String, String> textAreaValues = new HashMap<>();

        final String baseUrl = "http://vendev.vistaplex.org:9080/form?form=sbform&studyId=PARAXIAL01";

        driver.get(baseUrl);

        //set values in text inputs
        final List<WebElement> textElements = findElements(driver, "input[type='text']");
        textElements.forEach(webElement -> {
            final String elementName = webElement.getAttribute("name");
            final String elementValue = randomString();
            webElement.clear();
            webElement.sendKeys(elementValue);
            textValues.put(elementName, elementValue);
        });

        //select values in all dropdowns
        final List<WebElement> selectElements = findElements(driver, "select");
        selectElements.forEach(webElement -> {
            final List<WebElement> options = webElement.findElements(By.tagName("option"));
            final WebElement selectedOption = options.get(RandomUtils.nextInt(0, options.size()));
            selectedOption.click();
            selectValues.put(webElement.getAttribute("name"), selectedOption.getAttribute("value"));

        });

        //set values for textarea input
        final List<WebElement> textAreaElements = findElements(driver, "textarea");
        textAreaElements.forEach(webElement -> {
            final String value = randomString() + "\n" + randomString() + "\n";
            webElement.clear();
            webElement.sendKeys(value);
            textAreaValues.put(webElement.getAttribute("name"), value);
        });

        //find all radios
        final List<WebElement> radioElements = findElements(driver, "input[type='radio']");
        //unique radio groups
        final Set<String> radioElementGroups = radioElements.stream().map(webElement -> webElement.getAttribute("name")).distinct().collect(Collectors.toSet());
        //select random radio option for each grouping.
        radioElementGroups.forEach(radioGroupName -> {
                    final List<WebElement> radioOptions = radioElements.stream().filter(webElement -> webElement.getAttribute("name").equals(radioGroupName)).collect(Collectors.toList());
                    final WebElement chosen = radioOptions.get(RandomUtils.nextInt(0, radioOptions.size()));
                    chosen.click();
                    radioValues.put(radioGroupName, chosen.getAttribute("value"));
                }
        );

        //check all checkboxes
        final List<WebElement> checkboxElements = findElements(driver, "input[type='checkbox']");
        checkboxElements.forEach(webElement -> {
            webElement.click();
            checkboxValues.put(webElement.getAttribute("name"), webElement.getAttribute("value"));
        });


        driver.findElement(By.cssSelector("input[type='submit']")).submit();

        //ASSERTIONS
        driver.navigate().to(baseUrl); //reload the initial page

        final String pageTitle = driver.getTitle();
        assertNotNull(pageTitle); //TODO test this against something, should be actual patient name.


        textValues.forEach((fieldName, submittedValue) -> {
            final String actual = driver.findElement(By.name(fieldName)).getAttribute("value");
            assertThat("Incorrect value in text field " + fieldName, actual, is(submittedValue));
        });

        selectValues.forEach((fieldName, submittedValue) -> {
            final WebElement dropdown = driver.findElement(By.name(fieldName));
            assertNotNull("No dropdown by name of " + fieldName, dropdown);

            final WebElement selectedOption = dropdown.findElement(By.cssSelector("option[selected]"));
            assertNotNull("No selected option for dropdown " + fieldName, selectedOption);

            final String actual = selectedOption.getAttribute("value");
            assertThat("Incorrect value in dropdown field " + fieldName, actual, is(submittedValue));
        });


        textAreaValues.forEach((fieldName, submittedValue) -> {
            final WebElement textarea = driver.findElement(By.name(fieldName));
            assertNotNull("No textarea by name of " + fieldName, textarea);
            assertThat("Incorrect value in textarea field " + fieldName, textarea.getText(), is(submittedValue.trim()));
        });

        radioValues.forEach((fieldName, submittedValue) -> {
            final WebElement actualSelected = driver.findElement(By.cssSelector("input[type=radio][name=" + fieldName + "]:checked"));
            assertNotNull("No value selected for radio field " + fieldName, actualSelected);
            final String actual = actualSelected.getAttribute("value");
            assertThat("Incorrect value in radio field " + fieldName, actual, is(submittedValue));
        });

        checkboxValues.forEach((fieldName, submittedValue) -> {
            final WebElement checkbox = driver.findElement(By.cssSelector("input[type='checkbox'][name='" + fieldName + "'][value='" + submittedValue + "']"));
            assertNotNull("No checkbox by name of " + fieldName, checkbox);
            final String checked = checkbox.getAttribute("checked");
            assertThat("Checkbox " + fieldName + " should be checked", checked, is("true"));
        });


        driver.close();

    }
}
