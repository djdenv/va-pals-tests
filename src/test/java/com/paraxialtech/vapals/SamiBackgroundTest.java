package com.paraxialtech.vapals;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.RandomStringGenerator;
import org.hamcrest.CoreMatchers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

class SamiBackgroundTest {
    private final WebDriver driver = new HtmlUnitDriver();

    private final String baseUrl = "http://avicenna.vistaexpertise.net:9080/form?form=sbform&studyid=PARAXIAL01";
    //    private final String baseUrl = "http://54.172.139.35:9080/form?form=sbform&studyId=PARAXIAL01";
    //    private final String baseUrl = "http://vendev.vistaplex.org:9080/form?form=sbform&studyId=PARAXIAL01";
//    private final String baseUrl = "http://vendev.vistaplex.org:9080/form?form=sbform&studyId=PARAXIAL01";
    private final Set<String> ignoreFields = ImmutableSet.of(); //Temporarily ignore these fields so remaining tests can run. "sbwcos"

    private static final String ASCII_VALUE = "a Z  0!\\\"#$%^&*()-./<>=?@[]_`{}~";

    private List<WebElement> findElements(final WebDriver driver, final String selector) {
        return driver.findElements(By.cssSelector(selector)).stream()
                .filter(WebElement::isEnabled)
                .filter(webElement -> !ignoreFields.contains(webElement.getAttribute("name")))
                .collect(Collectors.toList());
    }


    private String randomAsciiExt() {
//        return new RandomStringGenerator.Builder().withinRange(32, 127).build().generate(length); //basic ASCII
        return new RandomStringGenerator.Builder().withinRange(32, 255).build().generate(30);
    }

    @BeforeEach
    void setUp() {
        //get the initial page and determine fields we'll be testing.
        driver.get(baseUrl);
    }

    @TestFactory
    Iterator<DynamicTest> testBasicAccessibility() {
        final String pageSource = driver.getPageSource();
        final Document doc = Jsoup.parse(pageSource);

        // 1) Get all <input>, <select>, and <textarea> elements
        Elements inputElements = doc.select("input,select,textarea");
        List<DynamicTest> tests = newArrayList();

        // 2) For each element from step 1, add a test to ensure it has a non-empty "name" attribute
        tests.addAll(inputElements.stream().map(element -> DynamicTest.dynamicTest("has name attribute: " + element.toString(), () -> {
            assertThat("name attribute is missing or empty: " + element.toString(), element.attr("name"), CoreMatchers.not(""));
        })).collect(Collectors.toList()));

        // 3) For each element from step 1, add a test to ensure it has a non-empty "id" attribute
        tests.addAll(inputElements.stream().map(element -> DynamicTest.dynamicTest("has id attribute: " + element.toString(), () -> {
            assertThat("id attribute is missing or empty: " + element.toString(), element.attr("id"), CoreMatchers.not(""));
        })).collect(Collectors.toList()));

        // 4) For each element from step 1, add a test to ensure it has exactly one label
        tests.addAll(inputElements.stream().map(element -> DynamicTest.dynamicTest("has label: " + element.toString(), () -> {
            final String elementId = element.attr("id");
            if (StringUtils.isNotBlank(elementId)) {
                //could check for a wrapped label like this: element.parents().stream().anyMatch(parent -> parent.tagName().equals("label")), but that's not a preferred method
                assertThat("field with id=" + elementId + "missing label", doc.getElementsByAttributeValue("for", elementId).size(), is(1));
            }
            else {
                fail("field with id=" + elementId + "missing label");
            }
        })).collect(Collectors.toList()));

        return tests.iterator();
    }

