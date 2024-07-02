package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

@Service
public class MarkdownTransformationService {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().softbreak("<br/>").escapeHtml(true).build();

    public String toHtml(String markdown) {
        Node document = parser.parse(markdown);

        return renderer.render(document);
    }

}
