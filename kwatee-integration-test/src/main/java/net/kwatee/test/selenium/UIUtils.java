/*
 * ${kwatee_copyright}
 */

package net.kwatee.test.selenium;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class UIUtils {

	static String KWATEE_FOLDER = "../kwatee-distrib/target/kwatee-distrib";
	private final static Logger LOG = Logger.getLogger(UIUtils.class.getName());

	final private static int IMPLICIT_WAIT = 5;

	static private Robot robot;
	static private double last_mouse_x = -1;
	static private double last_mouse_y;
	private WebDriver driver;
	final private String winHandle;
	private String screenshotsFolder = null;
	private boolean debugMode = false;

	public static void setup() throws IOException {
		String currentDir = System.getProperty("user.dir");
		KWATEE_FOLDER = new File(currentDir, KWATEE_FOLDER).getCanonicalPath() + "/";
		LOG.log(Level.INFO, "KWATEE_FOLDER={0}", KWATEE_FOLDER);
		try {
			robot = new Robot();
		} catch (AWTException e) {
			throw new IOException(e);
		}
		robot.setAutoDelay(10);
		robot.setAutoWaitForIdle(true);

	}

	public boolean debugMode() {
		return this.debugMode;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

	public static void cleanup() {}

	public UIUtils(String baseUrl) {
		ProfilesIni allProfiles = new ProfilesIni();
		FirefoxProfile profile = allProfiles.getProfile("webdriver");
		driver = new FirefoxDriver(profile);
		driver.get(baseUrl + "index.html");
		driver.manage().window().setPosition(new Point(10, 30));
		driver.manage().window().setSize(new Dimension(840, 680));
		this.winHandle = driver.getWindowHandle();
	}

	public void stop() {
		driver.quit();
	}

	public void typeKeys(String id, String s, boolean clearFirst) {
		typeKeys(By.id(id), s, clearFirst);
	}

	public void typeKeys(By by, String s, boolean clearFirst) {
		WebElement el = waitForElement(by);
		typeKeys(el, s, clearFirst);
	}

	public void typeRowKeys(String rowId, String name, String s) {
		WebElement el = waitForElement(By.xpath("//tr[@id='_" + rowId + "']/descendant::*[@name='" + name + "']"));
		typeKeys(el, s, true);
	}

	private void typeKeys(WebElement el, String s, boolean clearFirst) {
		if (clearFirst) {
			mouseMoveTo(el);
		}
		delay(300);
		if (clearFirst) {
			robot.mousePress(InputEvent.BUTTON1_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
		}
		if (s.length() > 200 || debugMode()) {
			el.clear();
			el.sendKeys(s);
			delay(200);
		} else {
			if (clearFirst) {
				robot.keyPress(KeyEvent.VK_META);
				robot.keyPress(KeyEvent.VK_A);
				robot.keyRelease(KeyEvent.VK_A);
				robot.keyRelease(KeyEvent.VK_META);
			}
			delay(300);
			for (int i = 0; i < s.length(); i++) {
				el.sendKeys(String.valueOf(s.charAt(i)));
			}
		}
	}

	public void clickElementById(String id) {
		clickElementById(id, false);
	}

	public void clickElementById(String id, boolean withShift) {
		clickElementById(id, 5, -5, withShift);
	}

	public void clickElementById(String id, int offsetX, int offsetY, boolean withShift) {
		clickElement(By.id(id), offsetX, offsetY, withShift);
		LOG.log(Level.INFO, "Clicked {0}", id);
	}

	public void clickElement(By by, int offsetX, int offsetY, boolean withShift) {
		WebElement el = waitForElement(by, true);
		mouseMoveTo(el, offsetX, offsetY);
		mouseClick(withShift);
	}

	public void clickElement(WebElement el, boolean withShift) {
		mouseMoveTo(el);
		mouseClick(withShift);
	}

	public void hover(String id) {
		hover(By.id(id));
	}

	public void hover(By by) {
		hover(by, 0, 0);
	}

	public void hover(By by, int xoffset, int yoffset) {
		WebElement el = waitForElement(by, true);
		mouseMoveTo(el, xoffset, yoffset);
		delay(1000);
	}

	public void hoverPackageItem(String name, String id) {
		WebElement el = waitForElement(By.xpath("//div[text()='" + name + "']"));
		mouseMoveTo(el, 650, -5);
		el = waitForElement(By.xpath("//div[text()='" + name + "']/following-sibling::*/descendant::img[contains(@class,'" + id + "')]"));
		mouseMoveTo(el, 0, -5);
		delay(1000);
	}

	public void clickSpan(String name) {
		WebElement el = waitForElement(By.xpath("//span[text()='" + name + "']"));
		mouseMoveTo(el, 0, -8);
		mouseClick(false);
	}

	public void openClosePackageItem(String name) {
		WebElement el = waitForElement(By.xpath("//div[div[div[text()='" + name + "']]]"));
		mouseMoveTo(el);
		mouseClick(false);
	}

	public void toggleTreeFolder(String name) {
		WebElement el = waitForElement(By.xpath("//div[@class='l' and starts-with(descendant::text(),'" + name + "')]/ancestor::tr/td"));
		mouseMoveTo(el);
		mouseClick(false);
	}

	public void selectPackageTreeItem(String name) {
		WebElement el = waitForElement(By.xpath("//span[@class='lbl' and starts-with(descendant::text(),'" + name + "')]"));
		mouseMoveTo(el);
		mouseClick(false);
	}

	public void clickElementWithOffsetByValue(String value) {
		clickElementWithOffset(By.xpath("//label[contains(.,'" + value + "')]"));
	}

	public void clickElementWithOffsetById(String id) {
		clickElementWithOffset(By.id(id));
	}

	public void clickElementWithOffset(By by) {
		WebElement el = waitForElement(by);
		mouseMoveTo(el, 0, -15);
		mouseClick(false);
	}

	public void selectRadio(String id) {
		selectRadio(By.id(id));
	}

	public void selectRadio(By by) {
		WebElement el = waitForElement(by);
		mouseMoveTo(el, 0, -13);
		mouseClick(false);
	}

	public void selectListItem(String listName, String item) {
		LOG.log(Level.INFO, "Server list: {0} item: {1}", new Object[] {listName, item});
		WebElement el = waitForElement(By.id(listName));
		mouseMoveTo(el, 20, -4);
		if (!debugMode())
			delay(500);
		WebElement optEl = waitForElement(By.xpath("//select[@id='" + listName + "']/option[text()='" + item + "']"));
		LOG.log(Level.INFO, "Element tagName={0}, text={1}, loc:{2}", new Object[] {optEl.getTagName(), optEl.getText(), optEl.getLocation().toString()});
		robot.mousePress(InputEvent.BUTTON1_MASK);
		// robot.mouseRelease(InputEvent.BUTTON1_MASK);
		if (!debugMode())
			delay(750);
		mouseMoveTo(optEl, 0, -16);
		// Select selectBox = new Select(el);
		// selectBox.selectByValue(item);
		if (!debugMode())
			delay(250);
		// robot.mousePress(InputEvent.BUTTON1_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_MASK);
	}

	public void openClosePanel(String id) {
		WebElement el = waitForElement(By.id(id));
		mouseMoveTo(el, 60, 0);
		mouseClick(false);
	}

	public void clickRowButton(String name, String buttonId) {
		WebElement el = waitForElement(By.xpath("//td[contains(.,'" + name + "')]/following-sibling::td/descendant::button[@id='" + buttonId + "']"));
		// mouseMoveTo(el, 5, -5);
		mouseMoveTo(el, 0, -5);
		mouseClick(false);
	}

	public void clickTreeItemButton(String name, String buttonId) {
		WebElement el = waitForElement(By.xpath("//span[@class='lbl' and starts-with(text(),'" + name + "')]"));
		mouseMoveTo(el, 650, -5);
		el = waitForElement(By.xpath("//span[@class='lbl' and starts-with(text(),'" + name + "')]/following-sibling::*/descendant::img[starts-with(@action,'" + buttonId + "')]"));
		mouseMoveTo(el, 5, -5);
		mouseClick(false);
	}

	public void openTreeFolder(String name) {
		WebElement el = waitForElement(By.xpath("//td[div[div[span[@class='lbl' and starts-with(text(),'" + name + "')]]]]/preceding-sibling::*/img"));
		mouseMoveTo(el);
		mouseClick(false);
	}

	public void gotoMainTab(String tabName, boolean topLevel) {
		try {
			new WebDriverWait(driver, 0).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//li[@id='" + tabName + "' and @class='active']/a")));
		} catch (TimeoutException e) {
			// clickElementById(tabName);
			clickElementById(tabName, 15, 0, false);
			waitForElement(By.xpath("//li[@id='" + tabName + "' and contains(@class,'active')]/a"));
		}
		if (topLevel) {
			WebElement el = null;
			try {
				el = driver.findElement(By.xpath("//ol[contains(@class,'breadcrumb')]/li/following-sibling::li"));
			} catch (Exception e) {}
			if (el == null || !el.isDisplayed()) {
				return;
			}
			clickElement(el, false);
			delay(500);
		}
	}

	public WebElement waitForElement(By by) {
		return waitForElement(by, true);
	}

	public WebElement waitForElement(By by, boolean visible) {
		LOG.log(Level.INFO, "Waiting for element {0}", by.toString());
		long startTime = System.currentTimeMillis();
		new WebDriverWait(driver, IMPLICIT_WAIT, 100).until(ExpectedConditions.presenceOfElementLocated(by));
		List<WebElement> els = driver.findElements(by);
		if (visible) {
			long timeOut = startTime + IMPLICIT_WAIT * 1000L;
			do {
				for (WebElement el : els) {
					if (el.isDisplayed()) {
						return el;
					}
				}
				delay(250);
				els = driver.findElements(by);
			} while (System.currentTimeMillis() < timeOut);
			throw new RuntimeException("Element found but not visible");
		}
		if (els.isEmpty()) {
			throw new RuntimeException("Element not found");
		}
		return els.get(0);
	}

	public void waitTillElementGone(By by) {
		long startTime = System.currentTimeMillis();
		long timeOut = startTime + IMPLICIT_WAIT * 1000L;
		WebElement el = driver.findElement(by);
		while (el != null && el.isDisplayed() && System.currentTimeMillis() < timeOut) {
			delay(250);
			el = driver.findElement(by);
		}
		if (el != null && el.isDisplayed()) {
			throw new RuntimeException("Element not found");
		}
	}

	public void selectWindow(String name) {
		driver.switchTo().window(name);
	}

	public void delay(long millisecs) {
		try {
			Thread.sleep(millisecs);
		} catch (InterruptedException e) {
			LOG.severe("Interrupted");
		}
	}

	private void mouseMoveTo(WebElement el) {
		mouseMoveTo(el, 0, 0);
	}

	public void mouseMoveTo(WebElement el, int xCorrection, int yCorrection) {
		Point where = el.getLocation();
		Point windowPos = driver.manage().window().getPosition();
		mouseMoveTo(where.getX() + windowPos.x + 7 + xCorrection, where.getY() + windowPos.y + 102 + yCorrection);
	}

	private void mouseMoveTo(double toX, double toY) {
		LOG.log(Level.INFO, "MouseMoveTo X={0}, Y={1}", new Object[] {toX, toY});
		if (debugMode()) {
			robot.mouseMove((int) toX, (int) toY);
			return;
		}
		if (last_mouse_x < 0) {
			last_mouse_x = toX;
			last_mouse_y = toY;
			robot.mouseMove((int) toX, (int) toY);
			return;
		}
		double factor = Math.max(1.0, 2.0 * Math.sqrt(Math.sqrt(Math.abs((toX - last_mouse_x) * (toX - last_mouse_x) + (toY - last_mouse_y))) / 200.0));
		if (factor == Double.NaN)
			factor = 1.0;
		while (Math.abs(last_mouse_x - toX) > 1.5 || Math.abs(last_mouse_y - toY) > 1.5) {
			double deltaX = toX - last_mouse_x;
			double deltaY = toY - last_mouse_y;
			double dx, dy;
			double speed;
			if (Math.abs(deltaX) > Math.abs(deltaY)) {
				speed = Math.log(Math.abs(deltaX));
				dx = (double) Math.signum(deltaX);
				dy = deltaY / Math.abs(deltaX) * Math.log(Math.abs(deltaX));
			} else {
				speed = Math.log(Math.abs(deltaY));
				dy = (double) Math.signum(deltaY);
				dx = deltaX / Math.abs(deltaY);
			}
			// speed = Math.max(1.0, Math.min(speed, 10.0));
			speed = Math.min(speed * factor, 10.0);
			last_mouse_x += dx * speed;
			last_mouse_y += dy * speed;
			robot.mouseMove((int) Math.round(last_mouse_x), (int) Math.round(last_mouse_y));
		}
	}

	private void mouseClick(boolean withShift) {
		if (!debugMode())
			robot.delay(350);
		if (withShift)
			robot.keyPress(KeyEvent.VK_SHIFT);
		robot.mousePress(InputEvent.BUTTON1_MASK);
		if (!debugMode())
			robot.delay(250);
		robot.mouseRelease(InputEvent.BUTTON1_MASK);
		if (withShift)
			robot.keyRelease(KeyEvent.VK_SHIFT);
		if (!debugMode())
			robot.delay(350);
	}

	public void selectTab() {
		for (String winHandle : driver.getWindowHandles()) {
			if (!winHandle.equals(this.winHandle)) {
				driver.switchTo().window(winHandle);
				return;
			}
		}
		throw new RuntimeException("No window found");
	}

	public void selectFrame(int idx) {
		driver.switchTo().frame(0);
	}

	public void closeWindow() {
		Point windowPos = driver.manage().window().getPosition();
		mouseMoveTo(windowPos.x + 14, windowPos.y + 20);
		delay(750);
		mouseClick(false);
		driver.switchTo().window(winHandle);
	}

	public void showWindow(String url) {
		WebDriver driver2 = new FirefoxDriver();
		driver2.get(url);
		if (!debugMode())
			delay(5000);
		else
			delay(1000);
		Point windowPos = driver2.manage().window().getPosition();
		mouseMoveTo(windowPos.x + 14, windowPos.y + 14);
		if (!debugMode())
			delay(750);
		mouseClick(false);
		driver.switchTo().window(winHandle);
		windowPos = driver.manage().window().getPosition();
		mouseMoveTo(windowPos.x + 90, windowPos.y + 14);
		mouseClick(false);
	}

	public void enableScreenShot(String screenshotsFolder) {
		try {
			FileUtils.deleteDirectory(new File(screenshotsFolder));
		} catch (IOException e) {}
		new File(screenshotsFolder + "thumbs/").mkdirs();
		this.screenshotsFolder = screenshotsFolder;
	}

	public void screenShot(String name) {
		screenShot(name, 0);
	}

	public void screenShot(String name, int height) {
		if (this.screenshotsFolder == null) {
			LOG.severe("No screenshot taken");
			return;
		}
		if (height != -1) {
			setWindowHeight(height);
		}
		delay(250);
		if (this.screenshotsFolder != null) {
			File scrFile = ((TakesScreenshot) this.driver).getScreenshotAs(OutputType.FILE);
			// Now you can do whatever you need to do with it, for example copy somewhere
			try {
				BufferedImage cropped = cropImage(scrFile);
				File croppedFile = new File(screenshotsFolder + name + ".png");
				ImageIO.write(cropped, "png", croppedFile);
				int thumbW = 155, thumbH = 110;
				BufferedImage bi = new BufferedImage(thumbW, thumbH, BufferedImage.TYPE_INT_ARGB);
				bi.getGraphics().drawImage(cropped, 0, 0, thumbW, thumbW, 20, 35, thumbW * 3, thumbH * 3, null);
				File thumbFile = new File(screenshotsFolder + "thumbnails/" + name + ".gif");
				ImageIO.write(bi, "gif", thumbFile);
				LOG.log(Level.INFO, "screenshot {0}", name);
			} catch (IOException e) {
				LOG.log(Level.SEVERE, "Failed to take screenshot", e);
			}
		}
	}

	private BufferedImage cropImage(File srcFile) throws IOException {
		BufferedImage img = ImageIO.read(srcFile);
		int white = Color.white.getRGB();
		boolean flag = false;
		int lowerBorder = img.getHeight();
		int rightBorder = img.getWidth();
		do {
			lowerBorder--;
			for (int x = 0; x < rightBorder; x++) {
				if (img.getRGB(x, lowerBorder) != white) {
					flag = true;
					break;
				}
			}
		} while (!flag && lowerBorder > 0);
		flag = false;
		do {
			rightBorder--;
			for (int y = 0; y < lowerBorder; y++) {
				if (img.getRGB(rightBorder, y) != white) {
					flag = true;
					break;
				}
			}
		} while (!flag && rightBorder > 0);
		BufferedImage cropped = new BufferedImage(rightBorder, lowerBorder, BufferedImage.TYPE_INT_ARGB);
		cropped.getGraphics().drawImage(img, 0, 0, rightBorder, lowerBorder, 0, 0, rightBorder, lowerBorder, null);
		return cropped;
	}

	public void setWindowHeight(int height) {
		if (height == 0) {
			WebElement el = waitForElement(By.xpath("//a[.='kwatee']"));
			height = el.getLocation().getY() + 300;
		}
		driver.manage().window().setSize(new Dimension(780, height));
	}

	public void fillWindowPrompt(String text) {
		if (text != null) {
			LOG.log(Level.INFO, "fillWindowPrompt {0}", text);
			Alert alert = driver.switchTo().alert();
			delay(500);
			alert.sendKeys(text);
			robot.keyPress(KeyEvent.VK_DOWN);
			if (!debugMode())
				delay(1500);
			else
				delay(200);
			alert.accept();
			driver.switchTo().window(winHandle);
			delay(500);
		}
	}

	public void back(String crumb) {
		clickElementWithOffset(By.xpath("//ol[contains(@class,'breadcrumb')]/li/a[text()='" + crumb + "']"));
	}

	private void robotType(String s) {
		delay(300);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			int code = 0;
			boolean shift = false;
			switch (c) {
				case ':':
					code = KeyEvent.VK_PERIOD;
					shift = true;
				break;
				case '/':
					code = KeyEvent.VK_7;
					shift = true;
				break;
				case '.':
					code = KeyEvent.VK_PERIOD;
				break;
			}
			if (code != 0) {
				if (shift) {
					robot.keyPress(KeyEvent.VK_SHIFT);
				}
				robot.keyPress(code);
				robot.keyRelease(code);
				if (shift) {
					robot.keyRelease(KeyEvent.VK_SHIFT);
				}
			} else if (Character.isLetterOrDigit(c)) {
				if (Character.isUpperCase(c)) {
					robot.keyPress(KeyEvent.VK_SHIFT);
				}
				robot.keyPress(Character.toUpperCase(c));
				robot.keyRelease(Character.toUpperCase(c));
				if (Character.isUpperCase(c)) {
					robot.keyRelease(KeyEvent.VK_SHIFT);
				}
			}
		}
		delay(300);
	}

	public void newBrowserWindow(String url, boolean reload) {
		mouseMoveTo(130, 12);
		mouseClick(false);
		mouseMoveTo(130, 54);
		delay(250L);
		mouseClick(false);
		delay(500L);
		selectTab();
		Point windowPos = driver.manage().window().getPosition();
		mouseMoveTo(windowPos.x + 70, windowPos.y + 62);
		mouseClick(false);
		robotType(url);
		mouseClick(false);
		mouseMoveTo(windowPos.x + 390, windowPos.y + 62);
		mouseClick(false);
		if (reload) {
			mouseClick(false);
			//			robot.mousePress(InputEvent.BUTTON1_MASK);
			//			robot.mouseRelease(InputEvent.BUTTON1_MASK);
		}
		if (!debugMode()) {
			delay(1000L);
		}
		closeWindow();
		delay(250L);
	}
}