    @TestFactory
    Iterator<DynamicTest> testDateFields() {

        Document doc = Jsoup.parse(driver.getPageSource());

        // 1) Parse the page with jsoup, find the elements with class "ddmmmyyyy", and collect into a list their name attributes
        final List<String> fieldNames = doc.getElementsByClass("ddmmmyyyy").stream().map(element -> element.attr("name")).collect(Collectors.toList());

        // 2) For each name from step 1, add a (set of 3) tests
        return fieldNames.stream().map(textFieldName -> DynamicTest.dynamicTest("testDateField: " + textFieldName, () -> {

            // 2.1) The elements we are testing are date fields; they should give a formatted date after form submission
            final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy"); //TODO: What is the actual expected output format for date fields?

            final String today = LocalDate.now().format(dateFormat);

            // 2.2) We will be testing 3 input values: a formatted date, "T" (today), and "T-1" (yesterday)
            //format of this set is K: input, V: expected output
            Set<Pair<String, String>> tests = Sets.newHashSet(
                    ImmutablePair.of(today, today),
                    ImmutablePair.of("T", today),
                    ImmutablePair.of("T-1", LocalDate.now().minusDays(1).format(dateFormat))
            );

            // 2.3) For each test: find the element (with Selenium), set its value, submit the form, reload the form, read the value & compare to the expected output
            for (Pair<String, String> test : tests) {
                final WebElement textField = driver.findElement(By.name(textFieldName));    // 2.3.1) Find the element (with Selenium)
                String input = test.getKey();
                String expected = test.getValue();
                textField.clear();                                                          // 2.3.2) Set the element's value to be the test input
                textField.sendKeys(input);
                textField.submit();                                                         // 2.3.3) Submit the element's value
                driver.navigate().to(baseUrl);                                              // 2.3.4) Reload the initial page
                assertThat("Incorrect value for field " + textFieldName                     // 2.3.5) Read the new value & compare to the expected value
                           + " when using " + input
                           + " as input",
                           driver.findElement(By.name(textFieldName)).getAttribute("value"),
                           is(expected));
            }

        })).iterator();
    }

    @TestFactory
    Iterator<DynamicTest> testAsciiCharactersForTextFields() {
        // 1) Find <input type="text" name="???"> elements and collect their "name" attribute values
        final List<String> textFieldNames = findElements(driver, "input[type='text'][name]").stream().map(webElement -> webElement.getAttribute("name")).collect(Collectors.toList());

        // 2) Make sure that these elements accept & reflect the pre-defined static ASCII text
        return generateTextTests(textFieldNames, "ASCII - ", () -> ASCII_VALUE);
    }

    @TestFactory
    Iterator<DynamicTest> testRandomCharactersForTextFields() {
        // 1) Find <input type="text" name="???"> elements and collect their "name" attribute values
        final List<String> textFieldNames = findElements(driver, "input[type='text'][name]").stream().map(webElement -> webElement.getAttribute("name")).collect(Collectors.toList());

        // 2) Make sure that these elements accept & reflect randomly-generated ASCII text
        return generateTextTests(textFieldNames, "ASCII - ", this::randomAsciiExt);
    }

    @TestFactory
    Iterator<DynamicTest> testPrintableCharactersForTextFields() {
        // 1) Find <input type="text" name="???"> elements and collect their "name" attribute values
        final List<String> textFieldNames = findElements(driver, "input[type='text'][name]").stream().map(webElement -> webElement.getAttribute("name")).collect(Collectors.toList());

        // 2) Make sure that these elements accept and reflect some pre-defined printable (Unicode) characters
        return generateTextTests(textFieldNames, "Printable - ", () -> "È\u0089q§Òú_" + (char) 138);
    }

    private Iterator<DynamicTest> generateTextTests(final List<String> fieldNames, final String prefix, final Supplier<String> value) {
        // 1) For each element name, add a test to ensure that the element accepts and reflects the given value
        return fieldNames.stream().map(textFieldName -> DynamicTest.dynamicTest(prefix + textFieldName, () -> {
            final WebElement textField = driver.findElement(By.name(textFieldName));    // 1.1) Find the element (with Selenium)
            final String asciiText = value.get();
            assertNotNull(textField, "Could not find field by name " + textFieldName);  // 1.2) Sanity check: make sure we found the element
            textField.clear();                                                          // 1.3) Set the element's value to be the test input
            textField.sendKeys(asciiText);
            textField.submit();                                                         // 1.4) Submit the element's value
            driver.navigate().to(baseUrl);                                              // 1.5) Reload the initial page
            assertThat("Incorrect value in text field " + textFieldName,                // 1.6) Read the new value & compare to the expected value
                       driver.findElement(By.name(textFieldName)).getAttribute("value"),
                       is(asciiText));
        })).iterator();
    }

