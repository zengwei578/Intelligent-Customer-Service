package baize.code.java.utils.documentload;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;

import org.springframework.core.io.Resource;

import java.util.List;

public class MarkdownDocumentLoader implements DocumentLoader {


    @Override
    public  List<Document> load(Resource resource) {
        MarkdownDocumentReaderConfig config=MarkdownDocumentReaderConfig.builder()
                .withIncludeBlockquote(false)
                .withIncludeCodeBlock(false)
                .build();
        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);

        return reader.read();
    }
}
