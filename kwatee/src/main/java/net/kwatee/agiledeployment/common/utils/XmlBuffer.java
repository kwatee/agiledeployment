/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.utils;

import java.util.Stack;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

public class XmlBuffer {

	private StringBuilder buffer = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
	private Stack<String> tags = new Stack<String>();
	private boolean closeLine = false;

	public XmlBuffer() {}

	public XmlBuffer(String comment) {
		this.buffer.append("<!-- ");
		this.buffer.append(comment);
		this.buffer.append(" -->\n");
	}

	public String toString() {
		return this.buffer.toString();
	}

	public void openTag(String tag) {
		closeLine();
		this.closeLine = true;
		this.buffer.append('<').append(tag);
		this.tags.push(tag);
	}

	public void addAttribute(String attr, String value) {
		if (value != null) {
			this.buffer.append(' ').append(attr).append("=\"").append(StringUtils.isEmpty(value) ? StringUtils.EMPTY : StringEscapeUtils.escapeXml(value)).append('"');
		}
	}

	public void addAttribute(String attr, Integer value) {
		if (value != null) {
			this.buffer.append(' ').append(attr).append("=\"").append(value.toString()).append('"');
		}
	}

	public void addValue(String value) {
		if (StringUtils.isNotEmpty(value)) {
			if (!this.tags.peek().startsWith("*")) {
				String tag = this.tags.pop();
				this.tags.push("*" + tag); // to indicate that it has children
			}
			this.buffer.append('>');
			this.buffer.append(StringEscapeUtils.escapeHtml3(value));
			this.closeLine = true;
		}
	}

	public void addCData(String value) {
		if (StringUtils.isNotEmpty(value)) {
			if (!this.tags.peek().startsWith("*")) {
				String tag = this.tags.pop();
				this.tags.push("*" + tag); // to indicate that it has children
			}
			this.buffer.append('>');
			if (value.contains("]]>")) {
				throw new RuntimeException("Cannot store string containing ]]> (" + this.tags.peek() + ")");
			}
			this.buffer.append("<![CDATA[" + value + "]]>");
			this.closeLine = true;
		}
	}

	private void closeLine() {
		if (this.closeLine) {
			if (!this.tags.peek().startsWith("*")) {
				String tag = this.tags.pop();
				this.tags.push("*" + tag); // to indicate that it has children
			}
			this.buffer.append(">\n");
			this.closeLine = false;
		}
	}

	public void closeTag() {
		String tag = this.tags.pop();
		this.closeLine = false;
		if (!tag.startsWith("*")) { /* no children */
			int l = this.buffer.length() - tag.length();
			if (this.buffer.substring(l).equals(tag)) {
				this.buffer.setLength(l - 1); // there's nothing in the tag,
												// just prune it
			} else {
				this.buffer.append("/>\n");
			}
		} else {
			tag = tag.substring(1);
			this.buffer.append("</").append(tag).append(">\n");
		}
	}

	public void addTagWithValue(String tag, String value) {
		if (StringUtils.isNotEmpty(value)) {
			openTag(tag);
			addValue(value);
			closeTag();
		}
	}

	public void addTagWithValue(String tag, Integer value) {
		if (value != null) {
			openTag(tag);
			addValue(value.toString());
			closeTag();
		}
	}

	public void addTagWithCData(String tag, String value) {
		if (StringUtils.isNotEmpty(value)) {
			openTag(tag);
			addCData(value);
			closeTag();
		}
	}
}
