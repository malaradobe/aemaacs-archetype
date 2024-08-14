package com.larasoft.core.transformer;

import java.io.IOException;

import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.apache.commons.lang3.StringUtils;

@Component(
	immediate = true, 
	service = TransformerFactory.class, 
	property = {
		"pipeline.type=" + MyRewriterTransformer.TYPE
	}
)
public class MyRewriterTransformer implements Transformer, TransformerFactory {
	public static final String TYPE = "mytransformer";
	private static final Logger LOG = LoggerFactory.getLogger(MyRewriterTransformer.class);
	private ContentHandler contentHandler;

    @Override
    public MyRewriterTransformer createTransformer() {
        LOG.info(">>>>>>>>>>>>>>>  createTransformer...");
        return new MyRewriterTransformer();
    }

	@Override
	public void init(ProcessingContext processingContext,
			ProcessingComponentConfiguration processingComponentConfiguration) throws IOException {
				LOG.debug(">>>>>>>>>>>>>>> Init of the transformer");
	}

	@Override
	public void setContentHandler(ContentHandler handler) {
		this.contentHandler = handler;
	}

	@Override
	public void dispose() {

	}

	@Override
	public void setDocumentLocator(Locator locator) {
		contentHandler.setDocumentLocator(locator);
	}

	@Override
	public void startDocument() throws SAXException {
		contentHandler.startDocument();
	}

	@Override
	public void endDocument() throws SAXException {
		contentHandler.endDocument();
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		contentHandler.startPrefixMapping(prefix, uri);
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		contentHandler.endPrefixMapping(prefix);
	}

	/*
	 * This is the main function which is responsible for URL
	 * main update.
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		LOG.info(">>>>>>>>>>>>>>> startElement...{}", qName);

		contentHandler.startElement(uri, localName, qName, atts);
	}

	public static String modifiedUrl(String path) {
		LOG.info(">>>>>>>>>>>>>>> modifiedUrl...{}", path);

		if (StringUtils.isBlank(path)) {
			return path; // blank, return it as is.
		} else {
			if (path.startsWith("/content/practice")) {
				return StringUtils.removeAll(path, "/content/practice");
			}
		}
		return path;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		contentHandler.endElement(uri, localName, qName);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		contentHandler.characters(ch, start, length);
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		contentHandler.ignorableWhitespace(ch, start, length);
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		contentHandler.processingInstruction(target, data);
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		contentHandler.skippedEntity(name);
	}
}
