package baize.code.java.utils.documentload;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;

import java.util.List;

public interface DocumentLoader {
     List<Document> load(Resource resource);
}