    /**
     * This is nearly identical to {@link #testAsciiCharactersForTextFields()}
     *
     * @return
     */
    @TestFactory
    Iterator<DynamicTest> testSaveAllText() {
        final List<String> textFieldNames = findElements(driver, "input[type='text']").stream().map(webElement -> webElement.getAttribute("name")).collect(Collectors.toList());
        return textFieldNames.stream().map(textFieldName -> DynamicTest.dynamicTest("Test save text " + textFieldName, () -> {
            final WebElement textField = driver.findElement(By.name(textFieldName));
            final String asciiText = ASCII_VALUE;
            assertNotNull(textField, "Could not find field by name " + textFieldName);
            textField.clear();
            textField.sendKeys(asciiText);
            textField.submit();
            driver.navigate().to(baseUrl); //reload the initial page
            assertThat("Incorrect value in text field " + textFieldName, driver.findElement(By.name(textFieldName)).getAttribute("value"), is(asciiText));
        })).iterator();
    }

    @TestFactory
    Iterator<DynamicTest> testSaveAllDropdowns() {
        // 1) Find <select> elements and collect their "name" attribute values
        final List<String> dropdownNames = findElements(driver, "select").stream().map(webElement -> webElement.getAttribute("name")).collect(Collectors.toList());

        // 2) For each name from step 1, add a test
        return dropdownNames.stream().map(dropdownName -> DynamicTest.dynamicTest("Test save dropdown " + dropdownName, () -> {
            // 2.1) Get the list of possible values for this dropdown element
            final WebElement dropdown = driver.findElement(By.name(dropdownName));
            final List<WebElement> options = dropdown.findElements(By.tagName("option"));

            // 2.2) Choose one randomly and select it
            final WebElement selectedOption = options.get(RandomUtils.nextInt(0, options.size()));
            selectedOption.click();
            String savedValue = selectedOption.getAttribute("value");

            // 2.3) Submit the element
            dropdown.submit();

            // 2.4) Reload the initial page
            driver.navigate().to(baseUrl);

            // 2.5) Find the dropdown element
            WebElement updatedDropdown = driver.findElement(By.name(dropdownName));
            assertNotNull(updatedDropdown, "No dropdown by name of " + dropdownName);

            // 2.6) Get its selected value
            final WebElement updatedOption = updatedDropdown.findElement(By.cssSelector("option[selected]"));
            assertNotNull(selectedOption, "No selected option for dropdown " + dropdownName);

            // 2.7) Read the value and compare to what we set it to in step 2.2
            final String actual = updatedOption.getAttribute("value");
            assertThat("Incorrect value in dropdown field " + dropdownName, actual, is(savedValue));

        })).iterator();
    }

    @TestFactory
    Iterator<DynamicTest> testSaveAllRadios() {
        // 1) Find <input type="radio"> elements and collect their "name" attribute values
        final Set<String> radioElementGroups = findElements(driver, "input[type='radio']").stream().map(webElement -> webElement.getAttribute("name")).distinct().collect(Collectors.toSet());

        // 2) For each name from step 1, add a test
        return radioElementGroups.stream().map(radioGroupName -> DynamicTest.dynamicTest("Test save radio " + radioGroupName, () -> {
            // 3) Find the element by name, select each of its options in turn and make sure it reflects back after submitting
            final List<WebElement> radioOptions = findElements(driver, "input[type='radio'][name='" + radioGroupName + "']");
            for (WebElement option : radioOptions) {
                // 3.1) Select the value and submit
                String submittedValue = option.getAttribute("value");
                option.click();
                option.submit();

                // 3.2) Reload the initial page
                driver.navigate().to(baseUrl);

                // 3.3) Find the element again, and find the selected option
                final WebElement updatedOption = driver.findElement(By.cssSelector("input[type=radio][name=" + radioGroupName + "]:checked"));
                assertNotNull(updatedOption, "No value selected for radio field " + radioGroupName);

                // 3.4) Read the value and compare to what we set in step 3.1
                final String actual = updatedOption.getAttribute("value");
                assertThat("Incorrect value in radio field " + radioGroupName, actual, is(submittedValue));
            }

        })).iterator();
    }

