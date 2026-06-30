package baize.code.java.utils.documentload;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.Resource;

import java.util.List;

public class PDFDocumentLoader implements DocumentLoader {
    @Override
    public List<Document> load(Resource resource) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
        return reader.read();
    }
}
