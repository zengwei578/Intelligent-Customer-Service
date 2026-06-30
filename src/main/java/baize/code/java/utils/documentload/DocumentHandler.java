package baize.code.java.utils.documentload;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class DocumentHandler {

    public List<Document> handleDocument(MultipartFile file) {
        //截取文件后缀
        String fileName = file.getOriginalFilename();
        String fileSuffix = null;
        if (fileName != null) {
            fileSuffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }

        //将文件转化为resource
        Resource resource = file.getResource();
        if(fileSuffix==null){
            throw new IllegalArgumentException("不存在的后缀");
        }
        List<Document>docs= switch (fileSuffix){
            case "md" -> new MarkdownDocumentLoader().load(resource);
            case "pdf" -> new PDFDocumentLoader().load(resource);
            case "txt" -> new TextDocumentLoader().load(resource);
            default -> throw new IllegalArgumentException("Unsupported file type: " + fileSuffix);
        };
        return docs;
    }

}
