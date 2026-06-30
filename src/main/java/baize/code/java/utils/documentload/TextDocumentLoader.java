package baize.code.java.utils.documentload;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.core.io.Resource;

import java.util.List;

public class TextDocumentLoader implements DocumentLoader {
    @Override
    public List<Document> load(Resource resource) {
        TextReader textReader = new TextReader(resource);
        return textReader.read();
    }
}