    @TestFactory
    Iterator<DynamicTest> testSaveAllTextAreas() {
        // 1) Find <textarea> elements and collect their "name" attribute values
        final Set<String> textAreaNames = findElements(driver, "textarea").stream().map(webElement -> webElement.getAttribute("name")).distinct().collect(Collectors.toSet());

        // 2) For each name from step 1, add a test
        return textAreaNames.stream().map(textAreaName -> DynamicTest.dynamicTest("Test save textarea " + textAreaName, () -> {
            // 2.1) Find the element by name
            final WebElement textarea = driver.findElement(By.name(textAreaName));
            assertNotNull(textarea, "No textarea by name of " + textAreaName);

            // 2.2) Set the element's value to be the test input, then submit
            final String submittedValue = ASCII_VALUE + "\n\n\t" + ASCII_VALUE + "\n";
            textarea.clear();
            textarea.sendKeys(submittedValue);
            textarea.submit();

            // 2.3) Reload the initial page
            driver.navigate().to(baseUrl);

            // 2.4) Find the element again
            final WebElement updatedTextarea = driver.findElement(By.name(textAreaName));
            assertNotNull(updatedTextarea, "No textarea found by name of " + textAreaName);

            // 2.5) Read the value and compare to what we set in step 2.2
            assertThat("Incorrect value in textarea field " + textAreaName, updatedTextarea.getText(), is(submittedValue.trim()));
        })).iterator();
    }


    @TestFactory
    Iterator<DynamicTest> testSaveAllCheckboxes() {
        // 1) Find <input type="checkbox"> elements and collect their "name" attribute values
        final Set<String> checkboxNames = findElements(driver, "input[type='checkbox']").stream().map(webElement -> webElement.getAttribute("name")).distinct().collect(Collectors.toSet());

        // 2) For each name from step 1, add a test
        return checkboxNames.stream().map(checkboxName -> DynamicTest.dynamicTest("Test save checkbox " + checkboxName, () -> {
            // 2.1) Find the element by name
            WebElement checkbox = driver.findElement(By.name(checkboxName));
            assertNotNull(checkbox, "Checkbox by name " + checkboxName + " not found");

            // 2.2) ?Invert the box's selection?
            String submittedValue = checkbox.getAttribute("value");
            checkbox.click();
            checkbox.submit();

            // 2.3) Reload the initial page
            driver.navigate().to(baseUrl);

            // 2.4) Find the element again
            final WebElement updatedCheckbox = driver.findElement(By.cssSelector("input[type='checkbox'][name='" + checkboxName + "'][value='" + submittedValue + "']"));
            assertNotNull(updatedCheckbox, "No checkbox by name of " + checkboxName);

            // 2.5) Read the "checked" attribute value make sure it is "true"
            final String checked = updatedCheckbox.getAttribute("checked");
            assertThat("Checkbox " + checkboxName + " should be checked", checked, is("true"));
        })).iterator();
    }

    @Test
    void testElementsHaveUniqueName() {

        String[] duplicateNames =
            // 1) Get all <select>, <textarea>, and <input> elements - provided the <input> elements are NOT radio or submit buttons
            findElements(driver, "input:not([type='radio']):not([type='submit']),select,textarea").stream()
            // 2) Reduce the elements to their names
            .map(webElement -> webElement.getAttribute("name"))
            // 3) Count each name
            .collect(Collectors.groupingBy(name -> name, Collectors.counting()))
            .entrySet().stream()
            // 4) Filter for duplicate names
            .filter(entry -> entry.getValue() > 1)
            // 5) Pretty print the 'name':count
            .map(entry -> "'" + entry.getKey() + "':" + entry.getValue())
            .toArray(String[]::new);

        assertThat("Duplicate element name: " + String.join(", ", duplicateNames), duplicateNames.length, is(0));
    }
}
